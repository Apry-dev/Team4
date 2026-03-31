package com.example.esnmessenger.model

import com.google.firebase.Timestamp

data class Message(
    val id: String = "",
    val fromId: String = "",
    val toId: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now()
)
