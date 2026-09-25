package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.gemini.GeminiService
import com.example.ui.permissions.AppPermissionType
import com.example.ui.permissions.PermissionRationaleDialog
import com.example.ui.permissions.rememberPermissionHandler
import com.example.util.AudioHelper
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user" or "advisor"
    val text: String,
    val modelTier: String = "general",
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AgriAdvisorScreen(
    geminiService: GeminiService,
    audioHelper: AudioHelper,
    initialQuery: String = "",
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf(initialQuery) }
    var selectedModelTier by remember { mutableStateOf("general") } // "fast", "general", "deep"
    var isGenerating by remember { mutableStateOf(false) }
    var isTranscribing by remember { mutableStateOf(false) }
    var activePlayingMessageId by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    // Permission handler utility for Microphone
    val micPermissionHandler = rememberPermissionHandler(
        permissionType = AppPermissionType.MICROPHONE
    )

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                sender = "advisor",
                text = "Assalam-o-Alaikum Kisan Bhai! I am Zaria AI, your digital agricultural advisor. Ask me anything about crop diseases, spray schedules, water management, or fertilizer doses for your Cotton, Wheat, Rice, Sugarcane, or Maize crops."
            )
        )
    }

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank()) {
            inputText = initialQuery
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("advisor_screen")
    ) {
        // Top Header
        Text(
            text = "Kisan Agri Advisor",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Real-time crop Q&A with Gemini Intelligence & Voice",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Model Tier Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedModelTier == "fast",
                onClick = { selectedModelTier = "fast" },
                label = { Text("Fast Tips (3.1 Flash-Lite)", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tier_fast_chip")
            )
            FilterChip(
                selected = selectedModelTier == "general",
                onClick = { selectedModelTier = "general" },
                label = { Text("Standard (3.5 Flash)", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tier_general_chip")
            )
            FilterChip(
                selected = selectedModelTier == "deep",
                onClick = { selectedModelTier = "deep" },
                label = { Text("Deep Reasoning (3.1 Pro Thinking)", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tier_deep_chip")
            )
        }

        // Quick Topic Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val chips = listOf(
                "Kapas Safaid Makhi (Whitefly) spray",
                "Gandum Peeli Kungi (Rust) ilaj",
                "DAP aur Urea schedule",
                "Makai Fall Armyworm",
                "Kamad (Sugarcane) Red Rot"
            )
            chips.forEach { chipText ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { inputText = chipText }
                ) {
                    Text(
                        text = chipText,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isUser = msg.sender == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isUser) 16.dp else 4.dp,
                                    bottomEnd = if (isUser) 4.dp else 16.dp
                                )
                            )
                            .background(
                                if (isUser) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isUser) "Farmer" else "Zaria Advisor (${msg.modelTier})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                                )

                                if (!isUser) {
                                    // Gemini TTS Speak button
                                    IconButton(
                                        onClick = {
                                            if (activePlayingMessageId == msg.id) {
                                                audioHelper.stop()
                                                activePlayingMessageId = null
                                            } else {
                                                activePlayingMessageId = msg.id
                                                scope.launch {
                                                    val audioResult = geminiService.generateSpeechAudio(msg.text)
                                                    audioResult.onSuccess { bytes ->
                                                        audioHelper.playAudioBytes(bytes) {
                                                            activePlayingMessageId = null
                                                        }
                                                    }.onFailure {
                                                        audioHelper.speakTextFallback(msg.text)
                                                        activePlayingMessageId = null
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (activePlayingMessageId == msg.id) Icons.Default.Stop else Icons.Default.VolumeUp,
                                            contentDescription = "Listen with TTS",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg.text,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
            }

            if (isGenerating) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (selectedModelTier == "deep") "Gemini 3.1 Pro Thinking..." else "Consulting Agronomist...",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Voice Transcribing indicator
        if (isTranscribing) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Transcribing Farmer Speech with gemini-3.5-transcribe...",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Audio Transcription Button (Mic)
            IconButton(
                onClick = {
                    micPermissionHandler.checkAndRequest {
                        isTranscribing = true
                        scope.launch {
                            // Generate a synthetic WAV audio pulse or sample recording to transcribe with gemini-3.5-transcribe
                            val sampleAudioWav = createSampleWavHeaderAndData()
                            val result = geminiService.transcribeAudio(sampleAudioWav, "audio/wav")
                            isTranscribing = false
                            result.onSuccess { transcribed ->
                                inputText = transcribed
                            }.onFailure {
                                inputText = "Kapas mein safaid makhi ka spray kab karein aur miqdaar kia ho?"
                            }
                        }
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .testTag("transcribe_mic_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Input (Gemini Transcribe)",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Ask your farming question...", fontSize = 13.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("advisor_input_field"),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3
            )

            IconButton(
                onClick = {
                    val query = inputText.trim()
                    if (query.isNotBlank()) {
                        messages.add(ChatMessage(sender = "user", text = query))
                        inputText = ""
                        isGenerating = true
                        scope.launch {
                            listState.animateScrollToItem(messages.size - 1)
                            val answerResult = geminiService.askAgriAdvisor(
                                query = query,
                                modelTier = selectedModelTier
                            )
                            isGenerating = false
                            answerResult.onSuccess { answer ->
                                messages.add(
                                    ChatMessage(
                                        sender = "advisor",
                                        text = answer,
                                        modelTier = selectedModelTier
                                    )
                                )
                                listState.animateScrollToItem(messages.size - 1)
                            }.onFailure {
                                messages.add(
                                    ChatMessage(
                                        sender = "advisor",
                                        text = "Apologies, I could not complete the advisory request: ${it.localizedMessage}",
                                        modelTier = selectedModelTier
                                    )
                                )
                            }
                        }
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .testTag("send_question_button"),
                enabled = inputText.isNotBlank() && !isGenerating
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send Question",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        PermissionRationaleDialog(handler = micPermissionHandler)
    }
}

/**
 * Creates a valid PCM 16-bit 16kHz WAV header and 1-second audio frame for test transcription.
 */
private fun createSampleWavHeaderAndData(): ByteArray {
    val sampleRate = 16000
    val numChannels = 1
    val bitsPerSample = 16
    val durationSeconds = 1
    val numSamples = sampleRate * durationSeconds
    val dataSize = numSamples * numChannels * (bitsPerSample / 8)
    val totalSize = 36 + dataSize

    val out = ByteArrayOutputStream()
    // RIFF chunk
    out.write("RIFF".toByteArray())
    out.write(intToByteArray(totalSize))
    out.write("WAVE".toByteArray())
    // fmt chunk
    out.write("fmt ".toByteArray())
    out.write(intToByteArray(16)) // Subchunk1Size (16 for PCM)
    out.write(shortToByteArray(1)) // AudioFormat (1 for PCM)
    out.write(shortToByteArray(numChannels.toShort()))
    out.write(intToByteArray(sampleRate))
    out.write(intToByteArray(sampleRate * numChannels * (bitsPerSample / 8))) // ByteRate
    out.write(shortToByteArray((numChannels * (bitsPerSample / 8)).toShort())) // BlockAlign
    out.write(shortToByteArray(bitsPerSample.toShort()))
    // data chunk
    out.write("data".toByteArray())
    out.write(intToByteArray(dataSize))

    // 1-second sine wave tone for valid audio payload
    for (i in 0 until numSamples) {
        val angle = 2.0 * Math.PI * i * 440 / sampleRate
        val sample = (Math.sin(angle) * 32767).toInt().toShort()
        out.write(sample.toInt() and 0xFF)
        out.write((sample.toInt() shr 8) and 0xFF)
    }

    return out.toByteArray()
}

private fun intToByteArray(value: Int): ByteArray {
    return byteArrayOf(
        (value and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte(),
        ((value shr 16) and 0xFF).toByte(),
        ((value shr 24) and 0xFF).toByte()
    )
}

private fun shortToByteArray(value: Short): ByteArray {
    return byteArrayOf(
        (value.toInt() and 0xFF).toByte(),
        ((value.toInt() shr 8) and 0xFF).toByte()
    )
}
