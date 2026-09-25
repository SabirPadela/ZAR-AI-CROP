package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class CropScan(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val cropType: String = "",
    val diseaseName: String = "",
    val confidence: Double = 0.0,
    val severity: String = "Moderate",
    val symptoms: String = "",
    val treatmentAdvice: String = "",
    val thinkingNotes: String = "",
    val imageUri: String = "",
    val locationName: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)

data class FieldNote(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val cropName: String = "",
    val plotSizeAcres: Double = 0.0,
    val noteContent: String = "",
    val audioTranscript: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)

data class UserProfile(
    @DocumentId
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val region: String = "Punjab - Multan",
    val primaryCrops: List<String> = emptyList(),
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)

data class MandiRateItem(
    val cropName: String = "",
    val variety: String = "",
    val marketCity: String = "",
    val minPrice: String = "",
    val maxPrice: String = "",
    val unit: String = "Rs. / 40 Kg",
    val trend: String = "Stable",
    val notes: String = ""
)

data class KisanCenterItem(
    val name: String = "",
    val type: String = "Agri Extension",
    val city: String = "",
    val address: String = "",
    val phone: String = "",
    val services: String = "",
    val rating: Double = 4.5
)

data class CropAnalysisResult(
    val cropType: String,
    val diseaseName: String,
    val confidence: Double,
    val severity: String,
    val symptoms: String,
    val treatmentAdvice: String,
    val thinkingNotes: String,
    val chemicalSprays: List<String> = emptyList(),
    val biologicalControls: List<String> = emptyList(),
    val precautionsUrdu: String = ""
)
