package com.oki.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Al encender el teléfono, vuelve a poner Oki en marcha sin que tengas que abrirlo. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && OkiPrefs(context).keepAlive) {
            KeepAliveService.start(context)
        }
    }
}
