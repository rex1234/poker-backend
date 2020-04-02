package io.pokr.network

import io.pokr.game.GameEngine
import io.pokr.game.model.Game
import io.pokr.game.model.Player
import io.pokr.network.model.GameSession
import io.pokr.network.model.PlayerAction
import io.pokr.network.model.PlayerSession
import io.pokr.network.responses.GameResponse
import io.pokr.network.util.TokenGenerator
import java.util.*

class GamePool {

    private val gameSessions: MutableList<GameSession> = mutableListOf()
    private val games = mutableMapOf<GameSession, GameEngine>()

    fun createGame(playerSession: String, playerName: String) {
        val playerSession = PlayerSession(playerSession, TokenGenerator.nextToken())

        val gameSession = GameSession(/* UUID.randomUUID().toString() */ "12345").apply {
            playerSessions.add(playerSession)
        }
        gameSessions.add(gameSession)

        System.err.println("Created game session: ${gameSession.uuid}")

        games[gameSession] = GameEngine(gameSession.uuid)
        games[gameSession]!!.addPlayer(playerSession.uuid)

        System.err.println("Added player ${playerSession.uuid} to ${gameSession.uuid}")

        executePlayerAction(playerSession.uuid, PlayerAction(
            action = PlayerAction.Action.CHANGE_NAME,
            textValue = playerName
        ))
    }

    fun connectToGame(
        session: String,
        gameUUID: String,
        playerUUID: String?,
        playerName: String
    ) {
        gameSessions.firstOrNull { it.uuid == gameUUID }?.let { gameSession ->
            val playerSession = gameSession.playerSessions.firstOrNull { it.uuid == playerUUID }?.also {
                it.sessionId = session
            } ?: PlayerSession(session, TokenGenerator.nextToken()).also {
                gameSession.playerSessions.add(it)
                games[gameSession]!!.addPlayer(it.uuid)
            }

            System.err.println("Added player ${playerSession.uuid} to ${gameSession.uuid}")

            getGameDataForPlayerSession(session).second.connected = true

            executePlayerAction(playerSession.uuid, PlayerAction( // replace with UUID
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

    fun playerDisconnected(session: String) {
        getGameDataForPlayerSession(session).second.connected = false
    }

    fun executePlayerActionOnSession(session: String, action: PlayerAction) {
        val gameSession = gameSessions.firstOrNull { session in it.playerSessions.map { it.sessionId } }
            ?: throw IllegalArgumentException("No such player in any game session")

        val playerSession = gameSession.playerSessions.first { it.sessionId == session }

        executePlayerActionOnSession(playerSession.uuid, action)
    }

    fun executePlayerAction(playerUUID: String, action: PlayerAction) {
        val gameSession = gameSessions.firstOrNull { playerUUID in it.playerSessions.map { it.uuid } }
            ?: throw IllegalArgumentException("No such player in any game session")

        val playerSession = gameSession.playerSessions.first { it.uuid == playerUUID }

        val game = games[gameSession]!!

        System.err.println("Executing action {$action} on player ${playerSession.uuid}")

        when(action.action) {
            PlayerAction.Action.CHANGE_NAME ->
                game.changePlayerName(playerSession.uuid, action.textValue!!)
        }
    }

    // TODO refactor
    fun getGameDataForPlayerSession(session: String): Pair<Game, Player> {
        val gameSession = gameSessions.first { session in it.playerSessions.map { it.sessionId } }
        val playerSession = gameSession.playerSessions.first { it.sessionId == session}

        val game = games[gameSession]!!.game
        val player = game.players.first { it.uuid == playerSession.uuid }

        return Pair(game, player)
    }
}