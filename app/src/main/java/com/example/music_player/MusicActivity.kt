package com.example.music_player

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.squareup.picasso.Picasso

class MusicActivity: AppCompatActivity() {

    private lateinit var ibDownArrow: ImageButton
    private lateinit var ivCurrentSongImage: ImageView
    private lateinit var tvCurrentSongTitle: TextView
    private lateinit var tvArtistName:TextView
    private lateinit var ibSongLike: ImageButton
    private lateinit var ibPlayPause: ImageButton
    private lateinit var currentSong: Song

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music)

        ibDownArrow = findViewById(R.id.down_arrow)
        ivCurrentSongImage = findViewById(R.id.ivCurrentSongImage)
        tvCurrentSongTitle = findViewById(R.id.tvCurrentSongTitle)
        tvArtistName = findViewById(R.id.tvArtistName)
        ibSongLike = findViewById(R.id.ibSongLike)
        ibPlayPause = findViewById(R.id.ibPlayPause)

        currentSong = MainActivity.songList[MainActivity.songList.getCurrentSongIndex()]

        updateSongUI()
    }

    private fun updateSongUI() {
        // Set song image
        Picasso.get().load(currentSong.getImageUrl()).into(ivCurrentSongImage)
        // Set song title
        tvCurrentSongTitle.text = currentSong.getTitle()
        // Set song artist
        tvArtistName.text = currentSong.getArtist()
        // Set song like
        if (currentSong.getLike()) {
            ibSongLike.setImageResource(R.drawable.like)
        } else {
            ibSongLike.setImageResource(R.drawable.like_border)
        }
        // Set play/pause
        if (PlayerManager.getPlayer().isPlaying) {
            ibPlayPause.setImageResource(R.drawable.pause)
        } else {
            ibPlayPause.setImageResource(R.drawable.play)
        }

        // Set up listeners
        setUpListeners()
    }

    private fun setUpListeners() {
        // Play/pause button handler
        ibPlayPause.setOnClickListener {
            // When music is playing, change to pause and pause music
            // Else change to play and play music
            if (PlayerManager.getPlayer().isPlaying) {
                ibPlayPause.setImageResource(R.drawable.play)
                PlayerManager.getPlayer().pause()
            } else {
                ibPlayPause.setImageResource(R.drawable.pause)
                PlayerManager.getPlayer().play()
            }
        }

        // Like button handler
        ibSongLike.setOnClickListener {
            if (currentSong.getLike()) {
                currentSong.setLike(false)
                ibSongLike.setImageResource(R.drawable.like_border)
            } else {
                currentSong.setLike(true)
                ibSongLike.setImageResource(R.drawable.like)
            }
        }

        // Down arrow button listener
        ibDownArrow.setOnClickListener {
            // Update songList
            updateSongList()
            val resultIntent = Intent()
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        // Update songList
        updateSongList()
        val resultIntent = Intent()
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun updateSongList() {
        MainActivity.songList[MainActivity.songList.getCurrentSongIndex()] = currentSong
    }
}