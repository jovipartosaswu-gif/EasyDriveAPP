package com.example.easydrive

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

object ChatManager {
    private const val TAG = "ChatManager"
    private val firestore = FirebaseFirestore.getInstance()

    // Chat ID is always a combination of both user IDs sorted alphabetically
    fun getChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "${userId1}_${userId2}" else "${userId2}_${userId1}"
    }

    fun createOrGetChat(
        renterId: String,
        renterName: String,
        renterEmail: String,
        agencyId: String,
        agencyName: String,
        carName: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val chatId = getChatId(renterId, agencyId)
        val chatData = hashMapOf(
            "chatId" to chatId,
            "renterId" to renterId,
            "renterName" to renterName,
            "renterEmail" to renterEmail,
            "agencyId" to agencyId,
            "agencyName" to agencyName,
            "carName" to carName,
            "lastMessage" to "Booking confirmed for $carName",
            "lastMessageTime" to System.currentTimeMillis(),
            "timestamp" to System.currentTimeMillis()
        )
        // Use set with merge=false — always writes to the same document ID
        firestore.collection("chats").document(chatId)
            .set(chatData)
            .addOnSuccessListener { onSuccess(chatId) }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun sendMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        message: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val msgData = hashMapOf(
            "senderId" to senderId,
            "senderName" to senderName,
            "message" to message,
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("chats").document(chatId)
            .collection("messages")
            .add(msgData)
            .addOnSuccessListener {
                // Update last message in chat
                firestore.collection("chats").document(chatId)
                    .update("lastMessage", message, "lastMessageTime", System.currentTimeMillis())
                onSuccess()
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun getMessages(
        chatId: String,
        onSuccess: (List<ChatMessage>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.collection("chats").document(chatId)
            .collection("messages")
            .get()
            .addOnSuccessListener { docs ->
                val messages = docs.map { doc ->
                    ChatMessage(
                        id = doc.id,
                        senderId = doc.getString("senderId") ?: "",
                        senderName = doc.getString("senderName") ?: "",
                        message = doc.getString("message") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                }.sortedBy { it.timestamp }
                onSuccess(messages)
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun getChatsForUser(
        userId: String,
        onSuccess: (List<ChatThread>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // Get chats where user is renter
        firestore.collection("chats")
            .whereEqualTo("renterId", userId)
            .get()
            .addOnSuccessListener { renterDocs ->
                // Get chats where user is agency
                firestore.collection("chats")
                    .whereEqualTo("agencyId", userId)
                    .get()
                    .addOnSuccessListener { agencyDocs ->
                        val allDocs = renterDocs.documents + agencyDocs.documents
                        val seen = mutableSetOf<String>()
                        val chats = allDocs.mapNotNull { doc ->
                            try {
                                val chatId = doc.getString("chatId") ?: return@mapNotNull null
                                if (!seen.add(chatId)) return@mapNotNull null // skip duplicate
                                ChatThread(
                                    chatId = chatId,
                                    renterId = doc.getString("renterId") ?: "",
                                    renterName = doc.getString("renterName") ?: "",
                                    renterEmail = doc.getString("renterEmail") ?: "",
                                    agencyId = doc.getString("agencyId") ?: "",
                                    agencyName = doc.getString("agencyName") ?: "",
                                    carName = doc.getString("carName") ?: "",
                                    lastMessage = doc.getString("lastMessage") ?: "",
                                    lastMessageTime = doc.getLong("lastMessageTime") ?: 0L
                                )
                            } catch (e: Exception) { null }
                        }.sortedByDescending { it.lastMessageTime }
                        onSuccess(chats)
                    }
                    .addOnFailureListener { e -> onFailure(e) }
            }
            .addOnFailureListener { e -> onFailure(e) }
    }
}

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val timestamp: Long = 0L
)

data class ChatThread(
    val chatId: String = "",
    val renterId: String = "",
    val renterName: String = "",
    val renterEmail: String = "",
    val agencyId: String = "",
    val agencyName: String = "",
    val carName: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L
)
