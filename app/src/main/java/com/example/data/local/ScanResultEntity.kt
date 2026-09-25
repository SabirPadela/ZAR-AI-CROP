package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Database entity representing a local crop disease scan result.
 * Persists prediction tag, confidence score, timestamp, and metadata.
 */
@Entity(tableName = "scan_results")
data class ScanResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "disease_prediction_tag")
    val diseasePredictionTag: String,

    @ColumnInfo(name = "confidence_score")
    val confidenceScore: Float,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "crop_type")
    val cropType: String = "",

    @ColumnInfo(name = "image_path")
    val imagePath: String? = null,

    @ColumnInfo(name = "severity")
    val severity: String = "Moderate",

    @ColumnInfo(name = "treatment_advice")
    val treatmentAdvice: String = "",

    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,

    @ColumnInfo(name = "notes")
    val notes: String = ""
)
