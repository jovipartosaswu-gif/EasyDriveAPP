package com.example.easydrive

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MessagesActivity : AppCompatActivity() {

    private lateinit var chatsContainer: LinearLayout
    private lateinit var tvEmptyState: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        chatsContainer = findViewById(R.id.chatsContainer)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        progressBar = findViewById(R.id.progressBar)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }
    }

    private fun loadChats() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        progressBar.visibility = View.VISIBLE
        chatsContainer.removeAllViews()

        ChatManager.getChatsForUser(userId,
            onSuccess = { chats ->
                progressBar.visibility = View.GONE
                // Deduplicate by chatId just in case Firestore has duplicates
                val unique = chats.distinctBy { it.chatId }
                android.util.Log.d("MessagesActivity", "Total: ${chats.size}, Unique: ${unique.size}")
                if (unique.isEmpty()) {
                    tvEmptyState.visibility = View.VISIBLE
                } else {
                    tvEmptyState.visibility = View.GONE
                    unique.forEach { addChatView(it, userId) }
                }
            },
            onFailure = {
                progressBar.visibility = View.GONE
                tvEmptyState.text = "Failed to load messages."
                tvEmptyState.visibility = View.VISIBLE
            }
        )
    }

    private fun addChatView(chat: ChatThread, currentUserId: String) {
        val view = layoutInflater.inflate(R.layout.item_chat_thread, chatsContainer, false)

        // Show the other person's name
        val isRenter = chat.renterId == currentUserId
        val displayName = if (isRenter) chat.agencyName else chat.renterName

        view.findViewById<TextView>(R.id.tvName).text = displayName
        view.findViewById<TextView>(R.id.tvCarName).text = "Re: ${chat.carName}"
        view.findViewById<TextView>(R.id.tvLastMessage).text = chat.lastMessage

        view.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("chatId", chat.chatId)
                putExtra("chatName", displayName)
                putExtra("carName", chat.carName)
            }
            startActivity(intent)
        }

        chatsContainer.addView(view)
    }

    override fun onResume() {
        super.onResume()
        loadChats()
    }
}
