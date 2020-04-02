package io.pokr.network

import io.pokr.game.GameEngine
import io.pokr.network.model.GameSession
import io.pokr.network.model.PlayerAction
import io.pokr.network.model.PlayerSession
import java.util.*

class GamePool {

    val gameSessions: MutableList<GameSession> = mutableListOf()
    val games = mutableMapOf<GameSession, GameEngine>()

    fun createGame(initialPlayer: PlayerSession): GameSession {
        val gameSession = GameSession(UUID.randomUUID().toString()).apply {
            playerSessions.add(initialPlayer)
        }

        games[gameSession] = GameEngine()

        return gameSession
    }

    fun connectToGame(gameUUID: String, session: String, playerUUID: String) {
        gameSessions.firstOrNull { it.uuid == gameUUID }?.let { gameSession ->
            val playerSession = PlayerSession(session, playerUUID)
            gameSession.playerSessions.add(playerSession)
//            games[gameSession]?.addPlayer(playerSession)
        } ?: throw IllegalArgumentException("Invalid game UUID")
    }

    fun discardGame(gameUUID: String) {
        gameSessions.firstOrNull { it.uuid == gameUUID }?.let {
            gameSessions.remove(it)
            it.playerSessions.forEach {
                // disconnect players
            }
        } ?: throw IllegalArgumentException("Invalid game UUID")
    }

    fun executePlayerAction(session: String, action: PlayerAction) {
        val gameSession = gameSessions.firstOrNull { session in it.playerSessions.map { it.sessionId } } ?:
                throw IllegalArgumentException("No such player in any game session")

        val playerSession = gameSession.playerSessions.first { it.sessionId == session }

        val game = games[gameSession]!!

        when(action.action) {
            PlayerAction.Action.CHANGE_NAME ->
                game.changePlayerName(playerSession.uuid, action.textValue!!)
        }
    }
}