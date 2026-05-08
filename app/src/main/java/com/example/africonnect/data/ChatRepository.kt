package com.example.africonnect.data

import com.example.africonnect.model.ChatMessage
import com.example.africonnect.model.Conversation
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import com.example.africonnect.model.MessageType

class ChatRepository {

    private val db = FirebaseDatabase.getInstance().reference

    // ── Generate a stable chat ID from two uids (sorted so order doesn't matter)
    fun getChatId(uid1: String, uid2: String): String {
        return listOf(uid1, uid2).sorted().joinToString("_")
    }

    // ── Send a message + update conversation metadata ─────────────────────────
    fun sendMessage(
        chatId: String,
        message: ChatMessage,
        currentUserId: String,
        otherUserId: String,
        otherUserName: String
    ) {
        val msgRef = db.child("chats").child(chatId).child("messages").push()
        val msgId  = msgRef.key ?: return

        val msgData = mapOf(
            "id"        to msgId,
            "senderId"  to message.senderId,
            "text"      to message.text,
            "timestamp" to message.timestamp,
            "type"      to message.type.name,
            "mediaUrl"  to message.mediaUrl,
            "fileName"  to message.fileName,
            "fileSize"  to message.fileSize,
            "fileType"  to message.fileType,
            "isRead"    to false
        )
        msgRef.setValue(msgData)

        // Update conversation node for both users
        val convData = mapOf(
            "lastMessage"     to message.text.ifBlank { if (message.type == MessageType.IMAGE) "📷 Photo" else "📎 File" },
            "lastMessageTime" to message.timestamp,
            "isPhoto"         to (message.type == MessageType.IMAGE)
        )

        // Current user's conversation entry
        db.child("conversations").child(currentUserId).child(otherUserId)
            .updateChildren(convData + mapOf("otherUserName" to otherUserName, "otherUserId" to otherUserId))

        // Other user's conversation entry + increment unread
        db.child("conversations").child(otherUserId).child(currentUserId)
            .updateChildren(convData).also {
                db.child("conversations").child(otherUserId).child(currentUserId)
                    .child("unreadCount")
                    .runTransaction(object : Transaction.Handler {
                        override fun doTransaction(data: MutableData): Transaction.Result {
                            val current = data.getValue(Int::class.java) ?: 0
                            data.value = current + 1
                            return Transaction.success(data)
                        }
                        override fun onComplete(e: DatabaseError?, b: Boolean, s: DataSnapshot?) {}
                    })
            }
    }

    // ── Observe messages in real time ────────────────────────────────────────
    fun observeMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val ref = db.child("chats").child(chatId).child("messages")
            .orderByChild("timestamp")

        val listener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { snap ->
                    val typeStr = snap.child("type").getValue(String::class.java) ?: "TEXT"
                    val type = try { MessageType.valueOf(typeStr) } catch (e: Exception) { MessageType.TEXT }
                    ChatMessage(
                        id        = snap.child("id").getValue(String::class.java) ?: snap.key ?: "",
                        senderId  = snap.child("senderId").getValue(String::class.java) ?: "",
                        text      = snap.child("text").getValue(String::class.java) ?: "",
                        timestamp = snap.child("timestamp").getValue(Long::class.java) ?: 0L,
                        type      = type,
                        mediaUrl  = snap.child("mediaUrl").getValue(String::class.java) ?: "",
                        fileName  = snap.child("fileName").getValue(String::class.java) ?: "",
                        fileSize  = snap.child("fileSize").getValue(Long::class.java) ?: 0L,
                        fileType  = snap.child("fileType").getValue(String::class.java) ?: "",
                        isRead    = snap.child("isRead").getValue(Boolean::class.java) ?: false
                    )
                }
                trySend(messages)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        })

        awaitClose { ref.removeEventListener(listener) }
    }

    // ── Observe conversations for a user ─────────────────────────────────────
    fun observeConversations(userId: String): Flow<List<Conversation>> = callbackFlow {
        val ref = db.child("conversations").child(userId)

        val listener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { snap ->
                    Conversation(
                        uid             = snap.key ?: "",
                        otherUserId     = snap.child("otherUserId").getValue(String::class.java) ?: snap.key ?: "",
                        otherUserName   = snap.child("otherUserName").getValue(String::class.java) ?: "Unknown",
                        otherUserAvatar = snap.child("otherUserAvatar").getValue(String::class.java) ?: "",
                        lastMessage     = snap.child("lastMessage").getValue(String::class.java) ?: "",
                        lastMessageTime = snap.child("lastMessageTime").getValue(Long::class.java) ?: 0L,
                        unreadCount     = snap.child("unreadCount").getValue(Int::class.java) ?: 0,
                        isOnline        = snap.child("isOnline").getValue(Boolean::class.java) ?: false,
                        isPhoto         = snap.child("isPhoto").getValue(Boolean::class.java) ?: false
                    )
                }.sortedByDescending { it.lastMessageTime }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        })

        awaitClose { ref.removeEventListener(listener) }
    }

    // ── Mark messages as read ────────────────────────────────────────────────
    fun markAsRead(chatId: String, currentUserId: String, otherUserId: String) {
        // Reset unread count
        db.child("conversations").child(currentUserId).child(otherUserId)
            .child("unreadCount").setValue(0)

        // Mark individual messages as read
        db.child("chats").child(chatId).child("messages")
            .orderByChild("isRead").equalTo(false)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach { msg ->
                        val senderId = msg.child("senderId").getValue(String::class.java)
                        if (senderId != currentUserId) {
                            msg.ref.child("isRead").setValue(true)
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }
}