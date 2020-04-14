package io.pokr.network.responses

class ChatResponse(
    val name: String,
    val index: String,
    val time: Long,
    val message: String,
    val flash: Boolean = false
)