package io.pokr.network.requests

import io.pokr.game.model.GameConfig
import java.beans.ConstructorProperties

class ConnectionRequest @ConstructorProperties("gameUUID", "playerUUID", "name") constructor(
    val gameUUID: String?,
    val playerUUID: String?,
    val name: String,
    val gameConfig: GameConfig?
)