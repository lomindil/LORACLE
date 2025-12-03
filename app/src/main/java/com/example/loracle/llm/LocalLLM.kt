package com.example.loracle.llm

import android.llama.cpp.LLamaAndroid
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.catch

object LocalLLM {

    private val TAG = "LocalLLM"

    // We increase generation length so prompts larger than 64 tokens still work
    private const val MAX_TOKENS = 256

    /**
     * Loads the GGUF model from disk.
     */
    suspend fun loadModel(path: String): Boolean {
        return try {
            Log.i(TAG, "Loading model: $path")
            LLamaAndroid.instance().load(path)
            Log.i(TAG, "Model loaded successfully.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model", e)
            false
        }
    }

    /**
     * Sends a single-turn prompt to the LLM with streaming output.
     */
    suspend fun sendMessage(
        userMessage: String,
        onToken: (String) -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        Log.d(TAG, "sendMessage() called with: $userMessage")

        try {
            val prompt = ChatTemplate.buildPrompt(userMessage)

            withContext(Dispatchers.IO) {

                LLamaAndroid.instance()
                    .send(prompt, formatChat = false)
                    .onEach { token ->
                        Log.v(TAG, "TOKEN: $token")
                        onToken(token)
                    }
                    .onCompletion {
                        Log.d(TAG, "Generation done.")
                        onDone()
                    }
                    .catch { e ->
                        Log.e(TAG, "Error during generation", e)
                        onError(e.message ?: "Unknown error")
                    }
                    .collect()
            }

        } catch (e: Exception) {
            Log.e(TAG, "sendMessage failed", e)
            onError(e.message ?: "Unknown error")
        }
    }
}
