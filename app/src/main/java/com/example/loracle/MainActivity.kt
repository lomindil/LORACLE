package com.example.loracle

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.loracle.managers.ChatSessionManager
import com.example.loracle.managers.SpeechRecognizerManager
import com.example.loracle.managers.TTSManager
import com.example.loracle.models.SessionPreview
import com.example.loracle.network.OllamaClient
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle

    private lateinit var edtInput: EditText
    private lateinit var btnMic: ImageButton
    private lateinit var btnSend: ImageButton
    private lateinit var txtThinking: TextView

    private lateinit var rvMessages: RecyclerView
    private lateinit var chatAdapter: ChatAdapter

    private lateinit var rvSessions: RecyclerView
    private lateinit var sessionAdapter: SessionAdapter

    private lateinit var speech: SpeechRecognizerManager
    private lateinit var tts: TTSManager

    private val REQUEST_AUDIO = 1

    private val messages = mutableListOf<ChatMessage>()
    private var currentResponse = StringBuilder()

    private val mainHandler = Handler(Looper.getMainLooper())

    private var currentSessionId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // IMPORTANT: Ensure ChatSessionManager is initialized
        ChatSessionManager.init(this)

        setupDrawer()
        setupViews()
        setupChatList()
        setupSessionList()

        askAudioPermission()
    }

    private fun setupDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout)
        toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            findViewById(R.id.toolbar),   // IMPORTANT
            R.string.drawer_open,
            R.string.drawer_close
        )

        drawerLayout.addDrawerListener(toggle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toggle.syncState()
    }

    private fun setupViews() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        edtInput = findViewById(R.id.edtUserInput)
        btnMic = findViewById(R.id.btnMic)
        btnSend = findViewById(R.id.btnSend)
        txtThinking = findViewById(R.id.txtThinking)
    }


    private fun setupChatList() {
        rvMessages = findViewById(R.id.rvMessages)
        chatAdapter = ChatAdapter()
        rvMessages.adapter = chatAdapter
        rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
    }

    private fun setupSessionList() {
        rvSessions = findViewById(R.id.rvSessions)
        sessionAdapter = SessionAdapter { sessionPreview: SessionPreview ->
            // Use the session id to load the session
            loadSession(sessionPreview.sessionId)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        rvSessions.adapter = sessionAdapter
        rvSessions.layoutManager = LinearLayoutManager(this)
        refreshSessionList()
    }

    private fun refreshSessionList() {
        // ChatSessionManager.getAllSessions() should return List<SessionPreview>
        val sessions = ChatSessionManager.getAllSessions()
        sessionAdapter.submitList(sessions)
    }

    private fun askAudioPermission() {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), REQUEST_AUDIO)
        } else {
            initAfterPermission()
        }
    }

    private fun initAfterPermission() {
        tts = TTSManager(this)

        speech = SpeechRecognizerManager(this, object : SpeechRecognizerManager.Callback {
            override fun onResult(text: String) {
                edtInput.setText(text)
                sendUserMessage(text)
            }

            override fun onError(error: String) {
                addSystemMessage("Speech error: $error")
            }
        })

        btnMic.setOnClickListener { speech.startListening() }

        btnSend.setOnClickListener {
            val text = edtInput.text.toString().trim()
            if (text.isNotEmpty()) {
                edtInput.setText("")
                sendUserMessage(text)
            }
        }

        // Start a new session on fresh launch
        startNewSession()
    }

    private fun startNewSession() {
        currentSessionId = ChatSessionManager.createNewSession()
        messages.clear()
        chatAdapter.submitList(ArrayList(messages))
        refreshSessionList()
    }

    private fun loadSession(id: String) {
        currentSessionId = id
        messages.clear()
        messages.addAll(ChatSessionManager.loadSession(id))
        chatAdapter.submitList(ArrayList(messages))
        scrollToBottom()
    }

    // ----------------------------
    // Message operations
    // ----------------------------

    private fun sendUserMessage(text: String) {
        val msg = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = text,
            isUser = true,
            timestamp = System.currentTimeMillis()
        )

        messages.add(msg)
        chatAdapter.submitList(ArrayList(messages))
        scrollToBottom()

        ChatSessionManager.addUserMessage(text)

        showThinking(true)
        sendToOllama(text)
    }

    private fun addBotChunk(chunk: String) {
        currentResponse.append(chunk)

        if (messages.isNotEmpty() && !messages.last().isUser) {
            val last = messages.last()
            messages[messages.size - 1] = last.copy(text = currentResponse.toString())
        } else {
            messages.add(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = currentResponse.toString(),
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        chatAdapter.submitList(ArrayList(messages))
        scrollToBottom()
    }

    private fun finalizeBotResponse() {
        showThinking(false)

        val finalText = currentResponse.toString()
        ChatSessionManager.addAssistantMessage(finalText)

        val last = messages.lastOrNull { !it.isUser }
        last?.let { tts.speak(it.text) }

        currentResponse = StringBuilder()
        refreshSessionList()
    }

    private fun addSystemMessage(text: String) {
        val msg = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = text,
            isUser = false,
            timestamp = System.currentTimeMillis()
        )
        messages.add(msg)
        chatAdapter.submitList(ArrayList(messages))
        scrollToBottom()
    }

    private fun scrollToBottom() {
        mainHandler.post {
            rvMessages.scrollToPosition(chatAdapter.itemCount - 1)
        }
    }

    private fun showThinking(show: Boolean) {
        txtThinking.visibility = if (show) View.VISIBLE else View.GONE
    }

    // ----------------------------
    // OLLAMA STREAMING
    // ----------------------------

    private fun sendToOllama(text: String) {
        OllamaClient.sendStreamingMessage(text, object : OllamaClient.StreamCallback {

            override fun onToken(token: String) {
                mainHandler.post { addBotChunk(token) }
            }

            override fun onComplete() {
                mainHandler.post { finalizeBotResponse() }
            }

            override fun onError(error: String) {
                mainHandler.post {
                    showThinking(false)
                    addSystemMessage("Error: $error")
                }
            }
        })
    }

    // ----------------------------
    // Permissions
    // ----------------------------

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_AUDIO &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            initAfterPermission()
        } else {
            Toast.makeText(this, "Audio permission required!", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    // ----------------------------
    // Drawer toggle
    // ----------------------------

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (toggle.onOptionsItemSelected(item)) true
        else super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        try { tts.shutdown() } catch (_: Exception) {}
    }
}
