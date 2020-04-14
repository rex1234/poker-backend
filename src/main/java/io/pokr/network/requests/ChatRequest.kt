package io.pokr.network.requests

import java.beans.*

class ChatRequest @ConstructorProperties( "message", "flash") constructor(
    val message: String,
    val flash: Boolean = false
)