package com.oki.app

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Preferencias y datos guardados solo en el teléfono. Nada sale del dispositivo. */
class OkiPrefs(context: Context) {

    private val sp = context.applicationContext.getSharedPreferences("oki", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = sp.getBoolean("enabled", true)
        set(value) = sp.edit().putBoolean("enabled", value).apply()

    var speakSender: Boolean
        get() = sp.getBoolean("speak_sender", true)
        set(value) = sp.edit().putBoolean("speak_sender", value).apply()

    var speakTotal: Boolean
        get() = sp.getBoolean("speak_total", false)
        set(value) = sp.edit().putBoolean("speak_total", value).apply()

    private fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    /** Total del día en céntimos; vuelve a cero cada día. */
    fun todayTotalCents(): Long =
        if (sp.getString("total_day", null) == todayKey()) sp.getLong("total_cents", 0L) else 0L

    fun addToToday(cents: Long): Long {
        val total = todayTotalCents() + cents
        sp.edit().putString("total_day", todayKey()).putLong("total_cents", total).apply()
        return total
    }

    /** Historial corto (lo más nuevo primero) para ver qué detectó Oki. */
    fun history(): List<String> =
        sp.getString("history", "")!!.split("\n").filter { it.isNotBlank() }

    fun addHistory(line: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val entry = "$time  ${line.replace('\n', ' ').take(140)}"
        val updated = (listOf(entry) + history()).take(40)
        sp.edit().putString("history", updated.joinToString("\n")).apply()
    }
}
