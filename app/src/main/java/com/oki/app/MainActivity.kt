package com.oki.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.google.android.material.materialswitch.MaterialSwitch

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: OkiPrefs
    private var speaker: Speaker? = null

    private lateinit var tvStatus: TextView
    private lateinit var tvTotal: TextView
    private lateinit var tvHistory: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = OkiPrefs(this)
        speaker = Speaker(this)

        tvStatus = findViewById(R.id.tvStatus)
        tvTotal = findViewById(R.id.tvTotal)
        tvHistory = findViewById(R.id.tvHistory)

        findViewById<Button>(R.id.btnAccess).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        findViewById<Button>(R.id.btnBattery).setOnClickListener {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
        findViewById<Button>(R.id.btnTest).setOnClickListener {
            speaker?.speak(getString(R.string.test_phrase))
        }

        findViewById<MaterialSwitch>(R.id.swEnabled).apply {
            isChecked = prefs.enabled
            setOnCheckedChangeListener { _, checked -> prefs.enabled = checked }
        }
        findViewById<MaterialSwitch>(R.id.swSender).apply {
            isChecked = prefs.speakSender
            setOnCheckedChangeListener { _, checked -> prefs.speakSender = checked }
        }
        findViewById<MaterialSwitch>(R.id.swTotal).apply {
            isChecked = prefs.speakTotal
            setOnCheckedChangeListener { _, checked -> prefs.speakTotal = checked }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onDestroy() {
        speaker?.shutdown()
        speaker = null
        super.onDestroy()
    }

    private fun hasListenerAccess(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)

    private fun refresh() {
        tvStatus.setText(if (hasListenerAccess()) R.string.status_on else R.string.status_off)
        tvTotal.text = getString(R.string.total_today, PaymentParser.formatSoles(prefs.todayTotalCents()))
        val history = prefs.history()
        tvHistory.text = if (history.isEmpty()) getString(R.string.history_empty) else history.joinToString("\n")
    }
}
