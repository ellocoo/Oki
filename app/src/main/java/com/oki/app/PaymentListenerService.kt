package com.oki.app

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** Lee las notificaciones de las apps de pago y anuncia en voz alta los pagos recibidos. */
class PaymentListenerService : NotificationListenerService() {

    private lateinit var prefs: OkiPrefs
    private var speaker: Speaker? = null

    private var lastKey = ""
    private var lastAt = 0L

    override fun onCreate() {
        super.onCreate()
        prefs = OkiPrefs(this)
        speaker = Speaker(this)
    }

    override fun onDestroy() {
        speaker?.shutdown()
        speaker = null
        super.onDestroy()
    }

    override fun onListenerDisconnected() {
        // Si Android desconecta el servicio, pedimos volver a conectarlo.
        requestRebind(ComponentName(this, PaymentListenerService::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!prefs.enabled) return
        if (sbn.packageName !in PaymentParser.MONITORED_PACKAGES) return
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val body = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        if (title.isBlank() && body.isBlank()) return

        // Algunas apps actualizan la misma notificación varias veces: evitamos repetir la voz.
        val key = "${sbn.packageName}|$title|$body"
        val now = System.currentTimeMillis()
        if (key == lastKey && now - lastAt < 15_000) return
        lastKey = key
        lastAt = now

        val payment = PaymentParser.parse(title, body)
        if (payment == null) {
            prefs.addHistory("Ignorada: $title | $body")
            return
        }

        val total = prefs.addToToday(payment.amountCents)
        prefs.addHistory("${PaymentParser.formatSoles(payment.amountCents)} — ${payment.sender ?: "sin nombre"}")

        var phrase = PaymentParser.toSpeech(payment, prefs.speakSender)
        if (prefs.speakTotal) {
            phrase += ". Llevas ${PaymentParser.spokenAmount(total)} hoy"
        }
        speaker?.speak(phrase)
    }
}
