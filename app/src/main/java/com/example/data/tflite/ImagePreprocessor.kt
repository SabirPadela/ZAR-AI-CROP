package com.example.data.tflite

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Handles image transformation and tensor normalization for local TFLite neural networks.
 */
class ImagePreprocessor(
    val inputWidth: Int = 224,
    val inputHeight: Int = 224,
    val isModelQuantized: Boolean = false,
    val mean: Float = 127.5f,
    val std: Float = 127.5f
) {
    /**
     * Converts a CameraX [ImageProxy] into an appropriately rotated and scaled [Bitmap].
     */
    fun processImageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val originalBitmap = imageProxy.toBitmap()
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

        val rotatedBitmap = if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
        } else {
            originalBitmap
        }

        // Center crop and scale to model dimensions
        return createCenterCropScaledBitmap(rotatedBitmap, inputWidth, inputHeight)
    }

    /**
     * Converts a source [Bitmap] to a direct, native-ordered [ByteBuffer]
     * formatted as [1, height, width, 3] for TFLite inference.
     */
    fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val scaledBitmap = if (bitmap.width != inputWidth || bitmap.height != inputHeight) {
            createCenterCropScaledBitmap(bitmap, inputWidth, inputHeight)
        } else {
            bitmap
        }

        val bytesPerChannel = if (isModelQuantized) 1 else 4
        val byteBuffer = ByteBuffer.allocateDirect(1 * inputHeight * inputWidth * 3 * bytesPerChannel).apply {
            order(ByteOrder.nativeOrder())
            rewind()
        }

        val intValues = IntArray(inputWidth * inputHeight)
        scaledBitmap.getPixels(intValues, 0, inputWidth, 0, 0, inputWidth, inputHeight)

        var pixelIndex = 0
        for (i in 0 until inputHeight) {
            for (j in 0 until inputWidth) {
                val pixel = intValues[pixelIndex++]

                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                if (isModelQuantized) {
                    byteBuffer.put(r.toByte())
                    byteBuffer.put(g.toByte())
                    byteBuffer.put(b.toByte())
                } else {
                    byteBuffer.putFloat((r - mean) / std)
                    byteBuffer.putFloat((g - mean) / std)
                    byteBuffer.putFloat((b - mean) / std)
                }
            }
        }

        byteBuffer.rewind()
        return byteBuffer
    }

    private fun createCenterCropScaledBitmap(src: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val srcWidth = src.width
        val srcHeight = src.height

        val scale = maxOf(targetWidth.toFloat() / srcWidth, targetHeight.toFloat() / srcHeight)
        val scaledWidth = (srcWidth * scale).toInt()
        val scaledHeight = (srcHeight * scale).toInt()

        val scaled = Bitmap.createScaledBitmap(src, scaledWidth, scaledHeight, true)

        val xOffset = maxOf(0, (scaledWidth - targetWidth) / 2)
        val yOffset = maxOf(0, (scaledHeight - targetHeight) / 2)

        return Bitmap.createBitmap(scaled, xOffset, yOffset, targetWidth, targetHeight)
    }
}
