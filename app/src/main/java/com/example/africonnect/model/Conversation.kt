package com.example.africonnect.model

data class Conversation(
    val uid             : String  = "",
    val otherUserId     : String  = "",
    val otherUserName   : String  = "",
    val otherUserAvatar : String  = "",
    val lastMessage     : String  = "",
    val lastMessageTime : Long    = 0L,
    val unreadCount     : Int     = 0,
    val isOnline        : Boolean = false,
    val isPhoto         : Boolean = false
)