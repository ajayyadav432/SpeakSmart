package com.example.speaksmart.data

import java.util.UUID

enum class MessageSender {
    USER,
    AI
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val sender: MessageSender,
    val timestamp: Long = System.currentTimeMillis(),
    val isPending: Boolean = false,
)
