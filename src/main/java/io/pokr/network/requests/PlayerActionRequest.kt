package io.pokr.network.requests

import io.pokr.network.model.PlayerAction

class PlayerActionRequest(
    val gameUUID: String?,
    val playerAction: PlayerAction
)