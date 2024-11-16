package com.sushii.djsync_user

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class SelectedSongs : AppCompatActivity() {
    private lateinit var songList: List<String>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_selected_songs)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val backButton = findViewById<MaterialButton>(R.id.btnBack)
        backButton.setOnClickListener {
            onBackPressed()
        }
        songList = intent.getStringArrayListExtra("SongList") ?: emptyList()
        displaySongs()
    }
    private fun displaySongs() {
        for ((i, song) in songList.withIndex()) {
            val textViewId = resources.getIdentifier("song$i", "id", packageName)
            val textView = findViewById<TextView>(textViewId)

            if (textView != null) {
                textView.text = song
            } else {
                Log.e("SelectedSongs", "TextView with ID song$i not found.")
            }
        }
    }



}