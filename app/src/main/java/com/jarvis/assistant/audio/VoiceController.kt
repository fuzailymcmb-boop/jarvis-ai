package com.jarvis.assistant.audio

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Push-to-talk. No unattended microphone recording, no hot mic or wake word. */
class VoiceController(
    private val context: Context,
    private val onText: (String) -> Unit,
    private val onStatus: (String) -> Unit,
    private val onListening: (Boolean) -> Unit
) {
    private var recognizer: SpeechRecognizer? = null
    private var ttsReady = false
    private val tts = TextToSpeech(context) { status -> ttsReady = status == TextToSpeech.SUCCESS }

    fun speak(text: String) {
        if (ttsReady) {
            val spoken = text.take(1200)
            tts.speak(spoken, TextToSpeech.QUEUE_FLUSH, null, "jarvis_spoken_reply")
        }
    }

    fun listen() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onStatus("Speech recognition is unavailable. Install or enable a speech recognition service.")
            return
        }
        if (recognizer == null) {
            recognizer = if (Build.VERSION.SDK_INT >= 31 &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
            ) SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            else SpeechRecognizer.createSpeechRecognizer(context)
            recognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { onStatus("Listening…") }
                override fun onBeginningOfSpeech() { }
                override fun onRmsChanged(rmsdB: Float) { }
                override fun onBufferReceived(buffer: ByteArray?) { }
                override fun onEndOfSpeech() { onStatus("Processing speech…") }
                override fun onError(error: Int) {
                    onListening(false)
                    val message = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                            "I didn't catch that. Tap the microphone and try again."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
                        else -> "Speech recognition error ($error). Try again."
                    }
                    onStatus(message)
                }
                override fun onResults(results: Bundle?) {
                    onListening(false)
                    val phrase = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()?.trim().orEmpty()
                    if (phrase.isNotEmpty()) onText(phrase)
                    else onStatus("No speech detected.")
                }
                override fun onPartialResults(partialResults: Bundle?) { }
                override fun onEvent(eventType: Int, params: Bundle?) { }
            })
        }
        tts.stop()
        onListening(true)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try { recognizer?.startListening(intent) }
        catch (error: Exception) {
            onListening(false)
            onStatus("Unable to start listening: ${error.localizedMessage.orEmpty()}")
        }
    }

    fun close() {
        stop()
        recognizer?.destroy()
        recognizer = null
        tts.shutdown()
    }

    fun stop() {
        recognizer?.cancel()
        onListening(false)
        tts.stop()
    }
}
