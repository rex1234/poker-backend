package io.pokr.network

import io.pokr.game.GameEngine
import io.pokr.game.model.Game
import io.pokr.game.model.Player
import io.pokr.network.model.GameSession
import io.pokr.network.model.PlayerAction
import io.pokr.network.model.PlayerSession
import io.pokr.network.util.TokenGenerator

/**
 * Class holding player and game sessions and their respective mappings to Game objects
 */
class GamePool {

    private val gameSessions: MutableList<GameSession> = mutableListOf()
    private val gameEngines = mutableMapOf<GameSession, GameEngine>()

    /**
     * Called when a game should be discarded
     */
    var gameDisbandedListener: ((List<PlayerSession>) -> Unit)? = null

    /**
     * Created a new game game discarding any previous games with the same UUID (should not happen outside of debugging).
     * It contains the starting player as an admin.
     */
    fun createGame(playerSession: String, playerName: String) {
        val playerSession = PlayerSession(playerSession, TokenGenerator.nextToken())

        val gameSession = GameSession(/* UUID.randomUUID().toString() */ "12345").apply {
            playerSessions.add(playerSession)
        }

        //clear previous session, if any
        discardGame(gameSession.uuid)

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

    /**
     * Connects a player session to a game with given gameUUID. Throws IllegalArgumentException if the game does not exist.
     * If playerUUID is given it will try to reconnect a player to a previous session
     */
    fun connectToGame(
        session: String,
        gameUuid: String,
        playerUuid: String?,
        playerName: String
    ) {
        gameSessions.firstOrNull { it.uuid == gameUuid }?.let { gameSession ->
            val playerSession = gameSession.playerSessions.firstOrNull { it.uuid == playerUuid }?.also {
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

    /**
     * Discards all associated games and player sessions for a game with given gameUUID
     */
    fun discardGame(gameUuid: String) {
        gameSessions.firstOrNull { it.uuid == gameUuid }?.apply {
            gameDisbandedListener?.invoke(playerSessions)
            playerSessions.clear()
            gameEngines.remove(this)
            gameSessions.remove(this)
        }
    }

    /**
     * Sets connected flag to false for a player with given session
     */
    fun playerDisconnected(session: String) {
        gameSessions.map { it.playerSessions }.flatten().firstOrNull { it.sessionId == session }?.uuid?.let {
            getGameDataForPlayerUuid(it).second.connected = false
        }
    }

    /**
     * Executes a command on a player with given session
     */
    fun executePlayerActionOnSession(session: String, action: PlayerAction) {
        val gameSession = gameSessions.firstOrNull { session in it.playerSessions.map { it.sessionId } }
            ?: throw IllegalArgumentException("No such player in any game session")

        val playerSession = gameSession.playerSessions.first { it.sessionId == session }

        executePlayerActionOnSession(playerSession.uuid, action)
    }

    /**
     * Executes a command on a player with given playerUUID
     */
    fun executePlayerAction(playerUuid: String, action: PlayerAction) {
        val gameSession = gameSessions.firstOrNull { playerUuid in it.playerSessions.map { it.uuid } }
            ?: throw IllegalArgumentException("No such player in any game session")

        val playerSession = gameSession.playerSessions.first { it.uuid == playerUuid }

        val game = gameEngines[gameSession]!!

        System.err.println("Executing action {$action} on player ${playerSession.uuid}")

        when(action.action) {
            PlayerAction.Action.CHANGE_NAME ->
                game.changePlayerName(playerSession.uuid, action.textValue!!)
        }
    }

    // TODO refactor
    /**
     * Returns sessions of all other players in the same game as the session player
     */
    fun getGroupSessions(session: String) =
        gameSessions.firstOrNull() { session in it.playerSessions.map { it.sessionId } }?.playerSessions
            ?: listOf<PlayerSession>()

    /**
     * Returns Player and Game instances for a player with given uuid
     */
    fun getGameDataForPlayerUuid(playerUuid: String): Pair<Game, Player> {
        val gameSession = gameSessions.first { playerUuid in it.playerSessions.map { it.uuid } }
        val playerSession = gameSession.playerSessions.first { it.uuid == playerUuid}

        val game = gameEngines[gameSession]!!.game
        val player = game.players.first { it.uuid == playerSession.uuid }

        return Pair(game, player)
    }
}