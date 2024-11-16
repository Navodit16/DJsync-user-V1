package com.sushii.djsync_user


import android.content.Intent
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.tasks.Task
import android.util.Log
import androidx.core.graphics.toColor
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class MainActivity : AppCompatActivity() {

    // Declare Firebase Auth and GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    val db = Firebase.firestore



    // Request code to identify the sign-in intent
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Handle edge-to-edge layout insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Change status bar color
        changeStatusBarColor("#8692f7")


            auth = Firebase.auth

            // Configure Google Sign-In options
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))  // Make sure this ID matches the one in Firebase project
                .requestEmail()
                .build()

            // Initialize GoogleSignInClient
            googleSignInClient = GoogleSignIn.getClient(this, gso)

            // Check if the user is already signed in
            val currentUser = auth.currentUser
            updateUI(currentUser)

            // Set up Sign-In Button click listener
            findViewById<Button>(R.id.signInButton).setOnClickListener {
                signIn()
            }

        // Initialize Firebase Auth

        // Set up Sign-Out Button click listener
        findViewById<Button>(R.id.signOutButton).setOnClickListener {
            signOut()
        }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    // Handle the result of the sign-in intent
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Log.w("GoogleSignIn", "signInResult:failed code=" + e.statusCode)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    Log.w("GoogleSignIn", "signInWithCredential:failure", task.exception)
                    updateUI(null)
                }
            }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            // User is signed in
            Toast.makeText(this, "Signed in as: ${user.email}", Toast.LENGTH_SHORT).show()
            val USERS = db.collection("Users")

            val data1 = hashMapOf(
                "name" to "${user.displayName}",

                )
            USERS.document(user.uid).set(data1)

            val intent = Intent(this, SelectClub::class.java)
            startActivity(intent)

            findViewById<Button>(R.id.signInButton).isEnabled = false
            findViewById<Button>(R.id.signOutButton).isEnabled = true
            findViewById<Button>(R.id.signOutButton).setBackgroundColor(getResources().getColor(R.color.lavender))
            findViewById<Button>(R.id.signInButton).setBackgroundColor(getResources().getColor(R.color.grey))
        } else {
            // User is signed out
            findViewById<Button>(R.id.signInButton).isEnabled = true
            findViewById<Button>(R.id.signOutButton).isEnabled = false
            findViewById<Button>(R.id.signOutButton).setBackgroundColor(getResources().getColor(R.color.grey))
            findViewById<Button>(R.id.signInButton).setBackgroundColor(getResources().getColor(R.color.lavender))


        }
    }

    private fun signOut() {
        // Firebase sign out
        auth.signOut()

        // Google sign out
        googleSignInClient.signOut().addOnCompleteListener(this) {
            updateUI(null)
        }
    }

    private fun changeStatusBarColor(color: String) {
        val window: Window = window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = android.graphics.Color.parseColor(color)
    }
}
