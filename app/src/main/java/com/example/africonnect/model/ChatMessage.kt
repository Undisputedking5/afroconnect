package com.example.africonnect.model


data class ChatMessage(
    val id        : String      = "",
    val senderId  : String      = "",
    val text      : String      = "",
    val timestamp : Long        = 0L,
    val type      : MessageType = MessageType.TEXT,
    val mediaUrl  : String      = "",
    val fileName  : String      = "",
    val fileSize  : Long        = 0L,
    val fileType  : String      = "",
    val isRead    : Boolean     = false
)