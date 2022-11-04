package com.ipdla.mobileadas.tflite.objectDetect

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import com.ipdla.mobileadas.R
import com.ipdla.mobileadas.databinding.FragmentCameraBinding
import com.ipdla.mobileadas.ui.base.BaseFragment
import com.ipdla.mobileadas.ui.main.viewmodel.MainViewModel
import org.tensorflow.lite.task.vision.detector.Detection
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : BaseFragment<FragmentCameraBinding>(R.layout.fragment_camera), ObjectDetectionHelper.DetectorListener {
    private val mainViewModel by activityViewModels<MainViewModel>()
    private var _fragmentCameraBinding: FragmentCameraBinding? = null
    private val fragmentCameraBinding
        get() = _fragmentCameraBinding!!

    private lateinit var objectDetectorHelper: ObjectDetectionHelper
    private lateinit var bitmapBuffer: Bitmap
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var targetList = listOf("person", "car", "laptop", "bus", "bicycle", "truck")
    private var scaleFactor: Float = 1f

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService

    override fun onResume() {
        super.onResume()

        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(requireActivity(), R.id.fragment_container)
                .navigate(CameraFragmentDirections.actionCameraToPermissions())
        }
    }

    override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()

        cameraExecutor.shutdown()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)

        return fragmentCameraBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        objectDetectorHelper = ObjectDetectionHelper(
            context = requireContext(),
            objectDetectorListener = this)

        cameraExecutor = Executors.newSingleThreadExecutor()

        fragmentCameraBinding.previewCamera.post {
            setUpCamera()
        }
    }

    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                cameraProvider = cameraProviderFuture.get()

                bindCameraUseCases()
            },
            ContextCompat.getMainExecutor(requireContext())
        )
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {

        val cameraProvider =
            cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        preview =
            Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.previewCamera.display.rotation)
                .build()

        imageAnalyzer =
            ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.previewCamera.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { image ->
                        if (!::bitmapBuffer.isInitialized) {
                            bitmapBuffer = Bitmap.createBitmap(
                                image.width,
                                image.height,
                                Bitmap.Config.ARGB_8888
                            )
                        }

                        detectObjects(image)
                    }
                }
        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            preview?.setSurfaceProvider(fragmentCameraBinding.previewCamera.surfaceProvider)
        } catch (exc: Exception) {
            Log.e("ObjectDetection", "Use case binding failed", exc)
        }
    }

    private fun detectObjects(image: ImageProxy) {
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }

        val imageRotation = image.imageInfo.rotationDegrees
        objectDetectorHelper.detect(bitmapBuffer, imageRotation)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = fragmentCameraBinding.previewCamera.display.rotation
    }

    override fun onResults(
        results: MutableList<Detection>?,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int
    ) {
        activity?.runOnUiThread {
            if (results != null) {
                for (result in results) {
                    if (targetList.contains(result.categories[0].label)) {
                        val boundingBox = result.boundingBox
                        var width = 0f
                        var height = 0f
                        if (result.categories[0].label.equals("person")) {
                            width = boundingBox.width() * scaleFactor
                            height = boundingBox.height() * scaleFactor

                            if (width / imageWidth > 0.5f && height / imageHeight > 0.3f) {
                                Toast.makeText(context,result.categories[0].label,Toast.LENGTH_SHORT).show()
                                mainViewModel.initCautionLevel(1)
                            }
                        }else if (result.categories[0].label.equals("bicycle")) {
                            width = boundingBox.width() * scaleFactor
                            height = boundingBox.height() * scaleFactor

                            if (width / imageWidth > 0.5f && height / imageHeight > 0.3f) {
                                Toast.makeText(context,result.categories[0].label,Toast.LENGTH_SHORT).show()
                                mainViewModel.initCautionLevel(1)
                            }

                        }else if (result.categories[0].label.equals("car")) {
                            width = boundingBox.width() * scaleFactor
                            height = boundingBox.height() * scaleFactor

                            if (width / imageWidth > 0.5f && height / imageHeight > 0.3f) {
                                Toast.makeText(context,result.categories[0].label,Toast.LENGTH_SHORT).show()
                                mainViewModel.initCautionLevel(1)
                            }
                        } else {
                            mainViewModel.initCautionLevel(0)
                        }
                    }
                }
            }
            fragmentCameraBinding.overlay.setResults(
                results ?: LinkedList<Detection>(),
                imageHeight,
                imageWidth
            )

            fragmentCameraBinding.overlay.invalidate()
        }
    }

    override fun onError(error: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }

}
