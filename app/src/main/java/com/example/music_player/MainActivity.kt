package com.example.music_player

import android.content.Intent
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
    private var i =0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        songRecyclerView = findViewById(R.id.rvSongs)
        songRecyclerView.layoutManager = LinearLayoutManager(this)
        songRecyclerView.setHasFixedSize(true)
        tvLoadingData = findViewById(R.id.tvLoadingData)
        ivCurrentSongImage = findViewById(R.id.ivCurrentSongImage)
        tvCurrentSongTitle = findViewById(R.id.tvCurrentSongTitle)
        tvCurrentArtistName = findViewById(R.id.tvCurrentArtistName)
        ibPlayPause = findViewById(R.id.ibPlayPause)
        llCurrentPlaying = findViewById(R.id.llCurrentPlaying)
        player = ExoPlayer.Builder(this).build()
        speedTracker = GPSSpeed(this)
        i = 0

        getSongs { songList ->
            // Continues App

            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(songList[songList.getCurrentSongIndex()].getSongUrl()))
                .setMimeType(MimeTypes.AUDIO_MP4)
                .build()
            player.setMediaItem(mediaItem)
            Log.w("Main Activity", mediaItem.toString())

            player.prepare()

            llCurrentPlaying.isClickable = true
            llCurrentPlaying.setOnClickListener {
                val intent = Intent(this, MusicActivity::class.java)
                startActivity(intent)
            }

            if (checkPlayServices() && checkLocationPermission()) {
                startLocationUpdates()
            }

            Thread {
                while (true) {
//                val speed = 10.9F
                    val speed = speedTracker.getSpeed()

                    runOnUiThread {
                        updateSpeedOnUI(speed)
                    }

                    Thread.sleep(1000)
                }
            }.start()
        }
    }

    private fun getSongs(callback: (Songs) -> Unit) {
        songRecyclerView.visibility = View.GONE
        tvLoadingData.visibility = View.VISIBLE
        llCurrentPlaying.visibility = View.GONE
        songList.clear()

        dbRef = FirebaseDatabase.getInstance().getReference("songs")

        dbRef.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (songSnap in snapshot.children) {
                        val songData = songSnap.getValue(Song::class.java)
                        if (songData != null) {
                            songList.add(songData)
                        }
                    }

                    updateUIWithSongs(songList)

                    callback(songList)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun updateUIWithSongs(songList: Songs) {
        songRecyclerView.visibility = View.VISIBLE
        tvLoadingData.visibility = View.GONE
        llCurrentPlaying.visibility = View.VISIBLE

        // Get current song with persistent data
        updateCurrentSong()

        val songAdapter = SongAdapter(songList)
        songRecyclerView.adapter = songAdapter

        songAdapter.setOnItemClickListener(object : SongAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                Log.w("Main Activity", songList[position].toString())
                songList.setCurrentSongIndex(position)

                val mediaItem = MediaItem.Builder()
                    .setUri(Uri.parse(songList[songList.getCurrentSongIndex()].getSongUrl()))
                    .setMimeType(MimeTypes.AUDIO_MP4)
                    .build()
                player.setMediaItem(mediaItem)

                player.prepare()

                updateCurrentSong()

            }

        })

        ibPlayPause.setOnClickListener {
            // When music is playing, change to pause and pause music
            // Else change to play and play music
            if (player.isPlaying) {
                ibPlayPause.setImageResource(R.drawable.play)
                player.pause()
            } else {
                ibPlayPause.setImageResource(R.drawable.pause)
                player.play()
            }


            Log.w("Main Activity", player.mediaItemCount.toString())
        }

    }

    private fun updateCurrentSong() {
        val currentSong = songList.getCurrentSongIndex()
        Picasso.get().load(songList[currentSong].getImageUrl()).into(ivCurrentSongImage)
        tvCurrentSongTitle.text = songList[currentSong].getTitle()
        tvCurrentArtistName.text = songList[currentSong].getArtist()
    }

    //function that happens when the updateSpeedThread updates
    private fun updateSpeedOnUI(speed: Float) {
//        i += 1
//        Log.w("MainActivity", "update number $i")
        Log.w("MainActivity", "$speed")
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
    }



}