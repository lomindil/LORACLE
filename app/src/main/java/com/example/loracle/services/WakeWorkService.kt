package com.example.loracle.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineManager
import android.util.Log

class WakeWordService : Service() {

    private var porcupineManager: PorcupineManager? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
        startWakeWordEngine()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "wakeword_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Wake Word Listener",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Loracle is listening…")
            .setContentText("Wake word detection is active")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()

        startForeground(1, notification)
    }

    private fun startWakeWordEngine() {
        try {
            porcupineManager = PorcupineManager(
                keywordPaths = listOf("keyword.ppn"),    // your asset keyword
                sensitivities = floatArrayOf(0.6f)
            ) {
                // Callback: wake word detected
                Log.d("WakeWordService", "Wake word detected!")
                // Start your app or activity here
            }

            porcupineManager!!.start()

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY   // ensures service restarts if killed
    }

    override fun onDestroy() {
        porcupineManager?.stop()
        porcupineManager?.delete()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
    val broadcastIntent = Intent("RESTART_WAKEWORD_SERVICE")
    sendBroadcast(broadcastIntent)
    super.onTaskRemoved(rootIntent)
}
}

