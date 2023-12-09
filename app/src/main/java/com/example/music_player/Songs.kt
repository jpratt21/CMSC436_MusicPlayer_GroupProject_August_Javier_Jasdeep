package com.example.music_player

import android.content.SharedPreferences

class Songs: ArrayList<Song>() {

    override fun toString(): String {
        var answer = ""
        for (song in this) {
            answer += song.toString()
        }
        return answer
    }
}