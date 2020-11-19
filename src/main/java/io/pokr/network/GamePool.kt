package io.pokr.network

import io.pokr.game.*
import io.pokr.game.exceptions.*
import io.pokr.game.model.*
import io.pokr.network.model.*
import io.pokr.network.responses.*
import io.pokr.network.util.*
import org.slf4j.*

/**
 * Class holding player and game sessions and their respective mappings to Game objects
 */
class GamePool {

    private val logger = LoggerFactory.getLogger(GamePool::class.java)

    val gameSessions = mutableMapOf<String, GameSession>()

    /**
     * Called when a game should be discarded
     */
    var gameDisbandedListener: ((playerSessionIds: List<String>) -> Unit)? = null

    /**
     * Called when game state should be sent to connected clients
     */
    var gameStateUpdatedListener: ((playerSession: String, gameState: GameResponse.GameState) -> Unit)? = null

    /**
     * Created a new game game discarding any previous games with the same UUID (should not happen outside of debugging).
     * It contains the starting player as an admin.
     */
    fun createGame(gameConfig: GameConfig, playerSessionId: String, playerName: String) {

        if (!gameConfig.isValid) {
            throw GameException(1, "Invalid game configuration", "Config: $gameConfig")
        }

        val playerSession = PlayerSession(playerSessionId, TokenGenerator.nextPlayerToken())

        val gameUuid = TokenGenerator.nextGameToken()

        // clear previous session, if any
        discardGame(gameUuid)

        logger.info("Created game session: ${gameUuid}")

        val gameEngine = HoldemTournamentGameEngine(
            gameUuid = gameUuid,
            gameConfig = gameConfig,

            updateStateListener = {
                notifyPlayers(it.gameData.uuid)
            },

            gameFinishedListener = {
                discardGame(it.gameData.uuid)
            },

            playerKickedListener = { gameEngine, player ->
                removePlayerSession(gameEngine.gameData.uuid, player.uuid)
            }
        )

        val gameSession = GameSession(gameUuid, gameEngine).apply {
            playerSessions.add(playerSession)
        }

        gameSessions[gameUuid] = gameSession

        gameEngine.addPlayer(playerSession.uuid)

        logger.info("Added player ${playerSession.uuid} to ${gameSession.uuid}")

        executePlayerAction(
            playerSession.uuid, PlayerAction(
                action = PlayerAction.Action.CHANGE_NAME,
                textValue = playerName
            )
        )

        notifyPlayers(gameSession.uuid)
    }

    /**
     * Connects a player session to a game with given gameUUID. Throws GameException if the game does not exist.
     * If playerUUID is given it will try to reconnect a player to a previous session
     */
    fun connectToGame(
        playerSessionId: String,
        gameUuid: String,
        playerUuid: String?,
        playerName: String,
    ) {
        gameSessions[gameUuid]?.let { gameSession ->
            if (playerName.length > 10 || playerName.length == 0) {
                throw GameException(7, "Invalid name", "Name: $playerName")
            }

            val playerSession = gameSession.playerSessions.firstOrNull {
                it.uuid == playerUuid
            }?.also {
                it.sessionId = playerSessionId
            } ?: PlayerSession(playerSessionId, TokenGenerator.nextPlayerToken()).also {
                gameSession.playerSessions.add(it)
                gameSession.gameEngine.addPlayer(it.uuid)
            }

            logger.info("Added player ${playerName} to ${gameSession.uuid}")

            gameSession.gameEngine.playerConnected(playerSession.uuid, true)

            executePlayerAction(
                playerSession.uuid,
                PlayerAction(
                    action = PlayerAction.Action.CHANGE_NAME,
                    textValue = playerName
                )
            )

            notifyPlayers(gameUuid)
        } ?: throw GameException(20, "Invalid game UUID")
    }

    /**
     * Sets connected flag to false for a player with given session
     */
    fun playerDisconnected(playerSessionId: String) {
        getGameSessionByPlayerSession(playerSessionId)?.let { gameSession ->
            val playerUuid = gameSession.playerSessions.first {
                it.sessionId == playerSessionId
            }.uuid

            gameSession.gameEngine.playerConnected(playerUuid, false)

            notifyPlayers(gameSession.uuid)
        }
    }

    /**
     * Discards all associated games and player sessions for a game with given gameUUID
     */
    fun discardGame(gameUuid: String) {
        gameSessions[gameUuid]?.let {
            gameDisbandedListener?.invoke(it.playerSessions.map { it.sessionId })
            gameSessions.remove(it.uuid)

            logger.info("Game session ${it.uuid} discarded")
        }
    }

    /**
     * Executes a command on a player with given session
     */
    fun executePlayerActionOnSession(playerSessionId: String, action: PlayerAction) {
        val gameSession = getGameSessionByPlayerSession(playerSessionId)
            ?: throw GameException(21, "No such player in any game session", "Player session ID: $playerSessionId")

        val playerSession = gameSession.playerSessions.first {
            it.sessionId == playerSessionId
        }

        executePlayerAction(playerSession.uuid, action)
        notifyPlayers(gameSession.uuid)
    }

    /**
     * Requests current game state for all connected players
     */
    fun requestGameState(playerSessionId: String) =
        getGameSessionByPlayerSession(playerSessionId)?.let {
            notifyPlayers(it.uuid)
        }

    /**
     * Sends given message to all other players in the same GameSession
     */
    fun sendChatMessage(playerSessionId: String, message: String, isFlash: Boolean) =
        getGameSessionByPlayerSession(playerSessionId)?.let {
            notifyPlayers(it.uuid) // TODO: send chat message
        }

    /**
     * Executes a command on a player with given playerUUID
     */
    private fun executePlayerAction(playerUuid: String, action: PlayerAction) {
        val gameSession = getGameSessionByPlayerUuid(playerUuid)
            ?: throw GameException(21, "No such player in any game session", "Player UUID: $playerUuid")

        val gameEngine = gameSession.gameEngine

        val player = gameEngine.gameData.allPlayers.firstOrNull { it.uuid == playerUuid }
        logger.debug("Executing action {$action} on player ${player?.name} [${gameSession.uuid}]")

        when (action.action) {
            PlayerAction.Action.CHANGE_NAME ->
                gameEngine.changeName(playerUuid, action.textValue!!)
            PlayerAction.Action.START_GAME ->
                gameEngine.startGame(playerUuid)
            PlayerAction.Action.SHOW_CARDS ->
                gameEngine.showCards(playerUuid)
            PlayerAction.Action.KICK ->
                gameEngine.kickPlayer(playerUuid, action.numericValue!!)
            PlayerAction.Action.LEAVE ->
                gameEngine.leave(playerUuid)
            PlayerAction.Action.DISCARD_GAME ->
                null // TODO
            PlayerAction.Action.REBUY ->
                gameEngine.rebuy(playerUuid)
            PlayerAction.Action.PAUSE ->
                gameEngine.pause(playerUuid, action.numericValue == 1)
            else ->
                gameEngine.nextPlayerMove(playerUuid, action)
        }
    }

    /**
     * Sends current game state to all players in a GameSession with given gameUuid
     */
    private fun notifyPlayers(gameUuid: String) {
        gameSessions[gameUuid]?.let { gameSession ->
            gameSession.playerSessions.forEach {
                gameStateUpdatedListener?.invoke(
                    it.sessionId,
                    GameResponse.GameStateFactory.from(gameSession.gameEngine.gameData, it.uuid)
                )
            }
        }
    }

    /**
     * Removes player from their player GameSession
     */
    private fun removePlayerSession(gameUuid: String, playerUuid: String) {
        gameSessions[gameUuid]?.playerSessions?.removeIf {
            if (it.uuid == playerUuid) {
                logger.debug("Player $playerUuid removed from game session ${gameUuid}")
                true
            } else {
                false
            }
        }
    }

    private fun getGameSessionByPlayerSession(playerSessionId: String) =
        gameSessions.values.firstOrNull {
            playerSessionId in it.playerSessions.map { it.sessionId }
        }

    private fun getGameSessionByPlayerUuid(playerUuid: String) =
        gameSessions.values.firstOrNull {
            playerUuid in it.playerSessions.map { it.uuid }
        }
}