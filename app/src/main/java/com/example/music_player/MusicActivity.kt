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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music)

        ibDownArrow = findViewById(R.id.down_arrow)
        ivCurrentSongImage = findViewById(R.id.ivCurrentSongImage)
        tvCurrentSongTitle = findViewById(R.id.tvCurrentSongTitle)
        tvArtistName = findViewById(R.id.tvArtistName)
        ibSongLike = findViewById(R.id.ibSongLike)
        ibPlayPause = findViewById(R.id.ibPlayPause)

        updateSong()

    }

    private fun updateSong() {
        val currentSong = MainActivity.songList.getCurrentSongIndex()

        Picasso.get().load(MainActivity.songList[currentSong].getImageUrl()).into(ivCurrentSongImage)

        tvCurrentSongTitle.text = MainActivity.songList[currentSong].getTitle()
        tvArtistName.text = MainActivity.songList[currentSong].getArtist()

        if (MainActivity.songList[currentSong].getLike()) {
            ibSongLike.setImageResource(R.drawable.like)
        } else {
            ibSongLike.setImageResource(R.drawable.like_border)
        }

        if (PlayerManager.getPlayer().isPlaying) {
            ibPlayPause.setImageResource(R.drawable.pause)
        } else {
            ibPlayPause.setImageResource(R.drawable.play)
        }

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

        ibSongLike.setOnClickListener {
            if (MainActivity.songList[currentSong].getLike()) {
                ibSongLike.setImageResource(R.drawable.like_border)
                MainActivity.songList[currentSong].setLike(false)
            } else {
                ibSongLike.setImageResource(R.drawable.like)
                MainActivity.songList[currentSong].setLike(true)
            }
        }

        ibDownArrow.setOnClickListener {
            // Inside MusicActivity when you want to finish it and return to MainActivity
            // Call this when you want to indicate successful completion
            val resultIntent = Intent()
            setResult(Activity.RESULT_OK, resultIntent)

            finish()
        }
    }
}