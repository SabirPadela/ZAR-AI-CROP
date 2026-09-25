package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path

data class CropSamplePreset(
    val id: String,
    val title: String,
    val cropName: String,
    val diseaseSuspected: String,
    val description: String,
    val leafColor: Int,
    val spotColor: Int
)

object SampleCropData {
    val presets = listOf(
        CropSamplePreset(
            id = "cotton_clcuv",
            title = "Cotton - Leaf Curl Virus",
            cropName = "Cotton (Kapas)",
            diseaseSuspected = "Cotton Leaf Curl Virus (CLCuV)",
            description = "Upward curling leaves, vein thickening, and stunted growth caused by whitefly vector.",
            leafColor = Color.rgb(65, 120, 55),
            spotColor = Color.rgb(180, 190, 70)
        ),
        CropSamplePreset(
            id = "wheat_yellow_rust",
            title = "Wheat - Yellow Rust",
            cropName = "Wheat (Gandum)",
            diseaseSuspected = "Yellow/Stripe Rust (Puccinia striiformis)",
            description = "Parallel yellow-orange pustules aligned along leaf veins, prevalent in cool humid Pakistani winters.",
            leafColor = Color.rgb(80, 140, 60),
            spotColor = Color.rgb(240, 175, 30)
        ),
        CropSamplePreset(
            id = "rice_blight",
            title = "Rice - Bacterial Leaf Blight",
            cropName = "Rice (Dhan/Chawal)",
            diseaseSuspected = "Bacterial Blight (Xanthomonas oryzae)",
            description = "Water-soaked to yellowish-white lesions with wavy margins along leaf margins.",
            leafColor = Color.rgb(70, 135, 65),
            spotColor = Color.rgb(205, 160, 80)
        ),
        CropSamplePreset(
            id = "sugarcane_red_rot",
            title = "Sugarcane - Red Rot",
            cropName = "Sugarcane (Kamad)",
            diseaseSuspected = "Red Rot (Colletotrichum falcatum)",
            description = "Red lesions with white centers on midribs and internal stalk reddening with sour alcohol smell.",
            leafColor = Color.rgb(85, 145, 60),
            spotColor = Color.rgb(190, 40, 40)
        ),
        CropSamplePreset(
            id = "citrus_canker",
            title = "Citrus - Citrus Canker",
            cropName = "Citrus (Kinnow/Malta)",
            diseaseSuspected = "Citrus Canker (Xanthomonas axonopodis)",
            description = "Raised corky lesions with oily margins and bright yellow halos on leaves and fruit peel.",
            leafColor = Color.rgb(50, 115, 45),
            spotColor = Color.rgb(160, 100, 30)
        ),
        CropSamplePreset(
            id = "healthy_crop",
            title = "Healthy Cotton Plant",
            cropName = "Cotton (Kapas)",
            diseaseSuspected = "Healthy Vigorous Growth",
            description = "Deep green vibrant foliage with no visible pest feeding or fungal lesions.",
            leafColor = Color.rgb(35, 130, 45),
            spotColor = Color.rgb(45, 150, 55)
        )
    )

    fun generateSampleBitmap(preset: CropSamplePreset): Bitmap {
        val width = 500
        val height = 500
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw soil background
        val bgPaint = Paint().apply {
            color = Color.rgb(235, 240, 230)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Draw stem
        val stemPaint = Paint().apply {
            color = Color.rgb(90, 75, 45)
            strokeWidth = 14f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val stemPath = Path().apply {
            moveTo(250f, 500f)
            cubicTo(250f, 380f, 240f, 260f, 250f, 80f)
        }
        canvas.drawPath(stemPath, stemPaint)

        // Draw main leaf blade
        val leafPaint = Paint().apply {
            color = preset.leafColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val leafPath = Path().apply {
            moveTo(250f, 90f)
            cubicTo(100f, 150f, 90f, 320f, 250f, 430f)
            cubicTo(410f, 320f, 400f, 150f, 250f, 90f)
            close()
        }
        canvas.drawPath(leafPath, leafPaint)

        // Draw veins
        val veinPaint = Paint().apply {
            color = Color.argb(90, 255, 255, 255)
            strokeWidth = 4f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawLine(250f, 100f, 250f, 420f, veinPaint)
        for (y in 160..380 step 45) {
            canvas.drawLine(250f, y.toFloat(), 150f, (y - 30).toFloat(), veinPaint)
            canvas.drawLine(250f, y.toFloat(), 350f, (y - 30).toFloat(), veinPaint)
        }

        // Draw disease symptom spots
        val spotPaint = Paint().apply {
            color = preset.spotColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val spotHaloPaint = Paint().apply {
            color = Color.argb(120, 245, 230, 100)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        if (preset.id != "healthy_crop") {
            val randomCoords = listOf(
                Pair(190f, 220f), Pair(280f, 260f), Pair(220f, 310f),
                Pair(320f, 210f), Pair(180f, 280f), Pair(270f, 340f),
                Pair(230f, 180f), Pair(310f, 300f), Pair(160f, 250f)
            )
            for (coord in randomCoords) {
                canvas.drawCircle(coord.first, coord.second, 22f, spotHaloPaint)
                canvas.drawCircle(coord.first, coord.second, 12f, spotPaint)
            }
        }

        return bitmap
    }
}
