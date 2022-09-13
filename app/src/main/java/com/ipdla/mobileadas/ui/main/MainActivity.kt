package com.ipdla.mobileadas.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.ipdla.mobileadas.R
import com.ipdla.mobileadas.databinding.ActivityMainBinding
import com.ipdla.mobileadas.ui.base.BaseActivity
import com.ipdla.mobileadas.ui.destination.DestinationActivity
import com.ipdla.mobileadas.ui.main.viewmodel.MainViewModel
import com.skt.Tmap.TMapPoint
import kotlin.math.abs
import kotlin.system.exitProcess

class MainActivity : BaseActivity<ActivityMainBinding>(R.layout.activity_main) {
    private val mainViewModel by viewModels<MainViewModel>()
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var previousLocation: Location
    private lateinit var presentTMapPoint: TMapPoint

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.mainViewModel = mainViewModel
        initLocationCallback()
        initLocationRequest()
        initFusedLocationClient()
        initActivityResultLauncher()
        initGuideBtnClickListener()
        initSoundBtnClickListener()
    }

    private fun initLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val presentLocation = locationResult.lastLocation
                if (presentLocation != null) {
                    var speed = 0.0
                    if (::previousLocation.isInitialized) {
                        val deltaTime = (presentLocation.time - previousLocation.time) / 1000.0
                        speed = abs(previousLocation.distanceTo(presentLocation) / deltaTime)
                    }
                    previousLocation = presentLocation
                    presentTMapPoint =
                        TMapPoint(presentLocation.latitude, presentLocation.longitude)
                    mainViewModel.initSpeed(speed.toInt())
                }
            }
        }
    }

    private fun initLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = 5000
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }
    }

    private fun initFusedLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
        fusedLocationClient.requestLocationUpdates(locationRequest,
            locationCallback,
            Looper.getMainLooper())
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
                mainViewModel.initDestination(null)
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

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_LOCATION_PERMISSION) {
            exitProcess(0)
        }
    }

    override fun onPause() {
        super.onPause()
        overridePendingTransition(0, 0)
        stopLocationUpdates()
    }

    companion object {
        const val REQUEST_LOCATION_PERMISSION = 0
    }
}
