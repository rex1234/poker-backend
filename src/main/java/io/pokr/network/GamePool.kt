package io.pokr.network

import io.pokr.game.GameEngine
import io.pokr.game.model.Game
import io.pokr.game.model.Player
import io.pokr.network.model.GameSession
import io.pokr.network.model.PlayerAction
import io.pokr.network.model.PlayerSession
import io.pokr.network.util.TokenGenerator

class GamePool {

    private val gameSessions: MutableList<GameSession> = mutableListOf()
    private val gameEngines = mutableMapOf<GameSession, GameEngine>()

    fun createGame(playerSession: String, playerName: String) {
        val playerSession = PlayerSession(playerSession, TokenGenerator.nextToken())

        val gameSession = GameSession(/* UUID.randomUUID().toString() */ "12345").apply {
            playerSessions.add(playerSession)
        }
        gameSessions.firstOrNull { it.uuid == gameSession.uuid }?.playerSessions?.clear()

        gameSessions.add(gameSession)


        System.err.println("Created game session: ${gameSession.uuid}")

        gameEngines[gameSession] = GameEngine(gameSession.uuid)
        gameEngines[gameSession]!!.addPlayer(playerSession.uuid)

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
                gameEngines[gameSession]!!.addPlayer(it.uuid)
            }

            System.err.println("Added player ${playerSession.uuid} to ${gameSession.uuid}")

            getGameDataForPlayerUuid(playerSession.uuid).second.connected = true

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
        gameSessions.map { it.playerSessions }.flatten().firstOrNull { it.sessionId == session }?.uuid?.let {
            getGameDataForPlayerUuid(it).second.connected = false
        }
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

        val game = gameEngines[gameSession]!!

        System.err.println("Executing action {$action} on player ${playerSession.uuid}")

        when(action.action) {
            PlayerAction.Action.CHANGE_NAME ->
                game.changePlayerName(playerSession.uuid, action.textValue!!)
        }
    }

    // TODO refactor
    fun getGroupSessions(session: String) =
        gameSessions.first { session in it.playerSessions.map { it.sessionId } }.playerSessions

    fun getGameDataForPlayerUuid(uuid: String): Pair<Game, Player> {
        val gameSession = gameSessions.first { uuid in it.playerSessions.map { it.uuid } }
        val playerSession = gameSession.playerSessions.first { it.uuid == uuid}

        val game = gameEngines[gameSession]!!.game
        val player = game.players.first { it.uuid == playerSession.uuid }

        return Pair(game, player)
    }
}