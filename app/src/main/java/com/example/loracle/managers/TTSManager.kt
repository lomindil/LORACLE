package com.example.loracle.managers

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.*

class TTSManager(context: Context) {

    private val tts: TextToSpeech = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
        }
    }

    fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_id")
    }

    fun shutdown() {
        tts.shutdown()
    }
}
