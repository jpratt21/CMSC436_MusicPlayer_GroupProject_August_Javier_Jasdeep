package com.example.music_player

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.squareup.picasso.Picasso

class MusicActivity: AppCompatActivity() {

    private lateinit var ivCurrentSongImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music)

        updateSong()

    }

    private fun updateSong() {
        ivCurrentSongImage = findViewById(R.id.ivCurrentSongImage)
        Picasso.get().load(MainActivity.songList[MainActivity.songList.getCurrentSongIndex()].getImageUrl()).into(ivCurrentSongImage)
        
    }
}