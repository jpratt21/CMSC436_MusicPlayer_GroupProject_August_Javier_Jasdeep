package com.example.music_player

import android.content.SharedPreferences

class Songs: ArrayList<Song>() {
    private var currentSongIndex: Int = 0

    fun getCurrentSongIndex(): Int {
        // Return current song
        return currentSongIndex
    }

    fun setCurrentSongIndex(currentSongIndex: Int) {
        // Set actual song
        this.currentSongIndex = currentSongIndex
    }

    override fun toString(): String {
        var answer = ""
        for (song in this) {
            answer += song.toString()
        }
        return answer
    }
}