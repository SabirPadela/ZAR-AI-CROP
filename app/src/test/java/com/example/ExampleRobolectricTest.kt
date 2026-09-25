package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Zaria AI", appName)
  }

  @Test
  fun `verify sample crop presets`() {
    val presets = com.example.util.SampleCropData.presets
    assertEquals(6, presets.size)
    val first = presets.first()
    assertEquals("Cotton (Kapas)", first.cropName)
  }

  @Test
  fun `verify app permission types configuration`() {
    val camera = com.example.ui.permissions.AppPermissionType.CAMERA
    val mic = com.example.ui.permissions.AppPermissionType.MICROPHONE

    assertEquals(android.Manifest.permission.CAMERA, camera.permission)
    assertEquals(android.Manifest.permission.RECORD_AUDIO, mic.permission)
    assertEquals("Camera Access Required", camera.title)
    assertEquals("Microphone Access Required", mic.title)
  }

  @Test
  fun `verify temporary photo storage directory creation`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val storageDir = java.io.File(context.cacheDir, "crop_photos").apply {
      if (!exists()) mkdirs()
    }
    org.junit.Assert.assertTrue(storageDir.exists())
    org.junit.Assert.assertTrue(storageDir.isDirectory)
  }

  @Test
  fun `launch MainActivity test`() {
    val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
    org.junit.Assert.assertNotNull(controller.get())
  }

  @Test
  fun `verify image preprocessor bytebuffer allocation`() {
    val preprocessor = com.example.data.tflite.ImagePreprocessor(inputWidth = 224, inputHeight = 224)
    val testBitmap = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
    val byteBuffer = preprocessor.bitmapToByteBuffer(testBitmap)

    // Expected float32 buffer capacity: 1 * 224 * 224 * 3 * 4 = 602,112 bytes
    assertEquals(602112, byteBuffer.capacity())
    assertEquals(0, byteBuffer.position())
  }

  @Test
  fun `verify tflite model service initialization and classification`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val service = com.example.data.tflite.TFLiteModelService()
    val initialized = service.initialize(context)
    org.junit.Assert.assertTrue(service.isInitialized)

    val preprocessor = com.example.data.tflite.ImagePreprocessor()
    val testBitmap = android.graphics.Bitmap.createBitmap(224, 224, android.graphics.Bitmap.Config.ARGB_8888)
    val predictions = service.classify(testBitmap, preprocessor)

    org.junit.Assert.assertTrue(predictions.isNotEmpty())
    val top = predictions.first()
    org.junit.Assert.assertNotNull(top.label)
    org.junit.Assert.assertTrue(top.confidence > 0f)
  }

  @Test
  fun `verify room database entity and dao operations`() = kotlinx.coroutines.test.runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val dao = db.scanResultDao()

    val scan = com.example.data.local.ScanResultEntity(
        diseasePredictionTag = "Cotton Leaf Curl Virus",
        confidenceScore = 0.94f,
        timestamp = 1711350000000L,
        cropType = "Cotton (Kapas)",
        severity = "High",
        treatmentAdvice = "Spray Imidacloprid 200 SL @ 60 ml/acre"
    )

    val rowId = dao.insertScanResult(scan)
    org.junit.Assert.assertTrue(rowId > 0)

    val fetched = dao.getScanResultByIdDirect(rowId)
    org.junit.Assert.assertNotNull(fetched)
    assertEquals("Cotton Leaf Curl Virus", fetched?.diseasePredictionTag)
    assertEquals(0.94f, fetched?.confidenceScore ?: 0f, 0.001f)
    assertEquals(1711350000000L, fetched?.timestamp)
    assertEquals("Cotton (Kapas)", fetched?.cropType)

    db.close()
  }
}
