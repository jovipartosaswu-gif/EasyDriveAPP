package com.example.easydrive

import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChatActivity : AppCompatActivity() {

    private lateinit var messagesContainer: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageView

    private lateinit var chatId: String
    private lateinit var currentUserId: String
    private lateinit var currentUserName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        chatId = intent.getStringExtra("chatId") ?: ""
        val chatName = intent.getStringExtra("chatName") ?: "Chat"
        val carName = intent.getStringExtra("carName") ?: ""

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Get current user name from prefs
        val prefs = getSharedPreferences("EasyDrivePrefs", MODE_PRIVATE)
        val firstName = prefs.getString("firstName", "") ?: ""
        val lastName = prefs.getString("lastName", "") ?: ""
        currentUserName = "$firstName $lastName".trim().ifEmpty { "User" }

        messagesContainer = findViewById(R.id.messagesContainer)
        scrollView = findViewById(R.id.scrollView)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)

        findViewById<TextView>(R.id.tvChatName).text = chatName
        findViewById<TextView>(R.id.tvChatCar).text = carName
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        btnSend.setOnClickListener { sendMessage() }

        loadMessages()
        listenForNewMessages()
    }

    private fun loadMessages() {
        ChatManager.getMessages(chatId,
            onSuccess = { messages ->
                messagesContainer.removeAllViews()
                messages.forEach { addMessageBubble(it) }
                scrollToBottom()
            },
            onFailure = {}
        )
    }

    private fun listenForNewMessages() {
        FirebaseFirestore.getInstance()
            .collection("chats").document(chatId)
            .collection("messages")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val messages = snapshot.documents.map { doc ->
                        ChatMessage(
                            id = doc.id,
                            senderId = doc.getString("senderId") ?: "",
                            senderName = doc.getString("senderName") ?: "",
                            message = doc.getString("message") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L
                        )
                    }.sortedBy { it.timestamp }
                    messagesContainer.removeAllViews()
                    messages.forEach { addMessageBubble(it) }
                    scrollToBottom()
                }
            }
    }

    private fun addMessageBubble(msg: ChatMessage) {
        val isMine = msg.senderId == currentUserId

        val bubble = TextView(this).apply {
            text = msg.message
            textSize = 14f
            setTextColor(if (isMine) 0xFFFFFFFF.toInt() else 0xFF212121.toInt())
            setPadding(32, 20, 32, 20)
            background = if (isMine)
                resources.getDrawable(R.drawable.bg_button_yellow, null)
            else
                resources.getDrawable(R.drawable.bg_button_white, null)
            maxWidth = (resources.displayMetrics.widthPixels * 0.7).toInt()
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = if (isMine) Gravity.END else Gravity.START
            setMargins(0, 8, 0, 8)
        }

        messagesContainer.addView(bubble, params)
    }

    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty()) return

        etMessage.setText("")

        ChatManager.sendMessage(chatId, currentUserId, currentUserName, text,
            onSuccess = {},
            onFailure = {
                android.widget.Toast.makeText(this, "Failed to send message", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun scrollToBottom() {
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }
}
