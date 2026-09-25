package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for local scan results in Room database.
 */
@Dao
interface ScanResultDao {

    /**
     * Observes all locally stored scan results ordered by descending timestamp.
     */
    @Query("SELECT * FROM scan_results ORDER BY timestamp DESC")
    fun getAllScanResults(): Flow<List<ScanResultEntity>>

    /**
     * Observes a single scan result by its unique ID.
     */
    @Query("SELECT * FROM scan_results WHERE id = :id LIMIT 1")
    fun getScanResultById(id: Long): Flow<ScanResultEntity?>

    /**
     * Fetches a single scan result by its unique ID directly (one-shot).
     */
    @Query("SELECT * FROM scan_results WHERE id = :id LIMIT 1")
    suspend fun getScanResultByIdDirect(id: Long): ScanResultEntity?

    /**
     * Filters scan results by a specific disease prediction tag.
     */
    @Query("SELECT * FROM scan_results WHERE disease_prediction_tag = :tag ORDER BY timestamp DESC")
    fun getScanResultsByTag(tag: String): Flow<List<ScanResultEntity>>

    /**
     * Retrieves all scans that haven't been synchronized to remote storage yet.
     */
    @Query("SELECT * FROM scan_results WHERE is_synced = 0 ORDER BY timestamp ASC")
    fun getUnsyncedScanResults(): Flow<List<ScanResultEntity>>

    /**
     * Observes the total count of saved scans.
     */
    @Query("SELECT COUNT(*) FROM scan_results")
    fun getScanCount(): Flow<Int>

    /**
     * Inserts or replaces a local scan result.
     * @return The auto-generated row ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanResult(scanResult: ScanResultEntity): Long

    /**
     * Inserts a list of scan results.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(scanResults: List<ScanResultEntity>): List<Long>

    /**
     * Updates an existing scan result.
     */
    @Update
    suspend fun updateScanResult(scanResult: ScanResultEntity)

    /**
     * Updates the sync status of a specific scan.
     */
    @Query("UPDATE scan_results SET is_synced = :synced WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, synced: Boolean)

    /**
     * Deletes a specific scan result entity.
     */
    @Delete
    suspend fun deleteScanResult(scanResult: ScanResultEntity)

    /**
     * Deletes a scan result by its ID.
     */
    @Query("DELETE FROM scan_results WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Clears all scan results from the database.
     */
    @Query("DELETE FROM scan_results")
    suspend fun clearAllScanResults()
}
