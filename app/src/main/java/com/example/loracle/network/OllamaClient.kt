package com.example.loracle.network

import android.os.Handler
import android.os.Looper
import com.example.loracle.managers.ChatSessionManager
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

object OllamaClient {

    // IMPORTANT: must include full endpoint
    private const val OLLAMA_URL = "NO Need Now :)"

    interface StreamCallback {
        fun onToken(token: String)
        fun onComplete()
        fun onError(error: String)
    }

    private val ui = Handler(Looper.getMainLooper())

    fun sendStreamingMessage(message: String, callback: StreamCallback) {
        Thread {
            try {
                // Load entire chat session as context
                //val history = ChatSessionManager.getContext()

                // Build final prompt for LLM
                val finalPrompt = message

                // Build JSON request
                val req = JSONObject().apply {
                    put("model", "gemma:2b")
                    put("prompt", finalPrompt)
                    put("stream", true)
                }

                // Prepare HTTP
                val conn = URL(OLLAMA_URL).openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 10000
                conn.readTimeout = 30000
                conn.doOutput = true

                // Send request body
                conn.outputStream.use { it.write(req.toString().toByteArray()) }

                // Error check
                val responseCode = conn.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    ui.post { callback.onError("HTTP $responseCode: ${conn.responseMessage}") }
                    return@Thread
                }

                // Read streamed response
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    val json = line!!.trim()
                    if (json.isEmpty()) continue

                    val obj = JSONObject(json)

                    if (obj.has("response")) {
                        val token = obj.getString("response")
                        ui.post { callback.onToken(token) }
                    }

                    if (obj.optBoolean("done", false)) {
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
