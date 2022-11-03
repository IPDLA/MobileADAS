package com.ipdla.mobileadas.ui.main

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.ipdla.mobileadas.BuildConfig.TMAP_API_KEY
import com.ipdla.mobileadas.R
import com.ipdla.mobileadas.databinding.ActivityMainBinding
import com.ipdla.mobileadas.tflite.objectDetect.ObjectDetectionHelper
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
import org.tensorflow.lite.task.vision.detector.Detection
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
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

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var objectDetectorHelper: ObjectDetectionHelper
    private lateinit var bitmapBuffer: Bitmap
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

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

//        initDectector()
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
                            mainViewModel.initDistance(tMapPolyLine.distance.toInt())
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
        cameraExecutor.shutdown()
    }

    companion object {
        const val REQUEST_LOCATION_PERMISSION = 0
        const val REQUEST_CAMERA_PERMISSION = 1
    }


//    private fun initDectector() {
//        Log.d("abcd","init Detector")
//
//        if (ActivityCompat.checkSelfPermission(this,
//                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
//        ) {
//            ActivityCompat.requestPermissions(
//                this,
//                arrayOf(Manifest.permission.CAMERA),
//                REQUEST_CAMERA_PERMISSION
//            )
//        }
//
//        objectDetectorHelper = ObjectDetectionHelper(
//            context = this,
//            objectDetectorListener = this)
//
//        // Initialize our background executor
//        cameraExecutor = Executors.newSingleThreadExecutor()
//        binding.mainPreviewView.post{
//            setUpCamera()
//        }
//    }
//
//
//
//    private fun setUpCamera() {
//        Log.d("abcd","init setUpCamera")
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(applicationContext)
//        cameraProviderFuture.addListener(
//            {
//                // CameraProvider
//                cameraProvider = cameraProviderFuture.get()
//
//                // Build and bind the camera use cases
//                bindCameraUseCases()
//            },
//            ContextCompat.getMainExecutor(this)
//        )
//    }
//
//    @SuppressLint("UnsafeOptInUsageError")
//    private fun bindCameraUseCases() {
//        Log.d("abcd","init bindCameraUse")
//        // CameraProvider
//        val cameraProvider =
//            cameraProvider ?: throw IllegalStateException("Camera initialization failed.")
//
//        // CameraSelector - makes assumption that we're only using the back camera
//        val cameraSelector =
//            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
//
//        // Preview. Only using the 4:3 ratio because this is the closest to our models
//        preview =
//            Preview.Builder()
//                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
//                .setTargetRotation(binding.mainPreviewView.display.rotation)
//                .build()
//        Log.d("abcd","preview Passed")
//        // ImageAnalysis. Using RGBA 8888 to match how our models work
//        imageAnalyzer =
//            ImageAnalysis.Builder()
//                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
//                .setTargetRotation(binding.mainPreviewView.display.rotation)
//                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
//                .build()
//                // The analyzer can then be assigned to the instance
//                .also {
//                    Log.d("abcd","imageAnalyzer initiated")
//                    it.setAnalyzer(cameraExecutor) { image ->
//                        Log.d("abcd","image analyzer failed")
//                        if (!::bitmapBuffer.isInitialized) {
//                            // The image rotation and RGB image buffer are initialized only once
//                            // the analyzer has started running
//                            Log.d("abcd","imageAnalyzer Entered")
//                            bitmapBuffer = Bitmap.createBitmap(
//                                image.width,
//                                image.height,
//                                Bitmap.Config.ARGB_8888
//                            )
//                        }
//                        Log.d("abcd","after bitmapBuffer")
//
//                        detectObjects(image)
//                    }
//                }
//
//        // Must unbind the use-cases before rebinding them
//        cameraProvider.unbindAll()
//
//        try {
//            // A variable number of use-cases can be passed here -
//            // camera provides access to CameraControl & CameraInfo
//            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
//
//            // Attach the viewfinder's surface provider to preview use case
////            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
//        } catch (exc: Exception) {
//            Log.e("TAG", "Use case binding failed", exc)
//        }
//    }
//
//    private fun detectObjects(image: ImageProxy) {
//        Log.d("abcd","init detectObjects")
//        // Copy out RGB bits to the shared bitmap buffer
//        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }
//
//        val imageRotation = image.imageInfo.rotationDegrees
//        // Pass Bitmap and rotation to the object detector helper for processing and detection
//        objectDetectorHelper.detect(bitmapBuffer, imageRotation)
//    }
//
//    override fun onConfigurationChanged(newConfig: Configuration) {
//        super.onConfigurationChanged(newConfig)
//        imageAnalyzer?.targetRotation = binding.mainPreviewView.display.rotation
//    }
//
//    override fun onError(error: String) {
//        this?.runOnUiThread {
//            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    override fun onResults(
//        results: MutableList<Detection>?,
//        inferenceTime: Long,
//        imageHeight: Int,
//        imageWidth: Int
//    ) {
//        Log.d("abcd", "${results?.get(0)!!.categories[0].label}")
//        this?.runOnUiThread {
//            // Pass necessary information to OverlayView for drawing on the canvas
//            binding.overlay.setResults(
//                results ?: LinkedList<Detection>(),
//                imageHeight,
//                imageWidth
//            )
//
//            // Force a redraw
//            binding.overlay.invalidate()
//        }
//    }
//
//    private fun initBottomSheetControls() {
//        // When clicked, lower detection score threshold floor
//        binding.bottomSheetLayout.thresholdMinus.setOnClickListener {
//            if (objectDetectorHelper.threshold >= 0.1) {
//                objectDetectorHelper.threshold -= 0.1f
//                updateControlsUi()
//            }
//        }
//
//        // When clicked, raise detection score threshold floor
//        binding.bottomSheetLayout.thresholdPlus.setOnClickListener {
//            if (objectDetectorHelper.threshold <= 0.8) {
//                objectDetectorHelper.threshold += 0.1f
//                updateControlsUi()
//            }
//        }
//
//        // When clicked, reduce the number of objects that can be detected at a time
//        binding.bottomSheetLayout.maxResultsMinus.setOnClickListener {
//            if (objectDetectorHelper.maxResults > 1) {
//                objectDetectorHelper.maxResults--
//                updateControlsUi()
//            }
//        }
//
//        // When clicked, increase the number of objects that can be detected at a time
//        binding.bottomSheetLayout.maxResultsPlus.setOnClickListener {
//            if (objectDetectorHelper.maxResults < 5) {
//                objectDetectorHelper.maxResults++
//                updateControlsUi()
//            }
//        }
//
//        // When clicked, decrease the number of threads used for detection
//        binding.bottomSheetLayout.threadsMinus.setOnClickListener {
//            if (objectDetectorHelper.numThreads > 1) {
//                objectDetectorHelper.numThreads--
//                updateControlsUi()
//            }
//        }
//
//        // When clicked, increase the number of threads used for detection
//        binding.bottomSheetLayout.threadsPlus.setOnClickListener {
//            if (objectDetectorHelper.numThreads < 4) {
//                objectDetectorHelper.numThreads++
//                updateControlsUi()
//            }
//        }
//
//        // When clicked, change the underlying hardware used for inference. Current options are CPU
//        // GPU, and NNAPI
//        binding.bottomSheetLayout.spinnerDelegate.setSelection(0, false)
//        binding.bottomSheetLayout.spinnerDelegate.onItemSelectedListener =
//            object : AdapterView.OnItemSelectedListener {
//                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
//                    objectDetectorHelper.currentDelegate = p2
//                    updateControlsUi()
//                }
//
//                override fun onNothingSelected(p0: AdapterView<*>?) {
//                    /* no op */
//                }
//            }
//
//        // When clicked, change the underlying model used for object detection
//        binding.bottomSheetLayout.spinnerModel.setSelection(0, false)
//        binding.bottomSheetLayout.spinnerModel.onItemSelectedListener =
//            object : AdapterView.OnItemSelectedListener {
//                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
//                    objectDetectorHelper.currentModel = p2
//                    updateControlsUi()
//                }
//
//                override fun onNothingSelected(p0: AdapterView<*>?) {
//                    /* no op */
//                }
//            }
//    }
//
//    // Update the values displayed in the bottom sheet. Reset detector.
//    private fun updateControlsUi() {
//        binding.bottomSheetLayout.maxResultsValue.text =
//            objectDetectorHelper.maxResults.toString()
//        binding.bottomSheetLayout.thresholdValue.text =
//            String.format("%.2f", objectDetectorHelper.threshold)
//        binding.bottomSheetLayout.threadsValue.text =
//            objectDetectorHelper.numThreads.toString()
//
//        // Needs to be cleared instead of reinitialized because the GPU
//        // delegate needs to be initialized on the thread using it when applicable
//        objectDetectorHelper.clearObjectDetector()
//        binding.overlay.clear()
//    }

}
