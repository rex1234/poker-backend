package io.pokr.network.requests

import java.beans.ConstructorProperties

class ConnectionRequest @ConstructorProperties("gameUUID", "playerUUID", "name") constructor(
    val gameUUID: String?,
    val playerUUID: String?,
    val name: String
)