package com.example.loracle.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AutoRestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "RESTART_WAKEWORD_SERVICE") {
            context?.startForegroundService(Intent(context, WakeWordService::class.java))
        }
    }
}

