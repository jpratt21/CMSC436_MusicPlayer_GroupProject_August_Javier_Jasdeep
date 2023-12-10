package com.example.music_player

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
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
    private lateinit var player: ExoPlayer
    private lateinit var speedTracker: GPSSpeed
    private lateinit var songAdapter: SongAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PlayerManager.initializePlayer(this)
        player = PlayerManager.getPlayer()

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

        // Once songs are loaded
        getSongs {

            // Continues App

            setUpClickListeners()

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

    private fun setUpClickListeners() {
        // Make the current song clickable
        llCurrentPlaying.setOnClickListener {

            val intent = Intent(this, MusicActivity::class.java)
            @Suppress("DEPRECATION")
            startActivityForResult(intent, MUSIC_ACTIVITY_REQUEST_CODE, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }

        player.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updateCurrentSong()
                }
            }
        )

        // Play/pause button handler
        ibPlayPause.setOnClickListener {
            // When music is playing, change to pause and pause music
            // Else change to play and play music
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        val pref : SharedPreferences =
            this.getSharedPreferences(this.packageName + "_preferences", Context.MODE_PRIVATE)
        val editor : SharedPreferences.Editor = pref.edit()
        editor.putInt(CURRENT_SONG, player.currentMediaItemIndex)
        editor.apply()
    }

    override fun onDestroy() {

        Log.w("Main Activity", "Inside onDestroy")

        PlayerManager.releasePlayer()

        super.onDestroy()
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
        dbRef.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (songSnap in snapshot.children) {
                        val songData = songSnap.getValue(Song::class.java)
                        if (songData != null) {
                            songList.add(songData)
                        }
                    }

                    val mediaItems = ArrayList<MediaItem>()
                    for (song in songList) {
                        mediaItems.add(MediaItem.Builder().setUri(song.getSongUrl()).setMimeType(MimeTypes.AUDIO_MP4).build())
                    }
                    player.setMediaItems(mediaItems)

                    val pref: SharedPreferences =
                        this@MainActivity.getSharedPreferences(this@MainActivity.packageName + "_preferences", Context.MODE_PRIVATE)
                    player.seekToDefaultPosition(pref.getInt(CURRENT_SONG, DEFAULT_SONG))

                    player.prepare()
                    player.pause()
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
        songAdapter = SongAdapter(songList)

        // Sets a click listener for each of the songs to change the current songs
        songAdapter.setOnItemClickListener(object : SongAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {

                player.seekToDefaultPosition(position)

                player.prepare()
                player.play()
            }
        })

        songRecyclerView.adapter = songAdapter
    }

    // Update UI of the current song
    private fun updateCurrentSong() {
        val currentSong = songList[player.currentMediaItemIndex]

        if (player.isPlaying) {
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
                // Update Likes in Recycle View
                updateCurrentSong()
                songAdapter.notifyItemChanged(player.currentMediaItemIndex)
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