package com.example.music_player

import android.content.SharedPreferences

class Songs: ArrayList<Song>() {
    private var currentSongIndex: Int = 0

    fun getCurrentSongIndex(): Int {
        // Inputs: Author, Title
        // Search among the songs some song with the same title/author
        // If found, return index of the song
        // Else, return first song of the Array list
        return currentSongIndex
    }

    fun setCurrentSongIndex(currentSongIndex: Int) {
        this.currentSongIndex = currentSongIndex
    }

    override fun toString(): String {
        return "${this[getCurrentSongIndex()]}"
    }
}