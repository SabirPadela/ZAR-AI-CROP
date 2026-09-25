package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class AudioHelper(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    init {
        try {
            textToSpeech = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = textToSpeech?.setLanguage(Locale("ur", "PK"))
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        textToSpeech?.setLanguage(Locale.US)
                    }
                    isTtsReady = true
                }
            }
        } catch (e: Exception) {
            Log.w("AudioHelper", "Failed to initialize native TTS: ${e.message}")
        }
    }

    fun playAudioBytes(audioBytes: ByteArray, onComplete: () -> Unit = {}) {
        stop()
        try {
            val tempFile = File.createTempFile("gemini_tts", ".mp3", context.cacheDir)
            FileOutputStream(tempFile).use { it.write(audioBytes) }

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(tempFile.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    tempFile.delete()
                    onComplete()
                }
            }
        } catch (e: Exception) {
            Log.e("AudioHelper", "Error playing audio bytes, falling back", e)
            onComplete()
        }
    }

    fun speakTextFallback(text: String) {
        stop()
        if (isTtsReady && textToSpeech != null) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ZariaTts")
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {}

        try {
            textToSpeech?.stop()
        } catch (_: Exception) {}
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true || textToSpeech?.isSpeaking == true
    }

    fun release() {
        stop()
        try {
            textToSpeech?.shutdown()
            textToSpeech = null
        } catch (_: Exception) {}
    }
}
