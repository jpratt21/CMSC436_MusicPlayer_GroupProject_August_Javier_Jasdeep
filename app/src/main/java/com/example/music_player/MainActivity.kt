package com.example.music_player

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso

class MainActivity : AppCompatActivity() {

    private lateinit var songRecyclerView: RecyclerView
    private lateinit var tvLoadingData: TextView
    private lateinit var ivCurrentSongImage: ImageView
    private lateinit var tvCurrentSongTitle: TextView
    private lateinit var tvCurrentArtistName: TextView
    private lateinit var ibPlayPause: ImageButton
    private lateinit var llCurrentPlaying: LinearLayout
    private lateinit var dbRef: DatabaseReference
    private lateinit var speedTracker: GPSSpeed

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        songRecyclerView = findViewById(R.id.rvSongs)   // List of songs View
        songRecyclerView.layoutManager = LinearLayoutManager(this)
        songRecyclerView.setHasFixedSize(true)
        tvLoadingData = findViewById(R.id.tvLoadingData)    // Appears when fetching songs from server
        ivCurrentSongImage = findViewById(R.id.ivCurrentSongImage)
        tvCurrentSongTitle = findViewById(R.id.tvCurrentSongTitle)
        tvCurrentArtistName = findViewById(R.id.tvCurrentArtistName)
        ibPlayPause = findViewById(R.id.ibPlayPause)
        llCurrentPlaying = findViewById(R.id.llCurrentPlaying)
        speedTracker = GPSSpeed(this)
        PlayerManager.initializePlayer(this)

        val pref: SharedPreferences =
            this.getSharedPreferences(this.packageName + "_preferences", Context.MODE_PRIVATE)
        songList.setCurrentSongIndex(pref.getInt(CURRENT_SONG, DEFAULT_SONG))

        // Once songs are loaded
        getSongs { songList ->

            // Continues App
            // Create player which handles the music files
            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(songList[songList.getCurrentSongIndex()].getSongUrl()))
                .setMimeType(MimeTypes.AUDIO_MP4)
                .build()
            PlayerManager.getPlayer().setMediaItem(mediaItem)

            PlayerManager.getPlayer().prepare()
            PlayerManager.getPlayer().pause()

            // Make the current song clickable
            llCurrentPlaying.isClickable = true
            llCurrentPlaying.setOnClickListener {
                val intent = Intent(this, MusicActivity::class.java)
                @Suppress("DEPRECATION")
                startActivityForResult(intent, MUSIC_ACTIVITY_REQUEST_CODE)
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

            // Check for location permissions
            if (checkPlayServices() && checkLocationPermission()) {
                startLocationUpdates()
            }

            // Each second, update user speed
            Thread {
                while (true) {
                    val speed = speedTracker.getSpeed()
                    runOnUiThread {
                        updateSpeedOnUI(speed)
                    }
                    Thread.sleep(1000)
                }
            }.start()
        }
    }

    override fun onStop() {
        super.onStop()

        Log.w("Main Activity", "Inside onStop")
        updateServerLikes()

        val pref : SharedPreferences =
            this.getSharedPreferences(this.packageName + "_preferences", Context.MODE_PRIVATE)
        val editor : SharedPreferences.Editor = pref.edit()
        editor.putInt(CURRENT_SONG, songList.getCurrentSongIndex())
        editor.apply()
    }

    private fun updateServerLikes() {
        for (song in songList) {
            dbRef = FirebaseDatabase
                .getInstance()
                .getReference("songs")
                .child(song.getMediaId())
                .child("like")
            dbRef.setValue(song.getLike())
        }
    }

    private fun getSongs(callback: (Songs) -> Unit) {
        // Make everything invisible except: Loading data...
        songRecyclerView.visibility = View.GONE
        tvLoadingData.visibility = View.VISIBLE
        llCurrentPlaying.visibility = View.GONE
        songList.clear()

        // Get the reference from the Firebase server
        dbRef = FirebaseDatabase.getInstance().getReference("songs")
        // Retrieve all the songs
        dbRef.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (songSnap in snapshot.children) {
                        val songData = songSnap.getValue(Song::class.java)
                        if (songData != null) {
                            songList.add(songData)
                        }
                    }
                    // Update List of Songs (Recycler View)
                    updateUIWithSongs()
                    // Update Current Song
                    updateCurrentSong()
                    // callback so the rest of the app can continue
                    callback(songList)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                // Not necessary
            }
        })
    }

    private fun updateUIWithSongs() {
        // Make everything visible
        songRecyclerView.visibility = View.VISIBLE
        tvLoadingData.visibility = View.GONE
        llCurrentPlaying.visibility = View.VISIBLE

        // Update the visual list of songs
        val songAdapter = SongAdapter(songList)
        songRecyclerView.adapter = songAdapter

        // Sets a click listener for each of the songs to change the current songs
        songAdapter.setOnItemClickListener(object : SongAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {

                PlayerManager.getPlayer().pause()

                songList.setCurrentSongIndex(position)

                PlayerManager.getPlayer().addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        updateCurrentSong()
                    }
                })

                val mediaItem = MediaItem.Builder()
                    .setUri(Uri.parse(songList[songList.getCurrentSongIndex()].getSongUrl()))
                    .setMimeType(MimeTypes.AUDIO_MP4)
                    .build()
                PlayerManager.getPlayer().setMediaItem(mediaItem)
                PlayerManager.getPlayer().prepare()
                PlayerManager.getPlayer().play()
            }
        })
    }

    // Update UI of the current song
    private fun updateCurrentSong() {
        val currentSong = songList[songList.getCurrentSongIndex()]

        if (PlayerManager.getPlayer().isPlaying) {
            ibPlayPause.setImageResource(R.drawable.pause)
        } else {
            ibPlayPause.setImageResource(R.drawable.play)
        }

        Picasso.get().load(currentSong.getImageUrl()).into(ivCurrentSongImage)
        tvCurrentSongTitle.text = currentSong.getTitle()
        tvCurrentArtistName.text = currentSong.getArtist()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == MUSIC_ACTIVITY_REQUEST_CODE) {
            // Handle the result from MusicActivity
            if (resultCode == Activity.RESULT_OK) {
                // Do actions in MainActivity when MusicActivity finishes
                // This block will execute when MusicActivity calls setResult(Activity.RESULT_OK)
                updateCurrentSong()
            }
        }
    }

    //function that happens when the updateSpeedThread updates
    private fun updateSpeedOnUI(speed: Float) {
        // Log.w("MainActivity", speed.toString())
    }

    private fun checkPlayServices(): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(this)
        return resultCode == ConnectionResult.SUCCESS
    }

    private fun checkLocationPermission(): Boolean {
        return if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            true
        } else {
            // Request location permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            false
        }
    }

    private fun startLocationUpdates() {
        speedTracker.startTrackingSpeed()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        var songList: Songs = Songs()
        const val MUSIC_ACTIVITY_REQUEST_CODE = 100
        const val DEFAULT_SONG = 0
        private const val CURRENT_SONG: String = "currentSongIndex"
    }
}