package com.example.data.repository

import com.example.data.local.ScanResultDao
import com.example.data.local.ScanResultEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository adhering to the Repository Pattern to abstract local Room database operations
 * from UI and ViewModel components.
 */
class LocalScanRepository(
    private val scanResultDao: ScanResultDao
) {
    /**
     * Reactive stream of all local scans ordered by newest first.
     */
    val allScans: Flow<List<ScanResultEntity>> = scanResultDao.getAllScanResults()

    /**
     * Total number of scans saved on the device.
     */
    val scanCount: Flow<Int> = scanResultDao.getScanCount()

    /**
     * Stream of scans that need synchronization to cloud storage.
     */
    val unsyncedScans: Flow<List<ScanResultEntity>> = scanResultDao.getUnsyncedScanResults()

    /**
     * Observes a single scan by ID.
     */
    fun getScanById(id: Long): Flow<ScanResultEntity?> = scanResultDao.getScanResultById(id)

    /**
     * Direct one-shot retrieval by ID.
     */
    suspend fun getScanByIdDirect(id: Long): ScanResultEntity? = scanResultDao.getScanResultByIdDirect(id)

    /**
     * Filters scans by disease prediction tag.
     */
    fun getScansByTag(tag: String): Flow<List<ScanResultEntity>> = scanResultDao.getScanResultsByTag(tag)

    /**
     * Inserts a scan result into Room.
     */
    suspend fun insertScan(
        diseasePredictionTag: String,
        confidenceScore: Float,
        timestamp: Long = System.currentTimeMillis(),
        cropType: String = "",
        imagePath: String? = null,
        severity: String = "Moderate",
        treatmentAdvice: String = "",
        notes: String = ""
    ): Long {
        val entity = ScanResultEntity(
            diseasePredictionTag = diseasePredictionTag,
            confidenceScore = confidenceScore,
            timestamp = timestamp,
            cropType = cropType,
            imagePath = imagePath,
            severity = severity,
            treatmentAdvice = treatmentAdvice,
            notes = notes
        )
        return scanResultDao.insertScanResult(entity)
    }

    suspend fun insert(scanResult: ScanResultEntity): Long {
        return scanResultDao.insertScanResult(scanResult)
    }

    suspend fun updateSyncStatus(id: Long, isSynced: Boolean) {
        scanResultDao.updateSyncStatus(id, isSynced)
    }

    suspend fun deleteById(id: Long) {
        scanResultDao.deleteById(id)
    }

    suspend fun clearAll() {
        scanResultDao.clearAllScanResults()
    }
}
