package com.example.loracle.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import ai.picovoice.porcupine.PorcupineException
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import com.example.loracle.R

class WakeWordService : Service() {

    private val TAG = "WakeWordService"
    private var porcupineManager: PorcupineManager? = null
    private var isListening = false

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "WakeWordServiceChannel"
    }

    // ---------------------------------------------------------
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        initializePorcupine()
    }

    // ---------------------------------------------------------
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Wake Word Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    // ---------------------------------------------------------
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Loracle")
            .setContentText("Listening for wake word...")
            .setSmallIcon(android.R.drawable.ic_media_play) // Use built-in Android icon
            .build()
    }

    // ---------------------------------------------------------
    private fun initializePorcupine() {
        // Check audio permission
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Audio permission not granted")
            return
        }

        try {
            val accessKey = "FE4XtYxkL88dIgRAtb1ZHMFaFiAAIvg2LOUYBZ07mJC30IgFRQynaA=="
            val keywordPath = "keywords/Lo-oracle_en_android_v3_0_0.ppn"

            Log.d(TAG, "Initializing Porcupine with access key and keyword: $keywordPath")

            porcupineManager = PorcupineManager.Builder()
                .setAccessKey(accessKey)
                .setKeywordPath(keywordPath)
                .setSensitivity(0.8f) // Increased sensitivity
                .build(applicationContext, object : PorcupineManagerCallback {
                    override fun invoke(keywordIndex: Int) {
                        Log.d(TAG, "WAKE WORD DETECTED - Triggering broadcast")
                        broadcastWakeWordDetected()
                    }
                })

            startListening()
            Log.d(TAG, "Porcupine initialized and listening for wake word")

        } catch (e: PorcupineException) {
            Log.e(TAG, "Error initializing Porcupine: ${e.message}")
            e.printStackTrace()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun broadcastWakeWordDetected() {
        try {
            Log.d(TAG, "Broadcasting wake word detection to MainActivity")
            val intent = Intent("LORACLE_WAKEWORD_HIT")
            intent.setPackage(packageName)
            sendBroadcast(intent)
            Log.d(TAG, "Wake word broadcast sent successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending wake word broadcast: ${e.message}")
        }
    }

    // ---------------------------------------------------------
    private fun startListening() {
        if (!isListening) {
            try {
                porcupineManager?.start()
                isListening = true
                Log.d(TAG, "Successfully started listening for wake word")

                // Test if audio is being processed
                Log.d(TAG, "Audio system should now be processing microphone input")

            } catch (e: PorcupineException) {
                Log.e(TAG, "Error starting Porcupine: ${e.message}")
                e.printStackTrace()
            }
        } else {
            Log.d(TAG, "Already listening for wake word")
        }
    }

    // ---------------------------------------------------------
    private fun stopListening() {
        if (isListening) {
            try {
                porcupineManager?.stop()
                isListening = false
                Log.d(TAG, "Stopped listening for wake word")
            } catch (e: PorcupineException) {
                Log.e(TAG, "Error stopping Porcupine: ${e.message}")
            }
        }
    }

    // ---------------------------------------------------------
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    // ---------------------------------------------------------
    override fun onBind(intent: Intent?): IBinder {
        return Binder()
    }

    // ---------------------------------------------------------
    override fun onDestroy() {
        super.onDestroy()
        stopListening()
        porcupineManager?.delete()
        Log.d(TAG, "WakeWordService destroyed")
    }
}