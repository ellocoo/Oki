package com.oki.app

import java.util.Locale
import kotlin.math.roundToLong

/** Un pago recibido. El monto va en céntimos para evitar errores de decimales. */
data class Payment(val amountCents: Long, val sender: String?)

object PaymentParser {

    /**
     * Apps de pago cuyas notificaciones se leen. Yape es el más seguro; los demás son
     * los nombres de paquete más probables. Verifícalos en tu teléfono (ver README).
     */
    val MONITORED_PACKAGES = setOf(
        "com.bcp.innovacxion.yapeapp",            // Yape
        "com.bcp.bank.bcp",                       // BCP (Plin)
        "pe.com.interbank.mobilebanking",         // Interbank (Plin)
        "com.bbva.nxt_peru",                      // BBVA Perú (Plin)
        "pe.com.scotiabank.blpm.android.client",  // Scotiabank (Plin)
    )

    private const val NAME = """[\p{L}][\p{L}'’. ]{0,59}?"""
    private const val VERB = """(?:envi[oó]|yape[oó]|plin[eé][oó]|pag[oó]|transfiri[oó]|deposit[oó])"""

    private val AMOUNT = Regex(
        """(?:S/\.?|PEN)\s*(\d{1,3}(?:,\d{3})+(?:\.\d{1,2})?|\d+(?:[.,]\d{1,2})?)""",
        RegexOption.IGNORE_CASE,
    )
    private val INCOMING = Regex(
        """te\s+$VERB|recibiste|has\s+recibido|te\s+lleg[oó]""",
        RegexOption.IGNORE_CASE,
    )
    private val OUTGOING = Regex(
        """yapeaste|plineaste|enviaste|pagaste|transferiste""",
        RegexOption.IGNORE_CASE,
    )
    private val SENDER_BEFORE_VERB = Regex(
        """^\s*(?:[¡!]*\s*(?:yape|plin)\s*[!¡:]*\s*)?($NAME)\s+te\s+$VERB""",
        RegexOption.IGNORE_CASE,
    )
    private val SENDER_AFTER_DE = Regex(
        """(?:recibiste|has\s+recibido).*?\bde\s+($NAME)(?=\s*(?:[,;]|\s+por\s|\s+a\s|\s+en\s|\s+el\s|$))""",
        RegexOption.IGNORE_CASE,
    )

    /** Devuelve el pago si la notificación es un pago recibido; null si no lo es. */
    fun parse(title: String, body: String): Payment? {
        val full = "$title. $body"
        if (OUTGOING.containsMatchIn(full)) return null
        if (!INCOMING.containsMatchIn(full)) return null
        val raw = AMOUNT.find(full)?.groupValues?.get(1) ?: return null
        val cents = toCents(raw) ?: return null
        if (cents <= 0) return null
        return Payment(cents, findSender(title, body))
    }

    private fun toCents(raw: String): Long? {
        val normalized = when {
            raw.contains(',') && raw.contains('.') -> raw.replace(",", "")
            raw.contains(',') && Regex(""",\d{1,2}$""").containsMatchIn(raw) -> raw.replace(',', '.')
            else -> raw.replace(",", "")
        }
        return normalized.toDoubleOrNull()?.let { (it * 100).roundToLong() }
    }

    private fun findSender(title: String, body: String): String? {
        for (text in listOf(body, title)) {
            val name = SENDER_BEFORE_VERB.find(text)?.groupValues?.get(1)?.let(::clean)
            if (!name.isNullOrEmpty()) return name
        }
        val name = SENDER_AFTER_DE.find("$title. $body")?.groupValues?.get(1)?.let(::clean)
        return if (name.isNullOrEmpty()) null else name
    }

    private fun clean(s: String): String =
        s.replace(Regex("\\s+"), " ").trim().trimEnd('.', ' ')

    /** "S/ 10.50" */
    fun formatSoles(cents: Long): String =
        "S/ " + String.format(Locale.US, "%.2f", cents / 100.0)

    /** Monto como se dice en voz alta: "1 sol", "5 soles", "10 soles con 50 céntimos". */
    fun spokenAmount(cents: Long): String {
        val soles = cents / 100
        val cts = (cents % 100).toInt()
        val solesText = if (soles == 1L) "1 sol" else "$soles soles"
        return when {
            cts == 0 -> solesText
            soles == 0L -> "$cts céntimos"
            else -> "$solesText con $cts céntimos"
        }
    }

    /** Frase que Oki dice: "Has recibido 1 sol de Pepito". */
    fun toSpeech(payment: Payment, includeSender: Boolean): String {
        val base = "Has recibido ${spokenAmount(payment.amountCents)}"
        return if (includeSender && !payment.sender.isNullOrBlank()) "$base de ${payment.sender}" else base
    }
}
