package com.oki.app

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** Lee las notificaciones de las apps de pago y anuncia en voz alta los pagos recibidos. */
class PaymentListenerService : NotificationListenerService() {

    companion object {
        /** Es true mientras Android mantiene conectado este servicio. */
        @Volatile
        var connected = false
    }

    private lateinit var prefs: OkiPrefs
    private var speaker: Speaker? = null

    override fun onCreate() {
        super.onCreate()
        prefs = OkiPrefs(this)
        speaker = Speaker(this)
    }

    override fun onDestroy() {
        connected = false
        prefs.addHistory("Servicio detenido por Android")
        speaker?.shutdown()
        speaker = null
        super.onDestroy()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        connected = true
        prefs.addHistory("Servicio conectado")
        if (prefs.keepAlive) KeepAliveService.start(this)
        // Si Android tuvo a Oki dormido, recupera los pagos que llegaron mientras tanto.
        catchUp()
    }

    override fun onListenerDisconnected() {
        connected = false
        prefs.addHistory("Servicio desconectado")
        // Si Android desconecta el servicio, pedimos volver a conectarlo.
        requestRebind(ComponentName(this, PaymentListenerService::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in PaymentParser.MONITORED_PACKAGES) return
        handle(sbn, late = false)
    }

    /** Revisa las notificaciones que siguen en la barra y anuncia las que no se habían leído. */
    private fun catchUp() {
        val active = try {
            activeNotifications
        } catch (e: Exception) {
            null
        } ?: return
        val limit = System.currentTimeMillis() - 15 * 60_000L
        active
            .filter { it.packageName in PaymentParser.MONITORED_PACKAGES && it.postTime >= limit }
            .sortedBy { it.postTime }
            .forEach { handle(it, late = true) }
    }

    private fun handle(sbn: StatusBarNotification, late: Boolean) {
        if (!prefs.enabled) return

        val notification = sbn.notification
        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val body = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        val isSummary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0
        val app = sbn.packageName.substringAfterLast('.')
        val tag = if (isSummary) "resumen $app" else app

        if (title.isBlank() && body.isBlank()) {
            if (!late) prefs.addHistory("Vacía [$tag]")
            return
        }

        // Cada notificación real tiene una identidad única (clave + hora de publicación).
        // Así, dos pagos con el mismo monto y la misma persona cuentan como dos pagos,
        // y una misma notificación nunca se anuncia dos veces.
        val id = "${sbn.key}#${sbn.postTime}"
        if (prefs.isProcessed(id)) return
        prefs.markProcessed(id)

        // Los resúmenes de grupo repiten el texto de sus pagos: se anotan pero no se leen.
        if (isSummary) {
            prefs.addHistory("Resumen omitido [$tag]: $title | $body")
            return
        }

        val payment = PaymentParser.parse(title, body)
        if (payment == null) {
            prefs.addHistory("Ignorada [$tag]: $title | $body")
            return
        }

        val total = prefs.addToToday(payment.amountCents)
        val recovered = if (late) " (recuperado)" else ""
        prefs.addHistory(
            "${PaymentParser.formatSoles(payment.amountCents)} — ${payment.sender ?: "sin nombre"}$recovered"
        )

        var phrase = PaymentParser.toSpeech(payment, prefs.speakSender)
        if (prefs.speakTotal) {
            phrase += ". Llevas ${PaymentParser.spokenAmount(total)} hoy"
        }
        speaker?.speak(phrase)
    }
}
