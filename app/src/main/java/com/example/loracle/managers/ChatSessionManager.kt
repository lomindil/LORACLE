package com.example.loracle.managers

import android.content.Context
import java.io.File

object ChatSessionManager {

    private var session: File? = null

    fun start(context: Context) {
        val dir = File(context.filesDir, "sessions")
        if (!dir.exists()) dir.mkdir()

        session = File(dir, "session_${System.currentTimeMillis()}.txt")
        session!!.createNewFile()
    }

    fun addUser(msg: String) {
        session?.appendText("USER: $msg\n")
    }

    fun addBot(msg: String) {
        session?.appendText("ASSISTANT: $msg\n")
    }

    fun getContext(): String {
        return session?.readText() ?: ""
    }

    fun end() {
        session?.delete()
        session = null
    }
}
