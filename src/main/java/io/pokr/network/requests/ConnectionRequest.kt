package io.pokr.network.requests

import io.pokr.game.model.*
import java.beans.*

data class ConnectionRequest @ConstructorProperties("gameUUID", "playerUUID", "name", "gameConfig") constructor(
    val gameUUID: String?,
    val playerUUID: String?,
    val name: String,
    val gameConfig: GameConfig?,
)