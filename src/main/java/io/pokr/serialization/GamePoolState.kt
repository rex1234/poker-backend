package io.pokr.serialization

import io.pokr.network.model.*

class GamePoolState(
    val gameSessions: List<GameSessionState>,
    val serializedAt: Long = System.currentTimeMillis()
) {
    class GameSessionState(
        val uuid: String,
        var gameRestorePoint: GameRestorePoint,
        val playerSessions: List<PlayerSession>,
        val created: Long
    )
}
