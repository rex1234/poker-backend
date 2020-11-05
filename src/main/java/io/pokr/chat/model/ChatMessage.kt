package io.pokr.chat.model

data class ChatMessage(
    val from: String,
    val text: String,
    val isFlash: Boolean,
    val time: Long = System.currentTimeMillis()
)