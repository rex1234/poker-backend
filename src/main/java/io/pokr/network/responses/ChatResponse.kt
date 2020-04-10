package io.pokr.network.responses

import io.pokr.game.model.GameConfig
import java.beans.ConstructorProperties

class ChatResponse(
    val name: String,
    val index: String,
    val time: Long,
    val message: String,
    val flash: Boolean = false
)