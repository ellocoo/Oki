package com.oki.app

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.material.materialswitch.MaterialSwitch

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: OkiPrefs
    private var speaker: Speaker? = null

    private lateinit var tvStatus: TextView
    private lateinit var tvBattery: TextView
    private lateinit var tvTotal: TextView
    private lateinit var tvHistory: TextView

    private val askNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = OkiPrefs(this)
        speaker = Speaker(this)

        tvStatus = findViewById(R.id.tvStatus)
        tvBattery = findViewById(R.id.tvBattery)
        tvTotal = findViewById(R.id.tvTotal)
        tvHistory = findViewById(R.id.tvHistory)

        findViewById<Button>(R.id.btnAccess).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        findViewById<Button>(R.id.btnBattery).setOnClickListener { requestBatteryExemption() }
        findViewById<Button>(R.id.btnGuide).setOnClickListener { openUrl("https://dontkillmyapp.com/") }
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
        findViewById<MaterialSwitch>(R.id.swKeepAlive).apply {
            isChecked = prefs.keepAlive
            setOnCheckedChangeListener { _, checked ->
                prefs.keepAlive = checked
                if (checked) KeepAliveService.start(this@MainActivity) else KeepAliveService.stop(this@MainActivity)
            }
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            askNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasListenerAccess()) {
            if (prefs.keepAlive) KeepAliveService.start(this)
            if (!PaymentListenerService.connected) {
                try {
                    NotificationListenerService.requestRebind(
                        ComponentName(this, PaymentListenerService::class.java)
                    )
                } catch (e: Exception) {
                    // Android decidirá cuándo reconectar.
                }
            }
        }
        refresh()
        // Damos un par de segundos a Android para conectar el servicio y volvemos a mirar.
        tvStatus.postDelayed({ if (!isFinishing && !isDestroyed) refresh() }, 2000)
    }

    override fun onDestroy() {
        speaker?.shutdown()
        speaker = null
        super.onDestroy()
    }

    private fun hasListenerAccess(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)

    private fun isBatteryUnrestricted(): Boolean =
        (getSystemService(POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(packageName)

    private fun requestBatteryExemption() {
        val intent = if (isBatteryUnrestricted()) {
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        } else {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            // No hay navegador disponible.
        }
    }

    private fun refresh() {
        tvStatus.setText(
            when {
                !hasListenerAccess() -> R.string.status_off
                PaymentListenerService.connected -> R.string.status_on
                else -> R.string.status_connecting
            }
        )
        tvBattery.setText(if (isBatteryUnrestricted()) R.string.battery_ok else R.string.battery_warn)
        tvTotal.text = getString(R.string.total_today, PaymentParser.formatSoles(prefs.todayTotalCents()))
        val history = prefs.history()
        tvHistory.text = if (history.isEmpty()) getString(R.string.history_empty) else history.joinToString("\n")
    }
}
