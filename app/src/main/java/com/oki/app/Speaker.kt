package com.oki.app

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import java.util.Locale
import java.util.UUID

/** Lee frases en voz alta con el motor de voz del teléfono (español). */
class Speaker(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private val pending = ArrayDeque<String>()

    override fun onInit(status: Int) {
        val engine = tts ?: return
        if (status != TextToSpeech.SUCCESS) return

        var result = engine.setLanguage(Locale("es", "PE"))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            result = engine.setLanguage(Locale("es"))
        }
        engine.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        ready = true
        while (pending.isNotEmpty()) speak(pending.removeFirst())
    }

    fun speak(text: String) {
        val engine = tts
        if (!ready || engine == null) {
            pending.addLast(text)
            return
        }
        engine.speak(text, TextToSpeech.QUEUE_ADD, null, UUID.randomUUID().toString())
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
