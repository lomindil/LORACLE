package com.example.loracle.managers

import android.content.Context
import com.example.loracle.ChatMessage
import com.example.loracle.models.SessionPreview
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Simple file-backed ChatSessionManager.
 *
 * - Sessions index stored in: filesDir/sessions/index.json
 * - Each session stored in: filesDir/sessions/session_<id>.json
 *
 * This is intentionally simple and synchronous (file IO). For heavy use, offload to background threads or DB.
 */
object ChatSessionManager {

    // set by init(context)
    private lateinit var appContext: Context
    private val sessionsDirName = "sessions"
    private val indexFileName = "index.json"

    // in-memory cache of previews (keeps things simple)
    private val previews = mutableListOf<SessionPreview>()

    // current open session id
    private var currentSessionId: String? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        ensureSessionsDir()
        loadIndex()
    }

    private fun ensureSessionsDir() {
        val dir = File(appContext.filesDir, sessionsDirName)
        if (!dir.exists()) dir.mkdirs()
    }

    // --- Index (previews) management ---
    private fun indexFile(): File = File(File(appContext.filesDir, sessionsDirName), indexFileName)

    private fun loadIndex() {
        previews.clear()
        val f = indexFile()
        if (!f.exists()) {
            saveIndex()
            return
        }
        try {
            val txt = f.readText()
            if (txt.isBlank()) return
            val arr = JSONArray(txt)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val p = SessionPreview(
                    sessionId = o.getString("sessionId"),
                    title = o.optString("title", "Chat"),
                    lastMessage = o.optString("lastMessage", ""),
                    lastTimestamp = o.optLong("lastTimestamp", System.currentTimeMillis())
                )
                previews.add(p)
            }
        } catch (e: Exception) {
            // ignore and reset
            previews.clear()
            saveIndex()
        }
    }

    private fun saveIndex() {
        val arr = JSONArray()
        previews.forEach { p ->
            val o = JSONObject()
            o.put("sessionId", p.sessionId)
            o.put("title", p.title)
            o.put("lastMessage", p.lastMessage)
            o.put("lastTimestamp", p.lastTimestamp)
            arr.put(o)
        }
        indexFile().writeText(arr.toString())
    }

    // --- Session files ---
    private fun sessionFile(sessionId: String): File {
        return File(File(appContext.filesDir, sessionsDirName), "session_$sessionId.json")
    }

    private fun readMessages(sessionId: String): MutableList<ChatMessage> {
        val f = sessionFile(sessionId)
        if (!f.exists()) return mutableListOf()
        return try {
            val txt = f.readText()
            if (txt.isBlank()) return mutableListOf()
            val arr = JSONArray(txt)
            val list = mutableListOf<ChatMessage>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val msg = ChatMessage(
                    id = o.getString("id"),
                    text = o.getString("text"),
                    isUser = o.getBoolean("isUser"),
                    timestamp = o.optLong("timestamp", System.currentTimeMillis())
                )
                list.add(msg)
            }
            list
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun writeMessages(sessionId: String, msgs: List<ChatMessage>) {
        val f = sessionFile(sessionId)
        val arr = JSONArray()
        msgs.forEach { m ->
            val o = JSONObject()
            o.put("id", m.id)
            o.put("text", m.text)
            o.put("isUser", m.isUser)
            o.put("timestamp", m.timestamp)
            arr.put(o)
        }
        f.writeText(arr.toString())
    }

    // --- Public API expected by the UI ---

    /**
     * Returns list of previews (most recent first)
     */
    fun getAllSessions(): List<SessionPreview> {
        // keep in-memory previews sorted by timestamp desc
        return previews.sortedByDescending { it.lastTimestamp }.toList()
    }

    /**
     * Creates a new session with given title and returns sessionId.
     * Also switches current session to the new one.
     */
    fun createNewSession(title: String = "Chat"): String {
        val id = UUID.randomUUID().toString()
        val preview = SessionPreview(sessionId = id, title = title, lastMessage = "", lastTimestamp = System.currentTimeMillis())
        previews.add(preview)
        saveIndex()
        // create empty session file
        writeMessages(id, emptyList())
        currentSessionId = id
        return id
    }

    /**
     * Switch current working session
     */
    fun switchSession(sessionId: String) {
        // ensure exists
        val found = previews.find { it.sessionId == sessionId }
        if (found == null) {
            // if not present, create a preview entry
            previews.add(SessionPreview(sessionId = sessionId, title = "Chat", lastMessage = "", lastTimestamp = System.currentTimeMillis()))
            saveIndex()
        }
        currentSessionId = sessionId
    }

    /**
     * Returns messages for the session
     */
    fun getMessagesForSession(sessionId: String): List<ChatMessage> {
        return readMessages(sessionId)
    }

    /**
     * Loads (alias) for UI compatibility — returns messages and also sets current session
     */
    fun loadSession(sessionId: String): List<ChatMessage> {
        switchSession(sessionId)
        return getMessagesForSession(sessionId)
    }

    /**
     * Append message to currently selected session. If no session open, create one automatically.
     */
    fun appendMessageToCurrentSession(msg: ChatMessage) {
        val sessionId = currentSessionId ?: createNewSession("Chat")
        val msgs = readMessages(sessionId)
        msgs.add(msg)
        writeMessages(sessionId, msgs)

        // update preview
        val previewIndex = previews.indexOfFirst { it.sessionId == sessionId }
        val lastText = msg.text
        val ts = msg.timestamp
        if (previewIndex >= 0) {
            val old = previews[previewIndex]
            previews[previewIndex] = SessionPreview(sessionId = old.sessionId, title = old.title, lastMessage = lastText, lastTimestamp = ts)
        } else {
            previews.add(SessionPreview(sessionId = sessionId, title = "Chat", lastMessage = lastText, lastTimestamp = ts))
        }
        saveIndex()
    }

    // convenience helpers used by UI
    fun addUserMessage(text: String) {
        val msg = ChatMessage(id = UUID.randomUUID().toString(), text = text, isUser = true, timestamp = System.currentTimeMillis())
        appendMessageToCurrentSession(msg)
    }

    fun addAssistantMessage(text: String) {
        val msg = ChatMessage(id = UUID.randomUUID().toString(), text = text, isUser = false, timestamp = System.currentTimeMillis())
        appendMessageToCurrentSession(msg)
    }

    /**
     * Returns the current session id, or null if none
     */
    fun getCurrentSessionId(): String? = currentSessionId

    /**
     * Helper: close current session (not delete)
     */
    fun closeCurrentSession() {
        currentSessionId = null
    }

    /**
     * Delete session entirely
     */
    fun deleteSession(sessionId: String) {
        // remove file and preview
        sessionFile(sessionId).delete()
        val idx = previews.indexOfFirst { it.sessionId == sessionId }
        if (idx >= 0) {
            previews.removeAt(idx)
            saveIndex()
        }
        if (currentSessionId == sessionId) currentSessionId = null
    }
}
