package com.ipdla.mobileadas.ui.main

import android.Manifest
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.ipdla.mobileadas.R
import com.ipdla.mobileadas.databinding.ActivityMainBinding
import com.ipdla.mobileadas.ui.base.BaseActivity
import com.ipdla.mobileadas.ui.destination.DestinationActivity
import com.ipdla.mobileadas.ui.main.viewmodel.MainViewModel

class MainActivity : BaseActivity<ActivityMainBinding>(R.layout.activity_main) {
    private val mainViewModel by viewModels<MainViewModel>()
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private lateinit var lastLocation: Location

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.mainViewModel = mainViewModel

        initGPSPermission()
        initActivityResultLauncher()
        initSpeedCheck()
        initGuideBtnClickListener()
        initSoundBtnClickListener()
    }

    private fun initGPSPermission() {
        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    // Precise location access granted.
                }
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    // Only approximate location access granted.
                }
                else -> {
                    // No location access granted.
                }
            }
        }
        locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    private fun initSpeedCheck() {
        locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager
        locationListener = LocationListener { location ->
            Log.d("testtt", location.toString())
            var deltaTime = 0.0
            var speed = 0.0
            if (location.hasSpeed()) {

                if (this::lastLocation.isInitialized) {
                    deltaTime = (location.time - lastLocation.time) / 1000.0
                    speed = lastLocation.distanceTo(location) / deltaTime
                }
                lastLocation = location
                Log.d("testtt", speed.toString())
                mainViewModel.initSpeed(speed.toInt())
            }
        }


    }

    private fun initActivityResultLauncher() {
        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    val destination = it.data?.getStringExtra("destination").toString()
                    mainViewModel.initDestination(destination)
                    mainViewModel.initIsGuide(true)
                    mainViewModel.initTime("06:44") // test
                }
            }
    }

    private fun initGuideBtnClickListener() {
        binding.btnMainGuide.setOnClickListener {
            if (mainViewModel.isGuide.value == true) {
                MainDialogFragment().show(supportFragmentManager, this.javaClass.name)
            } else {
                val intent = Intent(this, DestinationActivity::class.java)
                activityResultLauncher.launch(intent)
            }
        }
    }

    private fun initSoundBtnClickListener() {
        binding.btnMainSound.setOnClickListener {
            mainViewModel.initIsSoundOn(!mainViewModel.isSoundOn.value!!)
        }
    }

    override fun onPause() {
        super.onPause()
        overridePendingTransition(0, 0)
    }
}
