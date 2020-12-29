package io.pokr.network.model

import io.pokr.game.*
import java.util.*

class GameSession(
    val uuid: String,
    var gameEngine: HoldemTournamentGameEngine,
    val playerSessions: MutableList<PlayerSession> = Collections.synchronizedList(mutableListOf()),
    val created: Long = System.currentTimeMillis(),
) {

    override fun equals(other: Any?) =
        other is GameSession && other.uuid == uuid

    override fun hashCode(): Int {
        return uuid.hashCode()
    }
}