package com.example.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.view.ViewGroup
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import com.example.data.tflite.CropImageAnalyzer
import com.example.data.tflite.LiveDiagnosisResult
import com.example.data.tflite.TFLiteModelService
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import java.io.IOException
import java.util.concurrent.Executor

/**
 * Controller class managing CameraX capture operations, torch, error states, and saving
 * photos to temporary application storage with loading state tracking.
 */
@Stable
class CameraPreviewController(
    val context: Context,
    val imageCapture: ImageCapture,
    val executor: Executor = ContextCompat.getMainExecutor(context)
) {
    /**
     * Whether a photo capture and save operation is currently in progress.
     */
    var isLoading by mutableStateOf(false)
        private set

    /**
     * Any error message encountered during capture or image processing.
     */
    var errorMessage by mutableStateOf<String?>(null)
        private set

    /**
     * Current torch / flash status.
     */
    var isTorchOn by mutableStateOf(false)
        private set

    var camera: Camera? = null

    /**
     * Toggles camera torch / flashlight if the active camera hardware supports it.
     */
    fun toggleTorch() {
        camera?.let { cam ->
            if (cam.cameraInfo.hasFlashUnit()) {
                val nextState = !isTorchOn
                cam.cameraControl.enableTorch(nextState)
                isTorchOn = nextState
            }
        }
    }

    /**
     * Captures high-resolution photo from the live camera preview, saves it to
     * temporary application storage in cacheDir, rotates according to EXIF metadata,
     * and triggers a loading state throughout the capture lifecycle.
     *
     * @param onPhotoSaved Invoked with the temporary [File] and rotated [Bitmap] upon success.
     * @param onError Invoked with the [ImageCaptureException] if capture fails.
     */
    fun takePhoto(
        onPhotoSaved: (file: File, bitmap: Bitmap) -> Unit,
        onError: (ImageCaptureException) -> Unit = {}
    ) {
        if (isLoading) return

        // Trigger loading state immediately
        isLoading = true
        errorMessage = null

        val storageDir = File(context.cacheDir, "crop_photos").apply {
            if (!exists()) mkdirs()
        }
        val photoFile = File(storageDir, "crop_scan_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    try {
                        val bitmap = loadRotatedBitmapFromFile(photoFile)
                        isLoading = false
                        onPhotoSaved(photoFile, bitmap)
                    } catch (e: Exception) {
                        isLoading = false
                        val errorText = "Failed to decode saved image: ${e.localizedMessage ?: "Unknown error"}"
                        errorMessage = errorText
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    isLoading = false
                    val errorText = "Capture failed: ${exception.localizedMessage ?: "Camera error"}"
                    errorMessage = errorText
                    onError(exception)
                }
            }
        )
    }

    fun clearError() {
        errorMessage = null
    }
}

/**
 * Creates and remembers a [CameraPreviewController] instance.
 */
@Composable
fun rememberCameraPreviewController(): CameraPreviewController {
    val context = LocalContext.current
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    return remember(context, imageCapture) {
        CameraPreviewController(
            context = context,
            imageCapture = imageCapture
        )
    }
}

/**
 * CameraX live preview component for live feed crop disease inspection and high-res capture.
 */
@Composable
fun LiveCameraPreviewView(
    onImageCaptured: (Bitmap) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    controller: CameraPreviewController = rememberCameraPreviewController(),
    onPhotoSaved: ((File, Bitmap) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }

    // TFLite model service and real-time image analyzer
    val modelService = remember {
        TFLiteModelService().apply {
            initialize(context)
        }
    }
    var liveDiagnosis by remember { mutableStateOf<LiveDiagnosisResult?>(null) }
    val imageAnalyzer = remember(modelService) {
        CropImageAnalyzer(
            modelService = modelService,
            throttleIntervalMs = 400L,
            onResultListener = { result ->
                liveDiagnosis = result
            }
        )
    }
    val imageAnalysis = remember(imageAnalyzer) {
        CropImageAnalyzer.buildImageAnalysisUseCase(imageAnalyzer)
    }

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val preview = remember {
        Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }
    }

    // Bind Camera lifecycle
    LaunchedEffect(lensFacing) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                cameraProvider.unbindAll()
                val cam = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    controller.imageCapture,
                    imageAnalysis
                )
                controller.camera = cam

                // Restore torch state if supported
                if (cam.cameraInfo.hasFlashUnit()) {
                    cam.cameraControl.enableTorch(controller.isTorchOn)
                }
            } catch (e: Exception) {
                // If binding fails, report to controller
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Turn off torch, unbind camera, and release TFLite resources on disposal
    DisposableEffect(Unit) {
        onDispose {
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
            } catch (_: Exception) {}
            modelService.close()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("camera_live_preview_container")
    ) {
        // CameraX Surface View
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Crop Targeting Reticle / Framing Guide Overlay
        CropTargetingOverlay(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 80.dp)
        )

        // Real-time on-device TFLite live detection badge
        liveDiagnosis?.topPrediction?.let { top ->
            Surface(
                color = Color.Black.copy(alpha = 0.78f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF81C784).copy(alpha = 0.8f)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 124.dp)
                    .testTag("live_tflite_detection_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TFLite Live: ${top.label} (${(top.confidence * 100).toInt()}%)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${liveDiagnosis?.inferenceTimeMs}ms",
                        color = Color(0xFF81C784),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Top Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close Button
            IconButton(
                onClick = onClose,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White
                ),
                enabled = !controller.isLoading,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("camera_close_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Camera"
                )
            }

            // Title badge
            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CropFree,
                        contentDescription = null,
                        tint = Color(0xFF81C784),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Align Leaf / Symptom",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Torch Toggle (only if back camera has flash)
            IconButton(
                onClick = {
                    controller.toggleTorch()
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    contentColor = if (controller.isTorchOn) Color(0xFFFFD54F) else Color.White
                ),
                enabled = !controller.isLoading,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("camera_torch_button")
            ) {
                Icon(
                    imageVector = if (controller.isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Toggle Torch"
                )
            }
        }

        // Active Loading Overlay during photo capture and saving
        AnimatedVisibility(
            visible = controller.isLoading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.85f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF81C784)),
                modifier = Modifier.padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF81C784),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Saving Crop Photo...",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Preparing high-res leaf scan",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Error message card if any
        controller.errorMessage?.let { error ->
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                color = Color.Black.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = error,
                        color = Color.White,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Bottom Capture Controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Switch Camera Lens (Front/Back)
                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    ),
                    enabled = !controller.isLoading,
                    modifier = Modifier
                        .size(50.dp)
                        .testTag("camera_switch_lens_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera Lens",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Shutter / Capture Button calling takePhoto
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(if (controller.isLoading) Color.Gray else Color(0xFF2E7D32))
                        .testTag("camera_capture_shutter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            controller.takePhoto(
                                onPhotoSaved = { file, bitmap ->
                                    onPhotoSaved?.invoke(file, bitmap)
                                    onImageCaptured(bitmap)
                                }
                            )
                        },
                        enabled = !controller.isLoading,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (controller.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(30.dp),
                                color = Color.White,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Capture Photo",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // Placeholder space for symmetry
                Spacer(modifier = Modifier.size(50.dp))
            }
        }
    }
}

/**
 * Visual viewfinder overlay with crop corner brackets.
 */
@Composable
private fun CropTargetingOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(300.dp)
                .border(
                    BorderStroke(2.dp, Color(0xFF81C784).copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(16.dp)
                )
        )
    }
}

/**
 * Decodes the image from [File] and applies proper EXIF rotation.
 */
private fun loadRotatedBitmapFromFile(file: File): Bitmap {
    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        ?: throw IOException("Failed to decode image from ${file.absolutePath}")

    return try {
        val exif = ExifInterface(file.absolutePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }
        if (orientation != ExifInterface.ORIENTATION_NORMAL && orientation != ExifInterface.ORIENTATION_UNDEFINED) {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    } catch (_: Exception) {
        bitmap
    }
}
