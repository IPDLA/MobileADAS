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
    var threshold: Float = 0.5f,
    var numThreads: Int = 2,
    var maxResults: Int = 3,
    var currentDelegate: Int = 0,
    var currentModel: Int = 0,
    val context: Context,
    val objectDetectorListener: DetectorListener?
) {
    private var objectDetector: ObjectDetector? = null
    private var trafficDetector: ObjectDetector? = null

    fun clearObjectDetector() {
        objectDetector = null
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
                .setScoreThreshold(0.6f)
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
        if (objectDetector == null) {
            setObjectDetector()
        }
        //추가
        if (trafficDetector == null) {
            setTrafficDetector()
        }

        // Inference time is the difference between the system time at the start and finish of the
        // process
        var inferenceTime = SystemClock.uptimeMillis()
        var trafficInferenceTime = SystemClock.uptimeMillis()

        // Create preprocessor for the image.
        // See https://www.tensorflow.org/lite/inference_with_metadata/
        //            lite_support#imageprocessor_architecture
        val imageProcessor =
            ImageProcessor.Builder()
                .add(Rot90Op(-imageRotation / 90))
                .build()

        // Preprocess the image and convert it into a TensorImage for detection.
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))

        val results = objectDetector?.detect(tensorImage)
        val trafficResults = trafficDetector?.detect(tensorImage)

        inferenceTime = SystemClock.uptimeMillis() - inferenceTime
        trafficInferenceTime = SystemClock.uptimeMillis() - trafficInferenceTime

        objectDetectorListener?.onResults(
            results,
            inferenceTime,
            trafficResults,
            trafficInferenceTime,
            tensorImage.height,
            tensorImage.width)
    }

    interface DetectorListener {
        fun onError(error: String)
        fun onResults(
            results: MutableList<Detection>?,
            inferenceTime: Long,
            trafficResults: MutableList<Detection>?,
            trafficInferenceTime: Long,
            imageHeight: Int,
            imageWidth: Int
        )
    }
}