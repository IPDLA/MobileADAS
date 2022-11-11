package com.ipdla.mobileadas.tflite.objectDetect

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector

class ObjectDetectionHelper(
    val context: Context,
    val objectDetectorListener: DetectorListener?
) {
    private var objectDetector: ObjectDetector? = null
    private var trafficDetector: ObjectDetector? = null

    fun clearObjectDetector() {
        objectDetector = null
    }
    fun clearTrafficDetector() {
        trafficDetector = null
    }

    fun setObjectDetector(){
        val optionsBuilder =
            ObjectDetector.ObjectDetectorOptions.builder()
                .setScoreThreshold(0.7f)
                .setMaxResults(3)
        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(2)
        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        val modelName = "mobilenetv1.tflite"

        try {
            objectDetector =
                ObjectDetector.createFromFileAndOptions(context, modelName, optionsBuilder.build())
        } catch (e: IllegalStateException) {
            objectDetectorListener?.onError(
                "Object detector failed to initialize. See error logs for details"
            )
            Log.e("Test", "TFLite failed to load model with error: " + e.message)
        }
    }

    fun setTrafficDetector(){
        val optionsBuilder =
            ObjectDetector.ObjectDetectorOptions.builder()
                .setScoreThreshold(0.8f)
                .setMaxResults(3)
        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(2)
        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        val modelName = "traffic.tflite"

        try {
            trafficDetector =
                ObjectDetector.createFromFileAndOptions(context, modelName, optionsBuilder.build())
        } catch (e: IllegalStateException) {
            objectDetectorListener?.onError(
                "Traffic detector failed to initialize. See error logs for details"
            )
            Log.e("Test", "TFLite failed to load traffic model with error: " + e.message)
        }
    }

    fun detect(image: Bitmap, imageRotation: Int) {
        if (objectDetector == null)
            setObjectDetector()
        if(trafficDetector == null)
            setTrafficDetector()

        var inferenceTime = SystemClock.uptimeMillis()
        val imageProcessor =
            ImageProcessor.Builder()
                .add(Rot90Op(-imageRotation / 90))
                .build()
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))
        val results = objectDetector?.detect(tensorImage)
        val trafficResults = trafficDetector?.detect(tensorImage)

        inferenceTime = SystemClock.uptimeMillis() - inferenceTime
        objectDetectorListener?.onResults(
            trafficResults,
            results,
            inferenceTime,
            tensorImage.height,
            tensorImage.width)
    }

    interface DetectorListener {
        fun onError(error: String)
        fun onResults(
            trafficResults: MutableList<Detection>?,
            results: MutableList<Detection>?,
            inferenceTime: Long,
            imageHeight: Int,
            imageWidth: Int
        )
    }
}