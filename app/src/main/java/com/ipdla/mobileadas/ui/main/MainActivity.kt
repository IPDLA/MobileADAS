package com.ipdla.mobileadas.ui.main

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
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
import com.ipdla.mobileadas.util.showToast
import com.skt.Tmap.TMapData
import com.skt.Tmap.TMapPoint
import com.skt.Tmap.TMapView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.processNextEventInCurrentThread
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
    private lateinit var lastTMapPoint: TMapPoint
    private lateinit var destinationPoint: TMapPoint
    private lateinit var mediaPlayer: MediaPlayer
    private var prevCautionLevel = 0

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
        initSoundEffect()
        initIsCautionObserver()
        initCautionLevelObserver()
        initIsSoundOnObserver()
        initDistanceObserver()
        initTrafficObserver()
    }

    private fun initLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val lastLocation = locationResult.lastLocation
                if (lastLocation != null) {
                    var speed = lastLocation.speed.toDouble()
                    speed *= METER_PER_SEC_TO_KILOMETER_PER_HOUR * SPEED_CORRECTION_VALUE
                    mainViewModel.initSpeed(speed.toInt())
                    lastTMapPoint = TMapPoint(lastLocation.latitude, lastLocation.longitude)
                    if (mainViewModel.isGuide.value == true) UpdateTMapView()
                }
            }
        }
    }

    private fun UpdateTMapView() {
        tMapView.setCenterPoint(lastTMapPoint.longitude,
            lastTMapPoint.latitude)
        tMapView.setLocationPoint(lastTMapPoint.longitude,
            lastTMapPoint.latitude)

        CoroutineScope(Dispatchers.IO).launch {
            delay(FIND_PATH_DELAY)
            // 보행자 경로 안내
            val tMapPolyLine =
                TMapData().findPathDataWithType(TMapData.TMapPathType.PEDESTRIAN_PATH,
                    lastTMapPoint,
                    destinationPoint)

            // 자동차 경로 안내
//                            val tMapPolyLine =
//                                TMapData().findPathData(presentTMapPoint, destinationPoint)

            tMapPolyLine.lineColor = getColor(R.color.light_blue)
            tMapPolyLine.outLineColor = getColor(R.color.light_blue)
            tMapPolyLine.lineWidth = 30f
            tMapPolyLine.outLineWidth = 50f
            mainViewModel.initDistance(tMapPolyLine.distance.toInt())
            tMapView.addTMapPolyLine("Line1", tMapPolyLine)
            tMapView.setCompassMode(true)
        }
    }

    private fun initLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = LOCATION_REQUEST_INTERVAL
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
                    if (lat != 0.0 && lon != 0.0) destinationPoint = TMapPoint(lat!!, lon!!)
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
                MainDialogFragment {
                    mainViewModel.initDistance(0)
                    mainViewModel.initIsGuide(false)
                    mainViewModel.initDestination(" - ")
                    binding.layoutMapView.removeView(tMapView)
                }.show(supportFragmentManager, this.javaClass.name)
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

    private fun initSoundEffect(level: Int = 1) {
        when (level) {
            1 -> mediaPlayer = MediaPlayer.create(applicationContext, R.raw.sound_beep_level_1)
            2 -> mediaPlayer = MediaPlayer.create(applicationContext, R.raw.sound_beep_level_2)
            3 -> mediaPlayer = MediaPlayer.create(applicationContext, R.raw.sound_beep_level_3)
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

    private fun initCautionLevelObserver() {
        mainViewModel.cautionLevel.observe(this) {
            val level = mainViewModel.cautionLevel.value
            if (mainViewModel.isSoundOn.value == true && (level == 1 || level == 2 || level == 3)) {
                if (mainViewModel.cautionLevel.value != prevCautionLevel) {
                    initSoundEffect(level)
                    prevCautionLevel = level
                }
                mediaPlayer.start()
            }
        }
    }

    private fun initTrafficObserver(){
        //동일한 표지판을 인식한 경우는 예외 처리 ==> 임시 완료
        //모든 순서가 동일할 때 까지
        mainViewModel.newTraffic.observe(this) {
            val newTraffic = mainViewModel.newTraffic.value
            val prevTraffic = mainViewModel.getPrevTraffic()

            if(newTraffic != null){
                val newTrafficList = newTraffic.split(", ")     //현재 탐지된 모든 표지판 리스트
                val prevTrafficList = prevTraffic.split(", ")   //이전에 탐지한 모든 표지판 리스트
                val nonOverlapTrafficList = mutableListOf<String>() //현재 탐지된 표지판 중 이전 탐지결과와 동일하지 않은 표지판 리스트
                var nonOverlapTraffic: String = ""                  //현재 탐지된 표지판 중 이전 탐지결과와 동일하지 않은 표지판 문자열

                //이전 탐지 결과와 새로운 탐지 결과가 중복되지 않는 경우만을 추출하여 화면 점등 여부 결정
                //어린이 보호구역은 해제 표지판이 없는 경우도 있는데, 이럼 표지판 인식 위치와 현재 위치를 비교하면서 제한 속도 30을 결정해야 하나...?
                for(data in newTrafficList){
                    if(!prevTrafficList.contains(data)){
                        nonOverlapTrafficList.add(data)
                        nonOverlapTraffic = "$nonOverlapTraffic $data"
                    }
                }

                //제한 속도 지정
                if(nonOverlapTraffic != "") {
                    for(data in nonOverlapTrafficList){
                        when(data){
                            "restriction_speed20"->mainViewModel.setSpeedLimit(20)
                            "caution_children", "instruction_children", "restriction_speed30"->mainViewModel.setSpeedLimit(30)
                            "restriction_speed40"->mainViewModel.setSpeedLimit(40)
                        }
                    }

                    val result1 = "NotOverlap: $nonOverlapTraffic"
                    val result2 = "Everything: " + mainViewModel.newTraffic.value
                    showToast(result2)
                }
                mainViewModel.setPrevTraffic(mainViewModel.newTraffic.value!!)
            }
        }
    }

    private fun initIsSoundOnObserver() {
        mainViewModel.isSoundOn.observe(this) {
            if (mainViewModel.isSoundOn.value == false) {
                if (mediaPlayer.isPlaying) mediaPlayer.stop()
            }
            initSoundEffect(0)
        }
    }

    private fun initDistanceObserver() {
        mainViewModel.distance.observe(this) {
            if (mainViewModel.isGuide.value == true) {
                if (mainViewModel.distance.value!!.toInt() in 1 until 20) {
                    mainViewModel.initIsGuide(false)
                    showToast(getString(R.string.main_arrival_at_destination))
                }
            }
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
        const val METER_PER_SEC_TO_KILOMETER_PER_HOUR = 3600 / 1000
        const val SPEED_CORRECTION_VALUE = 1.35
        const val LOCATION_REQUEST_INTERVAL = 100L
        const val FIND_PATH_DELAY = 3600L
    }
}
