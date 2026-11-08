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
        prefs.addHistory("Servicio detenido por Android")
        speaker?.shutdown()
        speaker = null
        super.onDestroy()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        prefs.addHistory("Servicio conectado")
    }

    override fun onListenerDisconnected() {
        prefs.addHistory("Servicio desconectado")
        // Si Android desconecta el servicio, pedimos volver a conectarlo.
        requestRebind(ComponentName(this, PaymentListenerService::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in PaymentParser.MONITORED_PACKAGES) return

        val notification = sbn.notification
        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val body = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        val isSummary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0
        val app = sbn.packageName.substringAfterLast('.')
        val tag = if (isSummary) "resumen $app" else app

        // Todo lo que llega de una app vigilada queda anotado, para poder ver qué pasó.
        if (!prefs.enabled) {
            prefs.addHistory("Oki apagado [$tag]: $title | $body")
            return
        }
        if (title.isBlank() && body.isBlank()) {
            prefs.addHistory("Vacía [$tag]")
            return
        }

        // Algunas apps actualizan la misma notificación varias veces: evitamos repetir la voz.
        val key = "${sbn.packageName}|$title|$body"
        val now = System.currentTimeMillis()
        if (key == lastKey && now - lastAt < 5_000) {
            prefs.addHistory("Repetida [$tag]: $title | $body")
            return
        }
        lastKey = key
        lastAt = now

        val payment = PaymentParser.parse(title, body)
        if (payment == null) {
            prefs.addHistory("Ignorada [$tag]: $title | $body")
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
