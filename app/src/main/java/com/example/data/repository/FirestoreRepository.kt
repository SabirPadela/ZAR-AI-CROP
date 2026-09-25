package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.CropScan
import com.example.data.model.FieldNote
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

enum class OperationType {
    CREATE, READ, UPDATE, DELETE, LIST
}

fun handleFirestoreError(e: Exception, operationType: OperationType, path: String): String {
    val message = "Firestore operation $operationType failed at path: $path: ${e.message}"
    Log.e("FirestoreRepository", message, e)
    return when {
        e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true ->
            "Access denied: You do not have permission to perform this action."
        e.message?.contains("NOT_FOUND", ignoreCase = true) == true ->
            "Record not found."
        e.message?.contains("UNAVAILABLE", ignoreCase = true) == true ->
            "Network unavailable. Your changes will sync once online."
        else -> e.localizedMessage ?: "A database error occurred."
    }
}

class FirestoreRepository(private val context: Context) {
    private val databaseId: String by lazy {
        try {
            context.getString(R.string.firestore_database_id)
        } catch (_: Exception) {
            "(default)"
        }
    }
    private val db: FirebaseFirestore?
        get() = try {
            if (com.google.firebase.FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseFirestore.getInstance(databaseId)
            } else null
        } catch (_: Exception) { null }

    private val auth: FirebaseAuth?
        get() = try {
            if (com.google.firebase.FirebaseApp.getApps(context).isNotEmpty()) {
                Firebase.auth
            } else null
        } catch (_: Exception) { null }

    private fun requireFirestore(): FirebaseFirestore {
        return db ?: throw IllegalStateException("Firebase Firestore is not initialized.")
    }

    private fun requireUserId(): String {
        return auth?.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeCropScans(): Flow<List<CropScan>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/scans"
        emitAll(
            requireFirestore().collection("users").document(uid).collection("scans")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(CropScan::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveCropScan(scan: CropScan): Result<String> {
        return try {
            val uid = requireUserId()
            val collectionRef = requireFirestore().collection("users").document(uid).collection("scans")
            val docRef = if (scan.id.isNotBlank()) collectionRef.document(scan.id) else collectionRef.document()

            val payload = hashMapOf<String, Any>(
                "id" to docRef.id,
                "userId" to uid,
                "cropType" to scan.cropType,
                "diseaseName" to scan.diseaseName,
                "confidence" to scan.confidence,
                "severity" to scan.severity,
                "symptoms" to scan.symptoms,
                "treatmentAdvice" to scan.treatmentAdvice,
                "thinkingNotes" to scan.thinkingNotes,
                "imageUri" to scan.imageUri,
                "locationName" to scan.locationName,
                "createdAt" to (scan.createdAt ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            val msg = handleFirestoreError(e, OperationType.CREATE, "users/${auth?.currentUser?.uid}/scans")
            Result.failure(Exception(msg, e))
        }
    }

    suspend fun deleteCropScan(scanId: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            val path = "users/$uid/scans/$scanId"
            requireFirestore().collection("users").document(uid).collection("scans").document(scanId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            val msg = handleFirestoreError(e, OperationType.DELETE, "users/${auth?.currentUser?.uid}/scans/$scanId")
            Result.failure(Exception(msg, e))
        }
    }

    fun observeFieldNotes(): Flow<List<FieldNote>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/notes"
        emitAll(
            requireFirestore().collection("users").document(uid).collection("notes")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(FieldNote::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveFieldNote(note: FieldNote): Result<String> {
        return try {
            val uid = requireUserId()
            val collectionRef = requireFirestore().collection("users").document(uid).collection("notes")
            val docRef = if (note.id.isNotBlank()) collectionRef.document(note.id) else collectionRef.document()

            val payload = hashMapOf<String, Any>(
                "id" to docRef.id,
                "userId" to uid,
                "cropName" to note.cropName,
                "plotSizeAcres" to note.plotSizeAcres,
                "noteContent" to note.noteContent,
                "audioTranscript" to note.audioTranscript,
                "createdAt" to (note.createdAt ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            val msg = handleFirestoreError(e, OperationType.CREATE, "users/${auth?.currentUser?.uid}/notes")
            Result.failure(Exception(msg, e))
        }
    }

    suspend fun deleteFieldNote(noteId: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            val path = "users/$uid/notes/$noteId"
            requireFirestore().collection("users").document(uid).collection("notes").document(noteId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            val msg = handleFirestoreError(e, OperationType.DELETE, "users/${auth?.currentUser?.uid}/notes/$noteId")
            Result.failure(Exception(msg, e))
        }
    }

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        return try {
            val uid = requireUserId()
            val path = "users/$uid"
            val payload = hashMapOf<String, Any>(
                "userId" to uid,
                "displayName" to profile.displayName,
                "email" to profile.email,
                "region" to profile.region,
                "primaryCrops" to profile.primaryCrops,
                "createdAt" to (profile.createdAt ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            requireFirestore().collection("users").document(uid).set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            val msg = handleFirestoreError(e, OperationType.UPDATE, "users/${auth?.currentUser?.uid}")
            Result.failure(Exception(msg, e))
        }
    }
}
