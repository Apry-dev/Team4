package com.example.esnmessenger.model

data class ChatSummary(
    val chatId: String,
    val otherUserId: String,
    val otherUserName: String,
    val lastMessage: String,
    val timestamp: Long,
    val otherUserPhotoBase64: String? = null
)