package io.pokr.network

import io.pokr.game.GameEngine
import io.pokr.network.model.GameSession
import io.pokr.network.model.PlayerAction
import io.pokr.network.model.PlayerSession
import java.util.*

class GamePool {

    val gameSessions: MutableList<GameSession> = mutableListOf()
    val games = mutableMapOf<GameSession, GameEngine>()

    fun createGame(playerSession: String, playerName: String) {
        val playerSession = PlayerSession(playerSession, UUID.randomUUID().toString())

        //val gameSession = GameSession(UUID.randomUUID().toString()).apply {
        val gameSession = GameSession("1234").apply { // for debug
            playerSessions.add(playerSession)
        }

        gameSessions.add(gameSession)

        System.err.println("Created game session: ${gameSession.uuid}")

        games[gameSession] = GameEngine()
        games[gameSession]!!.addPlayer(playerSession.uuid)

        System.err.println("Added player ${playerSession.uuid} to ${gameSession.uuid}")

        executePlayerAction(playerSession.sessionId, PlayerAction(
            action = PlayerAction.Action.CHANGE_NAME,
            textValue = playerName
        ))
    }

    fun connectToGame(gameUUID: String, session: String, playerUUID: String, playerName: String) {
        gameSessions.firstOrNull { it.uuid == gameUUID }?.let { gameSession ->
            val playerSession = PlayerSession(session, playerUUID)

            gameSession.playerSessions.add(playerSession)
            games[gameSession]?.addPlayer(playerSession.uuid)

            System.err.println("Added player ${playerSession.uuid} to ${gameSession.uuid}")

            executePlayerAction(playerSession.sessionId, PlayerAction( // replace with UUID
                action = PlayerAction.Action.CHANGE_NAME,
                textValue = playerName
            ))
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
        val gameSession = gameSessions.firstOrNull { session in it.playerSessions.map { it.sessionId } }
            ?: throw IllegalArgumentException("No such player in any game session")

        val playerSession = gameSession.playerSessions.first { it.sessionId == session }

        val game = games[gameSession]!!

        System.err.println("Executing action {$action} on player ${playerSession.uuid}")

        when(action.action) {
            PlayerAction.Action.CHANGE_NAME ->
                game.changePlayerName(playerSession.uuid, action.textValue!!)
        }
    }
}