package com.example.africonnect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.africonnect.data.ChatRepository
import com.example.africonnect.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.africonnect.model.MessageType

class ChatViewModel : ViewModel() {

    private val repository = ChatRepository()
    private val auth = FirebaseAuth.getInstance()

    val currentUserId: String get() = auth.currentUser?.uid ?: ""

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var activeChatId: String = ""
    private var activeOtherUserId: String = ""
    private var activeOtherUserName: String = ""

    // ── Call this when entering a chat screen ────────────────────────────────
    fun loadMessages(otherUserId: String, otherUserName: String) {
        if (currentUserId.isEmpty()) return
        activeChatId       = repository.getChatId(currentUserId, otherUserId)
        activeOtherUserId  = otherUserId
        activeOtherUserName = otherUserName

        _isLoading.value = true

        viewModelScope.launch {
            repository.observeMessages(activeChatId).collect { msgs ->
                _messages.value = msgs
                _isLoading.value = false
            }
        }

        // Mark as read when opening the chat
        repository.markAsRead(activeChatId, currentUserId, otherUserId)
    }

    // ── Send a plain text message ────────────────────────────────────────────
    fun sendMessage(text: String) {
        if (text.isBlank() || currentUserId.isEmpty() || activeChatId.isEmpty()) return

        val message = ChatMessage(
            id        = "",
            senderId  = currentUserId,
            text      = text.trim(),
            timestamp = System.currentTimeMillis(),
            type      = MessageType.TEXT
        )

        repository.sendMessage(
            chatId        = activeChatId,
            message       = message,
            currentUserId = currentUserId,
            otherUserId   = activeOtherUserId,
            otherUserName = activeOtherUserName
        )
    }

    // ── Format timestamp for display ─────────────────────────────────────────
    fun formatTimestamp(timestamp: Long): String {
        if (timestamp == 0L) return ""
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> {
                val mins = (diff / 60_000).toInt()
                "$mins min ago"
            }
            else -> {
                val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                sdf.format(java.util.Date(timestamp))
            }
        }
    }
}