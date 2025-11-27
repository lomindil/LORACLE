package com.example.loracle


import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import com.example.loracle.R
import com.example.loracle.managers.SpeechRecognizerManager
import com.example.loracle.managers.TTSManager
import com.example.loracle.network.OllamaClient


class MainActivity : AppCompatActivity() {

    private lateinit var edtInput: EditText
    private lateinit var txtOutput: TextView
    private lateinit var btnMic: ImageButton
    private lateinit var btnSend: ImageButton

    private lateinit var speech: SpeechRecognizerManager
    private lateinit var tts: TTSManager

    private val currentResponse = StringBuilder()


    private val REQUEST_AUDIO = 1

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    // 1. Ask for permission BEFORE anything else
    if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
        != android.content.pm.PackageManager.PERMISSION_GRANTED) {

        requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), REQUEST_AUDIO)
        return
    }

    // 2. Initialize UI
    edtInput = findViewById(R.id.edtUserInput)
    txtOutput = findViewById(R.id.txtResponse)
    btnMic = findViewById(R.id.btnMic)
    btnSend = findViewById(R.id.btnSend)

    // 3. Initialize TTS
    tts = TTSManager(this)

    // 4. Initialize SpeechRecognizer AFTER permission
    speech = SpeechRecognizerManager(this, object : SpeechRecognizerManager.Callback {
        override fun onResult(text: String) {
            edtInput.setText(text)
            sendToOllama(text)
        }

        override fun onError(error: String) {
            txtOutput.text = error
        }
    })

    // 5. Button listeners
    btnMic.setOnClickListener { speech.startListening() }
    btnSend.setOnClickListener { sendToOllama(edtInput.text.toString()) }
}


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

    override fun onDestroy() {
        super.onDestroy()
        tts.shutdown()
    }
    
    override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)

    if (requestCode == REQUEST_AUDIO &&
        grantResults.isNotEmpty() &&
        grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
        
        recreate() // Restart activity so SpeechRecognizer can initialize
    } else {
        Toast.makeText(this, "Audio permission required!", Toast.LENGTH_LONG).show()
        finish()
    }
}

}
