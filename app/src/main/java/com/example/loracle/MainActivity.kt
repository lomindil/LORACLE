package com.example.loracle

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import com.example.loracle.managers.SpeechRecognizerManager
import com.example.loracle.managers.TTSManager
import com.example.loracle.network.OllamaClient
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import com.example.loracle.services.WakeWordService
import android.os.Build

class MainActivity : AppCompatActivity() {

    private lateinit var edtInput: EditText
    private lateinit var txtOutput: TextView
    private lateinit var btnMic: ImageButton
    private lateinit var btnSend: ImageButton

    private lateinit var speech: SpeechRecognizerManager
    private lateinit var tts: TTSManager

    private val currentResponse = StringBuilder()

    private val REQUEST_AUDIO = 1

    // ---------------------------------------------------------
    // Wake word receiver
    // ---------------------------------------------------------
    private val wakeWordReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "LORACLE_WAKEWORD_HIT") {
                speech.startListening()
            }
        }
    }

    // ---------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Start wake-word listener service
        val serviceIntent = Intent(this, WakeWordService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // Audio permission
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {

            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), REQUEST_AUDIO)
        } else {
            initializeApp()
        }
    }

    // ---------------------------------------------------------
    private fun initializeApp() {

        edtInput = findViewById(R.id.edtUserInput)
        txtOutput = findViewById(R.id.txtResponse)
        btnMic = findViewById(R.id.btnMic)
        btnSend = findViewById(R.id.btnSend)

        tts = TTSManager(this)

        speech = SpeechRecognizerManager(this, object : SpeechRecognizerManager.Callback {
            override fun onResult(text: String) {
                edtInput.setText(text)
                sendToOllama(text)
            }

            override fun onError(error: String) {
                txtOutput.text = error
            }
        })

        btnMic.setOnClickListener { speech.startListening() }
        btnSend.setOnClickListener { sendToOllama(edtInput.text.toString()) }
    }

    // ---------------------------------------------------------
    private fun sendToOllama(text: String) {
        txtOutput.text = "Thinking…"
        currentResponse.clear()

        OllamaClient.sendStreamingMessage(text, object : OllamaClient.StreamCallback {
            override fun onToken(token: String) {
                currentResponse.append(token)
                txtOutput.text = currentResponse.toString()
            }

            override fun onComplete() {
                tts.speak(currentResponse.toString())
            }

            override fun onError(error: String) {
                txtOutput.text = "Error: $error"
            }
        })
    }

    // ---------------------------------------------------------
    override fun onResume() {
        super.onResume()

        // FIX: Add the export flag for Android 13+
        val filter = IntentFilter("LORACLE_WAKEWORD_HIT")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(wakeWordReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(wakeWordReceiver, filter)
        }
    }

    override fun onPause() {
        unregisterReceiver(wakeWordReceiver)
        super.onPause()
    }

    override fun onDestroy() {
        tts.shutdown()
        super.onDestroy()
    }

    // ---------------------------------------------------------
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_AUDIO &&
            grantResults.isNotEmpty() &&
            grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {

            initializeApp()

        } else {
            Toast.makeText(this, "Audio permission required!", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}