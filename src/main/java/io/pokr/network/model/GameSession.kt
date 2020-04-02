package io.pokr.network.model

import io.pokr.game.model.Player

data class GameSession(
    val uuid: String,
    val playerSessions: MutableList<PlayerSession> = mutableListOf()
)