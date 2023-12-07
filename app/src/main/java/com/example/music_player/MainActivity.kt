package com.example.music_player

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {

    private lateinit var speedTracker: GPSSpeed
    private lateinit var speedTextView: TextView
    private var i =0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        speedTextView = findViewById(R.id.speedTextView)
        speedTracker = GPSSpeed(this)
        i = 0

        if (checkPlayServices() && checkLocationPermission()) {
            startLocationUpdates()
        }

        val updateSpeedThread = Thread(Runnable
            {
            while (true) {
//                val speed = 10.9F
                val speed = speedTracker.getSpeed()

                runOnUiThread{
                    updateSpeedOnUI(speed)
                }

                Thread.sleep(1000)
            }
        }).start()

    }



    //function that happens when the updateSpeedThread updates
    private fun updateSpeedOnUI(speed: Float) {
        // Update the TextView with the new speed value
        speedTextView.text = "Speed: $speed"
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
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        } else {
            // Request location permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return false
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
    }



}