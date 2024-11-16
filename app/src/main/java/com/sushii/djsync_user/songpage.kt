package com.sushii.djsync_user


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresPermission
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.ReturnCode
import com.arthenica.ffmpegkit.Statistics
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer


class songpage : AppCompatActivity() {

    private val RECORD_AUDIO_REQUEST_CODE = 101
    private lateinit var songAdapter: songAdapter
    private val REDIRECT_URI = "djsync-user://callback"
    private var accessToken: String? = null
    private var selectedSongName: String? = null
    private var selectedArtistName: String? = null// Searched song name
    private var detectedSongName: String? = null  // Detected song name from microphone
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val db = Firebase.firestore
    private var points : Int = 0
    private val cat = Catalog()
    private var currentIndex = 0
    private var counter= 0
    private val songList2 = ArrayList<String?>()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_songpage)

        // Edge to Edge handling
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        auth = FirebaseAuth.getInstance()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val searchBar = findViewById<EditText>(R.id.search_bar)
        val searchButton = findViewById<Button>(R.id.search_button)
        val detectButton = findViewById<FloatingActionButton>(R.id.fab)  // Detect Button
        val artistName = findViewById<EditText?>(R.id.ArtistName)
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_nav)

        bottomNavigationView.background = null
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.points -> {
                    // Handle "Points" item click
                    val intent = Intent(this, PointsInstructions::class.java)
                    startActivity(intent)
                    // Add your navigation logic here
                    true
                }
                R.id.Selected_songs -> {
                    // Handle "Selected Songs" item click
                    val intent = Intent(this, SelectedSongs::class.java)
                    intent.putStringArrayListExtra("SongList", ArrayList(songList2))
                    startActivity(intent)
                    // Add your navigation logic here
                    true

                }
                R.id.Home -> {
                    // Handle "Selected Songs" item click
                    true

                }
                R.id.Logout -> {
                    auth.signOut()

                    // Google sign out
                    googleSignInClient.signOut().addOnCompleteListener {
                        // After signing out, navigate back to MainActivity
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }

                    true

                }
                else -> false
            }
        }
        songAdapter = songAdapter { song ->
            selectedSongName = song.songName
            selectedArtistName = song.artistName

            findViewById<RecyclerView>(R.id.recycler_view).visibility = View.GONE

            runOnUiThread {
                saveSelectedSongToDatabase(selectedSongName, selectedArtistName)
            }

        } // Use CamelCase for the class name

        // Find the RecyclerView and set its layout manager and adapter
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = songAdapter




        val builder = AuthorizationRequest.Builder(getString(R.string.CLIENT_ID), AuthorizationResponse.Type.TOKEN, REDIRECT_URI)
        builder.setScopes(arrayOf("streaming"))
        val request = builder.build()

        AuthorizationClient.openLoginInBrowser(this, request)




        // Search button click listener
        searchButton.setOnClickListener {
            val n = cat.songQuantity(points)
            val songList = Array<String?>(n) { null }
            
            // Add selectedSongName to songList if within bounds
            try {
                if (currentIndex>=5){
                    Toast.makeText(this, "Cannot recommend more than 5 songs", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (currentIndex >= n) {
                    throw ArrayIndexOutOfBoundsException()
                }
                songList[currentIndex] = selectedSongName // Set song at current index
                currentIndex++ // Move to the next index for future use
            } catch (e: ArrayIndexOutOfBoundsException) {
                Toast.makeText(this, "Not enough points", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Immediately exit the listener if error occurs
            }

            // Proceed only if no exception was caught
            val songQuery = searchBar.text?.toString()?.trim() ?: "" // Get text from searchBar and trim
            val artistQuery = artistName?.text?.toString()?.trim() ?: "" // Get text from artistName and trim
            val query = "$songQuery $artistQuery".trim() // Combine the two and trim

            // Check for accessToken and query
            if (accessToken != null && query.isNotEmpty()) {
                searchSpotify(accessToken!!, query)
            } else {
                Toast.makeText(this, "Please authenticate or enter a search query", Toast.LENGTH_SHORT).show()
            }
        }



        // Detect button click listener
        detectButton.setOnClickListener {
            if (checkAndRequestMicPermission()) {
                detectSongUsingMic()
                Toast.makeText(this, "Detecting song...", Toast.LENGTH_SHORT).show()

                        detectButton.isEnabled = false
//                        detectButton.text = "Detecting..."
            }
        }
        onBackPressedDispatcher.addCallback(this,object : OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                val build = AlertDialog.Builder(this@songpage)

                // Set the message show for the Alert time
                build.setMessage("To keep your recommendations, put the app in the background.\nLeave App?")

                // Set Alert Title
                build.setTitle("You will lose your recommendations!")

                // Set Cancelable false for when the user clicks on the outside the Dialog Box then it will remain show
                build.setCancelable(false)

                // Set the positive button with yes name Lambda OnClickListener method is use of DialogInterface interface.
                build.setPositiveButton("Yes") {
                    // When the user click yes button then app will close
                        dialog, which -> finish()
                }

                // Set the Negative button with No name Lambda OnClickListener method is use of DialogInterface interface.
                build.setNegativeButton("No") {
                    // If user click no then dialog box is canceled.
                        dialog, which -> dialog.cancel()
                }

                // Create the Alert dialog
                val alertDialog = build.create()
                // Show the Alert Dialog box
                alertDialog.show()
            }
        })
        changeStatusBarColor("#8692f7")
    }



    // Handle redirect from browser and get access token
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { uri ->
            val response = AuthorizationResponse.fromUri(uri)
            when (response.type) {
                AuthorizationResponse.Type.TOKEN -> {
                    accessToken = response.accessToken
                    Toast.makeText(this, "Authentication successful!", Toast.LENGTH_SHORT).show()
                }
                AuthorizationResponse.Type.ERROR -> {
                    Toast.makeText(this, "Error during authentication: ${response.error}", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(this, "Unexpected response type: ${response.type}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Search for a song using Spotify's API
    private fun searchSpotify(token: String, query: String) {
        val url = "https://api.spotify.com/v1/search?q=$query&type=track&limit=5"


        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@songpage, "Search failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { jsonString ->
                    val jsonObject = JSONObject(jsonString)
                    val tracks = jsonObject.getJSONObject("tracks").getJSONArray("items")

                    val songList = mutableListOf<songList>() // Updated data list for RecyclerView

                    for (i in 0 until tracks.length()) {
                        val track = tracks.getJSONObject(i)
                        val songName = track.getString("name")
                        val artistName = track.getJSONArray("artists").getJSONObject(0).getString("name")
                        val albumImageUrl = track.getJSONObject("album").getJSONArray("images").getJSONObject(0).getString("url")

                        songList.add(songList(songName, artistName, albumImageUrl)) // Use the updated class name
                    }

                    runOnUiThread {
                        songAdapter.setSongList(songList) // Ensure songAdapter is initialized correctly
                        findViewById<RecyclerView>(R.id.recycler_view).visibility = View.VISIBLE
                    }
                }
            }
        })
    }




    // Detect song via microphone and store song name
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @WorkerThread
    private fun detectSongUsingMic() {
        Thread {
            val pcmFilePath = simpleMicRecording() // Record PCM audio and get the file path

            if (pcmFilePath != null) {
                val mp3FilePath = File(filesDir, "recorded_audio.mp3").absolutePath // Path for the MP3 file
                convertPcmToMp3(pcmFilePath, mp3FilePath) { mp3Path ->
                    // After conversion, read the MP3 file and send it to detectSongFromAudio
                    val mp3File = File(mp3Path)
                    val mp3Data = mp3File.readBytes() // Read the MP3 file into byte array
                    detectSongFromAudio(mp3Data) // Pass the MP3 data for song detection
                }
            }
        }.start()
    }


    // Record the song using the microphone
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @WorkerThread
    private fun simpleMicRecording(): String? {  // Change the return type to String (file path)
        val audioSource = MediaRecorder.AudioSource.UNPROCESSED
        val audioFormat = AudioFormat.Builder()
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(48000)
            .build()

        val bufferSize = AudioRecord.getMinBufferSize(
            48000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            return null
        }

        val audioRecord = AudioRecord.Builder()
            .setAudioSource(audioSource)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSize)
            .build()

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            return null
        }

        val pcmFile = File(filesDir, "recorded_audio.pcm") // Save PCM data to a file
        if (!pcmFile.exists() || pcmFile.length() == 0L) {
            // Handle error: PCM file is not properly created
            return null
        }
        val outputStream = pcmFile.outputStream()

        val maxSeconds = 6000 / 1000 // Convert ms to seconds
        val totalBytes = maxSeconds * audioFormat.sampleRate * 2 // 2 bytes per sample for PCM 16-bit
        val destination = ByteBuffer.allocate(totalBytes)

        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)

        audioRecord.startRecording()

        val readBuffer = ByteArray(bufferSize)

        try {
            while (destination.remaining() > 0) {
                val actualRead = audioRecord.read(readBuffer, 0, readBuffer.size)
                if (actualRead > 0) {
                    destination.putTrimming(readBuffer.sliceArray(0 until actualRead))
                    outputStream.write(readBuffer, 0, actualRead) // Save the raw PCM data
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            audioRecord.stop()
            audioRecord.release()
            outputStream.close()
        }

        return pcmFile.absolutePath // Return the PCM file path
    }





    private fun ByteBuffer.putTrimming(byteArray: ByteArray) {
        val spaceLeft = this.capacity() - this.position()
        if (byteArray.size <= spaceLeft) {
            this.put(byteArray)
        } else {
            this.put(byteArray, 0, spaceLeft)
        }
    }

    // Detect the song from audio data
    private fun detectSongFromAudio(audioData: ByteArray) {
        // Replace this URL with the actual endpoint of Shazam's API

        val client = OkHttpClient()
        val mediaType = "application/octet-stream".toMediaTypeOrNull()
        val requestBody = RequestBody.create(mediaType, audioData)

        val request = Request.Builder()
            .url("https://shazam-song-recognition-api.p.rapidapi.com/recognize/file")
            .post(requestBody)
            .addHeader("x-rapidapi-key", getString(R.string.shazam_api))
            .addHeader("x-rapidapi-host", "shazam-song-recognition-api.p.rapidapi.com")
            .addHeader("Content-Type", "application/octet-stream")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@songpage, "Song detection failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    findViewById<FloatingActionButton>(R.id.fab).isEnabled = true
//                    findViewById<Button>(R.id.detect_button).text = "Detect Song via Mic"
                }
            }


            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()


                val responseCode = response.code
                println("Response Code: $responseCode")
                println("Response Body: $responseBody")

                // Check if the response was successful (HTTP 200)
                if (!response.isSuccessful ) {
                    runOnUiThread {
                        Toast.makeText(this@songpage, "Error: Invalid response from server", Toast.LENGTH_SHORT).show()
                        findViewById<FloatingActionButton>(R.id.fab).isEnabled = true
//                        findViewById<Button>(R.id.detect_button).text = "Detect Song via Mic"
                    }
                    return
                }
//                 Check if the response is JSON
                if (responseBody != null) {
                    if (!responseBody.trim().startsWith("{")) {
                        runOnUiThread {
                            Toast.makeText(this@songpage, "Error: Non-JSON response", Toast.LENGTH_SHORT).show()
                            findViewById<FloatingActionButton>(R.id.fab).isEnabled = true
//                            findViewById<Button>(R.id.detect_button).text = "Detect Song via Mic"
                        }
                        return
                    }
                }
                if( responseBody.isNullOrEmpty()){
                    runOnUiThread {
                        Toast.makeText(this@songpage, "Error: Empty response", Toast.LENGTH_SHORT).show()
                        findViewById<FloatingActionButton>(R.id.fab).isEnabled = true
//                        findViewById<Button>(R.id.detect_button).text = "Detect Song via Mic"
                    }
                    return
                }




                try {
                    val jsonObject = JSONObject(responseBody)
                    val track = jsonObject.getJSONObject("track")
                    val songName = track.getString("title") // Get song name
                    val artistName = track.getString("subtitle") // Get artist name

                    // Store the detected song name
                    detectedSongName = "$songName by $artistName"


                    runOnUiThread {
                        Toast.makeText(this@songpage, "Detected Song: $detectedSongName", Toast.LENGTH_SHORT).show()
//                        findViewById<TextView>(R.id.display_song).text = detectedSongName
                        findViewById<FloatingActionButton>(R.id.fab).isEnabled = true
//                        findViewById<Button>(R.id.detect_button).text = "Detect Song via Mic"
                        points()
//                        val user = auth.currentUser
//                        val USERS = db.collection("Users")
//                        if (user != null) {
//                            USERS.document(user.uid).update("detectedSong", detectedSongName)
//                                .addOnSuccessListener {
//                                    Toast.makeText(this@songpage, "Detected song stored", Toast.LENGTH_SHORT).show()
//                                }
//                                .addOnFailureListener { e ->
//                                    Toast.makeText(
//                                        this@songpage,
//                                        "Failed to store the detected song: ${e.message}",
//                                        Toast.LENGTH_SHORT
//                                    ).show()
//                                }
//                        }
                    }

                } catch (e: JSONException) {
                    runOnUiThread {
                        Toast.makeText(this@songpage, "Song not detected", Toast.LENGTH_SHORT).show()
//                        findViewById<TextView>(R.id.display_song).text = "" // Clear the TextView
                        findViewById<FloatingActionButton>(R.id.fab).isEnabled = true
//                        findViewById<Button>(R.id.detect_button).text = "Detect Song via Mic"
                    }
                } finally {
                    // Clear the TextView if no song was detected
                    runOnUiThread {
                        if (detectedSongName.isNullOrEmpty()) {
//                            findViewById<TextView>(R.id.display_song).text = ""
                            findViewById<FloatingActionButton>(R.id.fab).isEnabled = true
//                            findViewById<Button>(R.id.detect_button).text = "Detect Song via Mic"
                        }
                    }
                }
            }
        })
    }


    private fun checkAndRequestMicPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            // Permission is already granted
            true
        } else {
            // Request the permission
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST_CODE)
            false
        }
    }

    // Handle the permission result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with recording
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.RECORD_AUDIO
                    ) != PackageManager.PERMISSION_GRANTED
                ) {

                    return
                }
                detectSongUsingMic()
            } else {
                // Permission denied, notify the user
                Toast.makeText(this, "Microphone permission is required to detect songs.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun convertPcmToMp3(pcmFilePath: String, mp3FilePath: String, onComplete: (String) -> Unit) {
        val command = "-y -f s16le -ar 48000 -ac 1 -i $pcmFilePath -codec:a libmp3lame -b:a 192k $mp3FilePath"


        FFmpegKit.executeAsync(command, { session: FFmpegSession ->
            val returnCode: ReturnCode = session.returnCode

            if (ReturnCode.isSuccess(returnCode)) {
                // Conversion was successful, call the onComplete callback
                onComplete(mp3FilePath)
            } else {
                // Log any error
                Log.e("FFmpeg", "Error in FFmpeg conversion: ${session.failStackTrace}")

                // Handle the error on the UI thread
                runOnUiThread {
                    Toast.makeText(this@songpage, "Error converting PCM to MP3", Toast.LENGTH_SHORT).show()
                }
            }
        }, { log ->
            // Handle logs if needed
            Log.d("FFmpeg", log.message)
        }, { statistics: Statistics ->
            // Handle FFmpeg statistics if needed
            Log.d("FFmpeg", "Statistics: ${statistics.videoFrameNumber} frames processed.")
        })
    }

    private fun points() {
        val Ssong = selectedSongName?.normalizeString()?.trim() ?: ""
        val Dsong = detectedSongName?.normalizeString()?.trim() ?: ""

        // Early return if either Ssong or Dsong is empty
        if (Ssong.isEmpty() || Dsong.isEmpty()) {
            return
        }

        // Check if Ssong contains Dsong or vice versa
        if (Ssong.contains(Dsong, ignoreCase = true) || Dsong.contains(Ssong, ignoreCase = true)) {
            points += 10
            findViewById<TextView>(R.id.points).text = points.toString()
            Toast.makeText(this, "Points updated to $points", Toast.LENGTH_SHORT).show()
        }
    }

    // Extension function to normalize spaces
    private fun String.normalizeString(): String {
        return this.replace(Regex("\\s+"), "") // Replace multiple spaces with a single space
            .replace(Regex("\\(.*"), "") // Remove everything from the first "(" onwards
            .replace(Regex("-.*"), "") // Remove everything from first "-" onwards
    }

    private fun changeStatusBarColor(color: String) {
        val window: Window = window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = android.graphics.Color.parseColor(color)
    }

    private fun saveSelectedSongToDatabase(selectedSong: String?, selectedArtistName: String? ) {
        val user = auth.currentUser
        val USERS = db.collection("Users")
        if (user != null) {
            USERS.document(user.uid).update("selectedSong$counter", "$selectedSong by $selectedArtistName")
                .addOnSuccessListener {
                    Toast.makeText(this, "Song selected: $selectedSong", Toast.LENGTH_SHORT).show()
                    songList2.add("$selectedSong by $selectedArtistName")


                    counter++
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Song selection failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

    }


}
