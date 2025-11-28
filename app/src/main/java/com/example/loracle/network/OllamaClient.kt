package com.example.loracle.network

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

object OllamaClient {

    private const val OLLAMA_URL = "http://127.0.0.1/api/generate"

    interface StreamCallback {
        fun onToken(token: String)
        fun onComplete()
        fun onError(error: String)
    }

    private val ui = Handler(Looper.getMainLooper())

    fun sendStreamingMessage(message: String, callback: StreamCallback) {
        Thread {
            try {
                val req = JSONObject().apply {
                    put("model", "gemma:2b")
                    put("prompt", message)  // Use "prompt" instead of "messages"
                    put("stream", true)
                    // Remove the "messages" array - Ollama uses simple prompt format
                }

                val conn = URL(OLLAMA_URL).openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 10000
                conn.readTimeout = 30000
                conn.doOutput = true

                conn.outputStream.use { it.write(req.toString().toByteArray()) }

                // Check response code first
                val responseCode = conn.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    ui.post { callback.onError("HTTP error: $responseCode - ${conn.responseMessage}") }
                    return@Thread
                }

                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    val json = line!!.trim()
                    if (json.isEmpty()) continue

                    val chunk = JSONObject(json)

                    if (chunk.has("response")) {
                        ui.post { callback.onToken(chunk.getString("response")) }
                    }

                    if (chunk.optBoolean("done", false)) {
                        ui.post { callback.onComplete() }
                        break
                    }
                }

            } catch (e: Exception) {
                ui.post { callback.onError("Connection failed: ${e.message}") }
            }
        }.start()
    }
}

