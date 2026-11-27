package com.example.africonnect.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.africonnect.data.Repo
import com.example.africonnect.model.ChatMessage
import com.example.africonnect.model.MessageType
import com.example.africonnect.model.UserProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _other = MutableStateFlow<UserProfile?>(null)
    val other: StateFlow<UserProfile?> = _other

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var otherUid = ""
    private var jobs = mutableListOf<Job>()
    var visible = false

    fun open(uid: String) {
        if (uid == otherUid) return
        otherUid = uid
        jobs.forEach { it.cancel() }
        jobs.clear()
        jobs += viewModelScope.launch {
            Repo.observeUser(uid).catch { }.collect { _other.value = it }
        }
        jobs += viewModelScope.launch {
            Repo.observeMessages(uid)
                .map { s -> s.children.map { ChatMessage.from(it) } }
                .catch { _loading.value = false; _error.value = it.message }
                .collect { list ->
                    _messages.value = list
                    _loading.value = false
                    if (visible) Repo.markChatRead(uid, list)
                }
        }
    }

    fun markRead() { if (otherUid.isNotBlank()) Repo.markChatRead(otherUid, _messages.value) }

    fun send(me: UserProfile?, text: String) {
        val other = _other.value ?: UserProfile(uid = otherUid)
        if (me == null || text.isBlank()) return
        viewModelScope.launch {
            runCatching { Repo.sendMessage(other, me, text.trim()) }
                .onFailure { _error.value = it.message ?: "Message not sent" }
        }
    }

    fun sendImage(me: UserProfile?, uri: Uri) {
        val other = _other.value ?: UserProfile(uid = otherUid)
        if (me == null) return
        _sending.value = true
        viewModelScope.launch {
            runCatching {
                val cid = Repo.chatId(me.uid, other.uid)
                val url = Repo.uploadImage("chat_media/$cid/${System.currentTimeMillis()}.jpg", uri)
                Repo.sendMessage(other, me, "", MessageType.IMAGE, url)
            }.onFailure { _error.value = it.message ?: "Photo not sent" }
            _sending.value = false
        }
    }

    fun clearError() { _error.value = null }
}
