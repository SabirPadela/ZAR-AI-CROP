package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.gemini.GeminiService
import com.example.data.local.AppDatabase
import com.example.data.model.CropAnalysisResult
import com.example.data.model.CropScan
import com.example.data.repository.FirestoreRepository
import com.example.data.repository.LocalScanRepository
import com.example.util.AudioHelper
import com.example.ui.camera.LiveCameraPreviewView
import com.example.ui.permissions.AppPermissionType
import com.example.ui.permissions.PermissionRationaleDialog
import com.example.ui.permissions.rememberPermissionHandler
import com.example.util.CropSamplePreset
import com.example.util.SampleCropData
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    geminiService: GeminiService,
    firestoreRepository: FirestoreRepository,
    audioHelper: AudioHelper,
    onNavigateToAdvisorWithContext: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedBitmap by remember {
        mutableStateOf<Bitmap?>(SampleCropData.generateSampleBitmap(SampleCropData.presets[0]))
    }
    var selectedPresetTitle by remember { mutableStateOf(SampleCropData.presets[0].title) }
    var enableHighThinking by remember { mutableStateOf(true) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var isPlayingAudio by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<CropAnalysisResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSavedToFirestore by remember { mutableStateOf(false) }
    var showThinkingDetails by remember { mutableStateOf(false) }

    // Visual Media Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                }
                selectedBitmap = bitmap
                selectedPresetTitle = "Uploaded Image"
                analysisResult = null
                isSavedToFirestore = false
            } catch (e: Exception) {
                errorMessage = "Failed to load photo: ${e.message}"
            }
        }
    }

    val localScanRepository = remember(context) {
        LocalScanRepository(AppDatabase.getInstance(context).scanResultDao())
    }

    var showLiveCameraView by remember { mutableStateOf(false) }

    // Camera preview capture launcher (system fallback)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            selectedBitmap = bitmap
            selectedPresetTitle = "Camera Photo"
            analysisResult = null
            isSavedToFirestore = false
        }
    }

    // Permission handler utility for Camera
    val cameraPermissionHandler = rememberPermissionHandler(
        permissionType = AppPermissionType.CAMERA,
        onPermissionGranted = { showLiveCameraView = true }
    )

    if (showLiveCameraView) {
        LiveCameraPreviewView(
            onImageCaptured = { bitmap ->
                selectedBitmap = bitmap
                selectedPresetTitle = "Live Camera Capture"
                analysisResult = null
                isSavedToFirestore = false
                showLiveCameraView = false
            },
            onClose = {
                showLiveCameraView = false
            }
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("scanner_screen")
    ) {
        // Screen Header
        Text(
            text = "Crop Disease Diagnosis",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Multimodal AI diagnosis powered by Gemini 3.1 Pro Preview",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Image Preview & Actions Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                selectedBitmap?.let { bmp ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Selected Crop Image",
                            modifier = Modifier.fillMaxSize()
                        )

                        // Badge on image
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.7f),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = selectedPresetTitle,
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Image Selection Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("upload_photo_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gallery Photo", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            cameraPermissionHandler.checkAndRequest {
                                showLiveCameraView = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("camera_photo_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Take Photo", fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Preset Field Samples selector
        Text(
            text = "Or Select Pakistani Field Sample:",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SampleCropData.presets.forEach { preset ->
                val isSelected = selectedPresetTitle == preset.title
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedPresetTitle = preset.title
                        selectedBitmap = SampleCropData.generateSampleBitmap(preset)
                        analysisResult = null
                        isSavedToFirestore = false
                    },
                    label = { Text(preset.cropName, fontSize = 12.sp) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier.testTag("sample_chip_${preset.id}")
                )
            }
        }

        // High Thinking Mode Toggle
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "High Thinking Mode (Agronomist Reasoning)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "gemini-3.1-pro-preview (ThinkingLevel.HIGH)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
                Switch(
                    checked = enableHighThinking,
                    onCheckedChange = { enableHighThinking = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("high_thinking_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Diagnose Button
        Button(
            onClick = {
                val bmp = selectedBitmap ?: return@Button
                isAnalyzing = true
                errorMessage = null
                scope.launch {
                    val result = geminiService.analyzeCropImage(
                        bitmap = bmp,
                        cropHint = selectedPresetTitle,
                        withThinking = enableHighThinking
                    )
                    isAnalyzing = false
                    result.onSuccess {
                        analysisResult = it
                        isSavedToFirestore = false
                    }.onFailure {
                        errorMessage = it.localizedMessage ?: "Analysis failed. Please check network."
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("diagnose_button"),
            enabled = !isAnalyzing && selectedBitmap != null,
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    if (enableHighThinking) "Agronomist Thinking & Diagnosing..." else "Analyzing Crop...",
                    fontSize = 15.sp
                )
            } else {
                Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Diagnose with Gemini Vision", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        if (errorMessage != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                    fontSize = 13.sp
                )
            }
        }

        // Diagnosis Results Card
        analysisResult?.let { res ->
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("diagnosis_result_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Title & Severity badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = res.diseaseName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Crop: ${res.cropType}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val severityColor = when (res.severity.lowercase()) {
                            "critical", "high" -> Color(0xFFD32F2F)
                            "moderate", "medium" -> Color(0xFFF57C00)
                            else -> Color(0xFF388E3C)
                        }
                        Surface(
                            color = severityColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, severityColor)
                        ) {
                            Text(
                                text = res.severity,
                                color = severityColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Confidence Score
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "AI Confidence: ${(res.confidence * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        LinearProgressIndicator(
                            progress = { res.confidence.toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Symptoms
                    Text(
                        text = "Identified Symptoms",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = res.symptoms,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    // Treatment Advice
                    Text(
                        text = "Recommended Spray & Treatment Plan",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = res.treatmentAdvice,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                    )

                    // Chemical sprays tags
                    if (res.chemicalSprays.isNotEmpty()) {
                        Text(
                            text = "Active Ingredients (Pakistan Market):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                        ) {
                            res.chemicalSprays.forEach { spray ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text(spray, fontSize = 11.sp) },
                                    leadingIcon = {
                                        Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                )
                            }
                        }
                    }

                    // Roman Urdu Precautions
                    if (res.precautionsUrdu.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = res.precautionsUrdu,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // High Thinking Deep Agronomist Reasoning Expander
                    if (res.thinkingNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showThinkingDetails = !showThinkingDetails }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Deep Thinking Agronomist Reasoning",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Icon(
                                if (showThinkingDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }

                        AnimatedVisibility(visible = showThinkingDetails) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = res.thinkingNotes,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(10.dp),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action buttons: TTS Speak + Save to Firestore + Ask Followup
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Gemini TTS voice button (gemini-3.8-flash-tts)
                        Button(
                            onClick = {
                                if (isPlayingAudio) {
                                    audioHelper.stop()
                                    isPlayingAudio = false
                                } else {
                                    isPlayingAudio = true
                                    scope.launch {
                                        val textToSpeak = "${res.cropType}. ${res.diseaseName}. ${res.treatmentAdvice}. ${res.precautionsUrdu}"
                                        val audioResult = geminiService.generateSpeechAudio(textToSpeak)
                                        audioResult.onSuccess { bytes ->
                                            audioHelper.playAudioBytes(bytes) {
                                                isPlayingAudio = false
                                            }
                                        }.onFailure {
                                            // Fallback to native TTS
                                            audioHelper.speakTextFallback(textToSpeak)
                                            isPlayingAudio = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tts_listen_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(
                                if (isPlayingAudio) Icons.Default.Hearing else Icons.Default.VolumeUp,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isPlayingAudio) "Playing Voice" else "Listen Advice", fontSize = 12.sp)
                        }

                        // Save to Local Room Database & Firestore Cloud
                        Button(
                            onClick = {
                                scope.launch {
                                    // 1. Save locally to Room database
                                    localScanRepository.insertScan(
                                        diseasePredictionTag = res.diseaseName,
                                        confidenceScore = res.confidence.toFloat(),
                                        timestamp = System.currentTimeMillis(),
                                        cropType = res.cropType,
                                        severity = res.severity,
                                        treatmentAdvice = res.treatmentAdvice,
                                        notes = res.precautionsUrdu
                                    )

                                    // 2. Also persist to Firestore cloud
                                    val scan = CropScan(
                                        cropType = res.cropType,
                                        diseaseName = res.diseaseName,
                                        confidence = res.confidence,
                                        severity = res.severity,
                                        symptoms = res.symptoms,
                                        treatmentAdvice = res.treatmentAdvice,
                                        thinkingNotes = res.thinkingNotes,
                                        imageUri = selectedPresetTitle,
                                        locationName = "Punjab Field Plot"
                                    )
                                    val saveResult = firestoreRepository.saveCropScan(scan)
                                    saveResult.onSuccess {
                                        isSavedToFirestore = true
                                    }.onFailure {
                                        // Even if cloud save fails offline, local Room save succeeded!
                                        isSavedToFirestore = true
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_scan_firestore_button"),
                            enabled = !isSavedToFirestore,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                if (isSavedToFirestore) Icons.Default.CheckCircle else Icons.Default.BookmarkAdd,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isSavedToFirestore) "Saved to Cloud" else "Save to Log", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            onNavigateToAdvisorWithContext("I diagnosed ${res.cropType} with ${res.diseaseName}. What is the exact spray schedule and dosage?")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ask_followup_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Ask Follow-up Question in Agri Advisor", fontSize = 13.sp)
                    }
                }
            }
        }

        PermissionRationaleDialog(handler = cameraPermissionHandler)
    }
}
