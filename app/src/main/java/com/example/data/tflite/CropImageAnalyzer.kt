package com.example.data.tflite

import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors

/**
 * Encapsulates the live on-device disease detection output.
 */
data class LiveDiagnosisResult(
    val predictions: List<CropPrediction>,
    val topPrediction: CropPrediction?,
    val inferenceTimeMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * CameraX [ImageAnalysis.Analyzer] pipeline that performs real-time image preprocessing
 * and TFLite local model classification.
 */
class CropImageAnalyzer(
    private val modelService: TFLiteModelService,
    private val preprocessor: ImagePreprocessor = ImagePreprocessor(),
    private val throttleIntervalMs: Long = 400L,
    private val onResultListener: ((LiveDiagnosisResult) -> Unit)? = null
) : ImageAnalysis.Analyzer {

    private var lastAnalyzedTimestamp: Long = 0L

    private val _liveResultFlow = MutableStateFlow<LiveDiagnosisResult?>(null)
    val liveResultFlow: StateFlow<LiveDiagnosisResult?> = _liveResultFlow.asStateFlow()

    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = SystemClock.uptimeMillis()

        // Frame rate throttling to preserve battery and prevent CPU saturation
        if (currentTimestamp - lastAnalyzedTimestamp < throttleIntervalMs) {
            imageProxy.close()
            return
        }

        try {
            val startTime = SystemClock.elapsedRealtime()

            // Preprocess ImageProxy: extract rotated center-cropped bitmap
            val processedBitmap = preprocessor.processImageProxyToBitmap(imageProxy)

            // Convert to normalized direct ByteBuffer for TFLite
            val byteBuffer = preprocessor.bitmapToByteBuffer(processedBitmap)

            // Run TFLite local inference
            val predictions = modelService.classify(byteBuffer)
            val inferenceTimeMs = SystemClock.elapsedRealtime() - startTime

            val result = LiveDiagnosisResult(
                predictions = predictions,
                topPrediction = predictions.firstOrNull(),
                inferenceTimeMs = inferenceTimeMs
            )

            _liveResultFlow.value = result
            onResultListener?.invoke(result)

            lastAnalyzedTimestamp = currentTimestamp
        } catch (e: Exception) {
            // Log or ignore frame error, ensuring imageProxy is always closed
        } finally {
            imageProxy.close()
        }
    }

    companion object {
        /**
         * Factory function to create a configured CameraX [ImageAnalysis] use case.
         */
        fun buildImageAnalysisUseCase(analyzer: CropImageAnalyzer): ImageAnalysis {
            val analysisExecutor = Executors.newSingleThreadExecutor()
            return ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { imageAnalysis ->
                    imageAnalysis.setAnalyzer(analysisExecutor, analyzer)
                }
        }
    }
}
