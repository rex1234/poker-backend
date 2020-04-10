package io.pokr.network.requests

import io.pokr.game.model.GameConfig
import java.beans.ConstructorProperties

class ChatRequest @ConstructorProperties( "message", "flash") constructor(
    val message: String,
    val flash: Boolean = false
)