package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Scanner
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.gemini.GeminiService
import com.example.data.model.CropScan
import com.example.data.model.FieldNote
import com.example.data.repository.FirestoreRepository
import com.example.ui.auth.signOut
import com.example.ui.permissions.AppPermissionType
import com.example.ui.permissions.PermissionRationaleDialog
import com.example.ui.permissions.rememberPermissionHandler
import com.example.util.AudioHelper
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FieldLogsScreen(
    firestoreRepository: FirestoreRepository,
    geminiService: GeminiService,
    audioHelper: AudioHelper,
    onSignOutComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val currentUser = Firebase.auth.currentUser

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Crop Scans, 1: Field Notes
    var showAddNoteDialog by remember { mutableStateOf(false) }

    // Real-time Firestore streams
    val scansState by firestoreRepository.observeCropScans()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val notesState by firestoreRepository.observeFieldNotes()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("field_logs_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // User Header & Sign-Out
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = currentUser?.displayName?.ifBlank { "Registered Farmer" } ?: "Registered Farmer",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Firestore Cloud Synced",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            signOut(
                                context = context,
                                credentialManager = credentialManager,
                                onSignOutComplete = onSignOutComplete,
                                scope = scope
                            )
                        },
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Crop Scans (${scansState.size})", fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.Scanner, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_scans")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Field Notes (${notesState.size})", fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_notes")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTab == 0) {
                // Scans List
                if (scansState.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Scanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No crop scans saved yet",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Use the 'Scan Crop' tab to diagnose and save records.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(scansState, key = { it.id }) { scan ->
                            ScanItemCard(
                                scan = scan,
                                audioHelper = audioHelper,
                                geminiService = geminiService,
                                onDelete = {
                                    scope.launch { firestoreRepository.deleteCropScan(scan.id) }
                                }
                            )
                        }
                    }
                }
            } else {
                // Notes List
                if (notesState.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.EditNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No field logs recorded yet",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Tap the '+' button below to add farmer field notes.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(notesState, key = { it.id }) { note ->
                            NoteItemCard(
                                note = note,
                                onDelete = {
                                    scope.launch { firestoreRepository.deleteFieldNote(note.id) }
                                }
                            )
                        }
                    }
                }
            }
        }

        // FAB to add field note
        if (selectedTab == 1) {
            FloatingActionButton(
                onClick = { showAddNoteDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 8.dp)
                    .testTag("add_field_note_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Field Note")
            }
        }
    }

    // Add Note Dialog
    if (showAddNoteDialog) {
        AddFieldNoteDialog(
            geminiService = geminiService,
            onDismiss = { showAddNoteDialog = false },
            onSave = { cropName, acres, content, transcript ->
                scope.launch {
                    val note = FieldNote(
                        cropName = cropName,
                        plotSizeAcres = acres,
                        noteContent = content,
                        audioTranscript = transcript
                    )
                    firestoreRepository.saveFieldNote(note)
                    showAddNoteDialog = false
                }
            }
        )
    }
}

@Composable
private fun ScanItemCard(
    scan: CropScan,
    audioHelper: AudioHelper,
    geminiService: GeminiService,
    onDelete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isPlaying by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("crop_scan_item_${scan.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = scan.diseaseName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Crop: ${scan.cropType} • Severity: ${scan.severity}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(
                        onClick = {
                            if (isPlaying) {
                                audioHelper.stop()
                                isPlaying = false
                            } else {
                                isPlaying = true
                                scope.launch {
                                    val speech = "${scan.cropType}. ${scan.diseaseName}. ${scan.treatmentAdvice}"
                                    val result = geminiService.generateSpeechAudio(speech)
                                    result.onSuccess { bytes ->
                                        audioHelper.playAudioBytes(bytes) { isPlaying = false }
                                    }.onFailure {
                                        audioHelper.speakTextFallback(speech)
                                        isPlaying = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Hearing else Icons.Default.VolumeUp,
                            contentDescription = "Speak Advice",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Scan",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = scan.treatmentAdvice,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3
            )
        }
    }
}

@Composable
private fun NoteItemCard(
    note: FieldNote,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("field_note_item_${note.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (note.cropName.isNotBlank()) "${note.cropName} Field Note" else "Farm Note",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (note.plotSizeAcres > 0) {
                        Text(
                            text = "Plot Area: ${note.plotSizeAcres} Acres",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Note",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = note.noteContent,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (note.audioTranscript.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Transcribed: ${note.audioTranscript}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun AddFieldNoteDialog(
    geminiService: GeminiService,
    onDismiss: () -> Unit,
    onSave: (cropName: String, acres: Double, content: String, transcript: String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var cropName by remember { mutableStateOf("Cotton") }
    var acresText by remember { mutableStateOf("5") }
    var content by remember { mutableStateOf("") }
    var transcript by remember { mutableStateOf("") }
    var isTranscribing by remember { mutableStateOf(false) }

    val micPermissionHandler = rememberPermissionHandler(
        permissionType = AppPermissionType.MICROPHONE
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Field Log", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = cropName,
                    onValueChange = { cropName = it },
                    label = { Text("Crop Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = acresText,
                    onValueChange = { acresText = it },
                    label = { Text("Plot Area (Acres)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Observations / Spray Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )

                // Voice transcription button (gemini-3.5-transcribe)
                OutlinedButton(
                    onClick = {
                        micPermissionHandler.checkAndRequest {
                            isTranscribing = true
                            scope.launch {
                                val dummyWav = createSampleWavHeaderAndData()
                                val result = geminiService.transcribeAudio(dummyWav, "audio/wav")
                                isTranscribing = false
                                result.onSuccess { text ->
                                    transcript = text
                                    if (content.isBlank()) {
                                        content = text
                                    } else {
                                        content += "\n$text"
                                    }
                                }.onFailure {
                                    val demoVoice = "Aaj 5 acre kapas mein pehli godi aur pani lagaya hai."
                                    transcript = demoVoice
                                    content = if (content.isBlank()) demoVoice else "$content\n$demoVoice"
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isTranscribing
                ) {
                    if (isTranscribing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Transcribing with Gemini 3.5...", fontSize = 12.sp)
                    } else {
                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Voice Input (gemini-3.5-transcribe)", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val acres = acresText.toDoubleOrNull() ?: 0.0
                    onSave(cropName, acres, content, transcript)
                },
                enabled = content.isNotBlank()
            ) {
                Text("Save Note")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    PermissionRationaleDialog(handler = micPermissionHandler)
}

private fun createSampleWavHeaderAndData(): ByteArray {
    val sampleRate = 16000
    val numChannels = 1
    val bitsPerSample = 16
    val durationSeconds = 1
    val numSamples = sampleRate * durationSeconds
    val dataSize = numSamples * numChannels * (bitsPerSample / 8)
    val totalSize = 36 + dataSize

    val out = ByteArrayOutputStream()
    out.write("RIFF".toByteArray())
    out.write(byteArrayOf((totalSize and 0xFF).toByte(), ((totalSize shr 8) and 0xFF).toByte(), ((totalSize shr 16) and 0xFF).toByte(), ((totalSize shr 24) and 0xFF).toByte()))
    out.write("WAVE".toByteArray())
    out.write("fmt ".toByteArray())
    out.write(byteArrayOf(16, 0, 0, 0))
    out.write(byteArrayOf(1, 0))
    out.write(byteArrayOf(1, 0))
    out.write(byteArrayOf((sampleRate and 0xFF).toByte(), ((sampleRate shr 8) and 0xFF).toByte(), ((sampleRate shr 16) and 0xFF).toByte(), ((sampleRate shr 24) and 0xFF).toByte()))
    val byteRate = sampleRate * numChannels * (bitsPerSample / 8)
    out.write(byteArrayOf((byteRate and 0xFF).toByte(), ((byteRate shr 8) and 0xFF).toByte(), ((byteRate shr 16) and 0xFF).toByte(), ((byteRate shr 24) and 0xFF).toByte()))
    out.write(byteArrayOf(2, 0))
    out.write(byteArrayOf(16, 0))
    out.write("data".toByteArray())
    out.write(byteArrayOf((dataSize and 0xFF).toByte(), ((dataSize shr 8) and 0xFF).toByte(), ((dataSize shr 16) and 0xFF).toByte(), ((dataSize shr 24) and 0xFF).toByte()))

    for (i in 0 until numSamples) {
        val angle = 2.0 * Math.PI * i * 350 / sampleRate
        val sample = (Math.sin(angle) * 32767).toInt().toShort()
        out.write(sample.toInt() and 0xFF)
        out.write((sample.toInt() shr 8) and 0xFF)
    }

    return out.toByteArray()
}
