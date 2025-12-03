package com.example.loracle.llm

import android.util.Log

object ChatTemplate {

    private val TAG = "ChatTemplate"

    /**
     * Minimal valid Gemma-3 chat prompt.
     * Single-turn only — works with your llama.cpp JNI binding.
     */
    fun buildPrompt(userInput: String): String {
        val system = """
            You are Loracle, a helpful and friendly AI assistant running entirely on the user's device.
            Be concise, polite, and safe. Refuse dangerous or unethical requests.
        """.trimIndent()

        val prompt = """
            <bos><start_of_turn>user
            $system

            $userInput
            <end_of_turn>
            <start_of_turn>model
        """.trimIndent()

        Log.d(TAG, "Built Gemma prompt (chars=${prompt.length})")
        Log.d(TAG, "Prompt preview:\n$prompt")

        return prompt
    }
}
