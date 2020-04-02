package io.pokr.network.requests

import java.beans.ConstructorProperties

class ConnectionRequest @ConstructorProperties("gameUUID", "name") constructor(
    val gameUUID: String?,
    val name: String
)