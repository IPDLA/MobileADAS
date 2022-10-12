package com.ipdla.mobileadas.ui.main

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorListenerAdapter
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
import com.ipdla.mobileadas.BuildConfig.TMAP_API_KEY
import com.ipdla.mobileadas.R
import com.ipdla.mobileadas.databinding.ActivityMainBinding
import com.ipdla.mobileadas.ui.base.BaseActivity
import com.ipdla.mobileadas.ui.destination.DestinationActivity
import com.ipdla.mobileadas.ui.main.viewmodel.MainViewModel
import com.skt.Tmap.TMapData
import com.skt.Tmap.TMapPoint
import com.skt.Tmap.TMapView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.system.exitProcess

class MainActivity : BaseActivity<ActivityMainBinding>(R.layout.activity_main) {
    private val mainViewModel by viewModels<MainViewModel>()
    private lateinit var animationColorChange: Animator
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var tMapView: TMapView
    private lateinit var previousLocation: Location
    private lateinit var presentTMapPoint: TMapPoint
    private lateinit var destinationPoint: TMapPoint

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.mainViewModel = mainViewModel

        initLocationCallback()
        initLocationRequest()
        initFusedLocationClient()
        initActivityResultLauncher()
        initGuideBtnClickListener()
        initSoundBtnClickListener()
        initCautionAnimator()
        initIsCautionObserver()
    }

    private fun initCautionAnimator() {
        animationColorChange =
            AnimatorInflater.loadAnimator(this, R.animator.animator_caution).apply {
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator?) {
                        super.onAnimationStart(animation)
                    }

                    override fun onAnimationEnd(animation: Animator?) {
                        super.onAnimationEnd(animation)
                    }
                })
                setTarget(binding.layoutMain)
            }
    }

    private fun initIsCautionObserver() {
        mainViewModel.isCaution.observe(this) {
            if (mainViewModel.isCaution.value == true) {
                animationColorChange.start()
            } else {
                animationColorChange.end()
            }
        }
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
                    if (mainViewModel.isGuide.value == true) {
                        tMapView.setCenterPoint(presentTMapPoint.longitude,
                            presentTMapPoint.latitude)
                        tMapView.setLocationPoint(presentTMapPoint.longitude,
                            presentTMapPoint.latitude)

                        CoroutineScope(IO).launch {
                            delay(3600)
                            // 자동차 경로 안내
//                            val tMapPolyLine =
//                                TMapData().findPathData(presentTMapPoint, destinationPoint)

                            // 보행자 경로 안내
                            val tMapPolyLine =
                                TMapData().findPathDataWithType(TMapData.TMapPathType.PEDESTRIAN_PATH,
                                    presentTMapPoint,
                                    destinationPoint)

                            tMapPolyLine.lineColor = getColor(R.color.light_red)
                            tMapPolyLine.outLineColor = getColor(R.color.light_red)
                            tMapPolyLine.lineWidth = 30f
                            tMapPolyLine.outLineWidth = 50f
                            tMapView.addTMapPolyLine("Line1", tMapPolyLine)
                            tMapView.setCompassMode(true)
                        }
                    }
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
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION),
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
                    val lat = it.data?.getDoubleExtra("latitude", 0.0)
                    val lon = it.data?.getDoubleExtra("longitude", 0.0)
                    if (lat != 0.0 && lon != 0.0) {
                        destinationPoint = TMapPoint(lat!!, lon!!)
                    }
                    mainViewModel.initDestination(destination)
                    mainViewModel.initIsGuide(true)
                    initTMapView()
                }
            }
    }

    private fun initTMapView() {
        tMapView = TMapView(this)
        tMapView.setSKTMapApiKey(TMAP_API_KEY)
        tMapView.setIconVisibility(true)
        tMapView.zoomLevel = 19
        tMapView.setUserScrollZoomEnable(true)
        tMapView.setCompassMode(true)
        tMapView.setSightVisible(true)
        tMapView.setTrackingMode(true)
        tMapView.setMapPosition(TMapView.POSITION_NAVI)
        binding.layoutMapView.addView(tMapView)
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
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }

    companion object {
        const val REQUEST_LOCATION_PERMISSION = 0
    }
}
