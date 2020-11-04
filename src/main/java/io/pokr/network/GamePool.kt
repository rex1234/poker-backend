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

    val logger = LoggerFactory.getLogger(GamePool::class.java)
    val gameSessions: MutableList<GameSession> = mutableListOf()
    val gameEngines = mutableMapOf<String, HoldemTournamentGameEngine>()

    /**
     * Called when a game should be discarded
     */
    var gameDisbandedListener: ((playerSessions: List<PlayerSession>) -> Unit)? = null

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
            throw GameException(1, "Invalid gameConfig")
        }

        val playerSession = PlayerSession(playerSessionId, TokenGenerator.nextPlayerToken())

        val gameSession = GameSession(TokenGenerator.nextGameToken()).apply {
            playerSessions.add(playerSession)
        }

        // clear previous session, if any
        discardGame(gameSession.uuid)

        gameSessions.add(gameSession)

        logger.info("Created game session: ${gameSession.uuid}")

        val gameEngine = HoldemTournamentGameEngine(
            gameUuid = gameSession.uuid,
            gameConfig = gameConfig,

            updateStateListener = {
                notifyPlayers(it.game.uuid)
            },

            gameFinishedListener = {
                discardGame(it.game.uuid)
            },

            playerKickedListener = { gameEngine, player ->
                removePlayerSession(gameSession, player.uuid)
            }
        )

        gameEngines[gameSession.uuid] = gameEngine
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
        playerName: String
    ) {
        gameSessions.firstOrNull {
            it.uuid == gameUuid
        }?.let { gameSession ->
            if (playerName.length > 10 || playerName.length == 0) {
                throw GameException(7, "Invalid name")
            }

            val playerSession = gameSession.playerSessions.firstOrNull {
                it.uuid == playerUuid
            }?.also {
                it.sessionId = playerSessionId
            } ?: PlayerSession(playerSessionId, TokenGenerator.nextPlayerToken()).also {
                gameSession.playerSessions.add(it)
                gameEngines[gameSession.uuid]!!.addPlayer(it.uuid)
            }

            logger.info("Added player ${playerName} to ${gameSession.uuid}")

            gameEngines[gameSession.uuid]!!.playerConnected(playerSession.uuid, true)

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
        gameSessions.first {
            playerSessionId in it.playerSessions.map { it.sessionId }
        }.let {

        }

        gameSessions.flatMap { it.playerSessions }.firstOrNull {
            it.sessionId == playerSessionId
        }?.uuid?.let {
            //getGameDataForPlayerUuid(it).second.isConnected = false
            // TODO: notify
        }
    }

    /**
     * Discards all associated games and player sessions for a game with given gameUUID
     */
    fun discardGame(gameUuid: String) {
        gameSessions.filter {
            it.uuid == gameUuid
        }.forEach {
            gameDisbandedListener?.invoke(it.playerSessions)
            it.playerSessions.clear()
            gameEngines.remove(it.uuid)
            gameSessions.remove(it)

            logger.info("Game session ${it.uuid} discarded")
        }
    }


    /**
     * Executes a command on a player with given session
     */
    fun executePlayerActionOnSession(playerSessionId: String, action: PlayerAction) {
        val gameSession = gameSessions.firstOrNull {
            playerSessionId in it.playerSessions.map { it.sessionId }
        } ?: throw GameException(21, "No such player in any game session")

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
        gameSessions.filter {
            playerSessionId in it.playerSessions.map { it.sessionId }
        }.forEach {
            notifyPlayers(it.uuid)
        }

    /**
     * Sends given message to all other players in the same GameSession
     */
    fun sendChatMessage(playerSessionId: String, message: String, isFlash: Boolean) =
        gameSessions.filter {
            playerSessionId in it.playerSessions.map { it.sessionId }
        }.forEach {
            notifyPlayers(it.uuid)
        }

    /**
     * Executes a command on a player with given playerUUID
     */
    private fun executePlayerAction(playerUuid: String, action: PlayerAction) {
        val gameSession = gameSessions.firstOrNull {
            playerUuid in it.playerSessions.map { it.uuid }
        } ?: throw GameException(21, "No such player in any game session")

        val playerSession = gameSession.playerSessions.first { it.uuid == playerUuid }

        val gameEngine = gameEngines[gameSession.uuid]!!

        val player = gameEngine.game.allPlayers.firstOrNull { it.uuid == playerSession.uuid }
        logger.debug("Executing action {$action} on player ${player?.name} [${gameSession.uuid}]")

        when (action.action) {
            PlayerAction.Action.CHANGE_NAME ->
                gameEngine.changeName(playerSession.uuid, action.textValue!!)
            PlayerAction.Action.START_GAME ->
                gameEngine.startGame(playerSession.uuid)
            PlayerAction.Action.SHOW_CARDS ->
                gameEngine.showCards(playerSession.uuid)
            PlayerAction.Action.KICK ->
                gameEngine.kickPlayer(playerSession.uuid, action.numericValue!!)
            PlayerAction.Action.LEAVE ->
                gameEngine.leave(playerSession.uuid)
            PlayerAction.Action.DISCARD_GAME ->
                null // TODO
            PlayerAction.Action.REBUY ->
                gameEngine.rebuy(playerUuid)
            PlayerAction.Action.PAUSE ->
                gameEngine.pause(playerSession.uuid, action.numericValue == 1)
            else ->
                gameEngine.nextPlayerMove(playerSession.uuid, action)
        }
    }

    /**
     * Sends current game state to all players in a GameSession with given gameUuid
     */
    private fun notifyPlayers(gameUuid: String) {
        val gameSession = gameSessions.firstOrNull {
            it.uuid == gameUuid
        }

        val game = gameEngines[gameUuid]!!.game

        gameSession?.let {
            it.playerSessions.forEach {
                gameStateUpdatedListener?.invoke(
                    it.sessionId, GameResponse.GameStateFactory.from(game, it.uuid)
                )
            }
        }
    }

    /**
     * Removes player from their player GameSession
     */
    private fun removePlayerSession(gameSession: GameSession, playerUuid: String) {
        gameSession.playerSessions.removeIf {
            if (it.uuid == playerUuid) {
                logger.debug("Player $playerUuid removed from game session ${gameSession.uuid}")
                true
            } else {
                false
            }
        }
    }
}