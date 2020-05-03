package io.pokr.network

import io.pokr.game.*
import io.pokr.game.exceptions.*
import io.pokr.game.model.*
import io.pokr.network.model.*
import io.pokr.network.util.*
import org.slf4j.*

/**
 * Class holding player and game sessions and their respective mappings to Game objects
 */
class GamePool {

    val gameSessions: MutableList<GameSession> = mutableListOf()
    val gameEngines = mutableMapOf<GameSession, HoldemTournamentGameEngine>()

    /**
     * Called when a game should be discarded
     */
    var gameDisbandedListener: ((List<PlayerSession>) -> Unit)? = null

    /**
     * Called when game state should by synced
     */
    var updateStateListener: ((List<PlayerSession>) -> Unit)? = null

    val logger = LoggerFactory.getLogger(GamePool::class.java)

    /**
     * Created a new game game discarding any previous games with the same UUID (should not happen outside of debugging).
     * It contains the starting player as an admin.
     */
    fun createGame(gameConfig: GameConfig, session: String, playerName: String) {

        if(!gameConfig.isValid) {
            throw GameException(1, "Invalid gameConfig")
        }

        val playerSession = PlayerSession(session, TokenGenerator.nextPlayerToken())

        val gameSession = GameSession(TokenGenerator.nextGameToken()).apply {
            playerSessions.add(playerSession)
        }

        //clear previous session, if any
        discardGame(gameSession.uuid)

        gameSessions.add(gameSession)

        logger.info("Created game session: ${gameSession.uuid}")

        gameEngines[gameSession] =
            HoldemTournamentGameEngine(
                gameUuid = gameSession.uuid,
                updateStateListener = { gameEngine ->
                    updateStateListener?.invoke(gameSession.playerSessions)
                },
                gameFinishedListener = {
                    discardGame(it.game.uuid)
                },
                playerKickedListener = { gameEngine, player ->
                    gameSession.playerSessions.removeIf {
                        if(it.uuid == player.uuid) {
                            logger.debug("Player ${it.uuid} removed from game session ${gameSession.uuid}")
                            true
                        } else {
                            false
                        }
                    }
                }
            ).apply {
                initGame(gameConfig)
            }

        gameEngines[gameSession]!!.addPlayer(playerSession.uuid)

        logger.info("Added player ${playerSession.uuid} to ${gameSession.uuid}")

        executePlayerAction(playerSession.uuid, PlayerAction(
            action = PlayerAction.Action.CHANGE_NAME,
            textValue = playerName
        ))
    }

    /**
     * Connects a player session to a game with given gameUUID. Throws GameException if the game does not exist.
     * If playerUUID is given it will try to reconnect a player to a previous session
     */
    fun connectToGame(
        session: String,
        gameUuid: String,
        playerUuid: String?,
        playerName: String
    ) {
        gameSessions.firstOrNull { it.uuid == gameUuid }?.let { gameSession ->
            if(playerName.length > 10 || playerName.length == 0) {
                throw GameException(7, "Invalid name")
            }

            val playerSession = gameSession.playerSessions.firstOrNull { it.uuid == playerUuid }?.also {
                it.sessionId = session
            } ?: PlayerSession(session, TokenGenerator.nextPlayerToken()).also {
                gameSession.playerSessions.add(it)
                gameEngines[gameSession]!!.addPlayer(it.uuid)
            }

            logger.info("Added player ${playerName} to ${gameSession.uuid}")

            getGameDataForPlayerUuid(playerSession.uuid).second.isConnected = true

            executePlayerAction(playerSession.uuid,
                PlayerAction(
                    action = PlayerAction.Action.CHANGE_NAME,
                    textValue = playerName
                )
            )
        } ?: throw GameException(20, "Invalid game UUID")
    }

    /**
     * Discards all associated games and player sessions for a game with given gameUUID
     */
    fun discardGame(gameUuid: String) {
        gameSessions.filter { it.uuid == gameUuid }.forEach {
            gameDisbandedListener?.invoke(it.playerSessions)
            it.playerSessions.clear()
            gameEngines.remove(it)
            gameSessions.remove(it)

            logger.info("Game session ${it.uuid} discarded")
        }
    }

    /**
     * Sets connected flag to false for a player with given session
     */
    fun playerDisconnected(session: String) {
        gameSessions.map { it.playerSessions }.flatten().firstOrNull { it.sessionId == session }?.uuid?.let {
            getGameDataForPlayerUuid(it).second.isConnected = false
        }
    }

    /**
     * Executes a command on a player with given session
     */
    fun executePlayerActionOnSession(session: String, action: PlayerAction) {
        val gameSession = gameSessions.firstOrNull { session in it.playerSessions.map { it.sessionId } }
            ?: throw GameException(21, "No such player in any game session")

        val playerSession = gameSession.playerSessions.first { it.sessionId == session }

        executePlayerAction(playerSession.uuid, action)
    }

    /**
     * Executes a command on a player with given playerUUID
     */
    fun executePlayerAction(playerUuid: String, action: PlayerAction) {
        val gameSession = gameSessions.firstOrNull { playerUuid in it.playerSessions.map { it.uuid } }
            ?: throw GameException(21, "No such player in any game session")

        val playerSession = gameSession.playerSessions.first { it.uuid == playerUuid }

        val gameEngine = gameEngines[gameSession]!!

        val player = gameEngine.game.allPlayers.firstOrNull { it.uuid == playerSession.uuid }
        logger.debug("Executing action {$action} on player ${player?.name} [${gameSession.uuid}]")

        when(action.action) {
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

    // TODO refactor
    /**
     * Returns sessions of all other players in the same game as the session player
     */
    fun getGroupSessions(session: String) =
        gameSessions.firstOrNull { session in it.playerSessions.map { it.sessionId } }?.playerSessions
            ?: listOf<PlayerSession>()

    /**
     * Returns Player and Game instances for a player with given uuid
     */
    fun getGameDataForPlayerUuid(playerUuid: String): Pair<Game, Player> {
        val gameSession = gameSessions.first { playerUuid in it.playerSessions.map { it.uuid } }
        val playerSession = gameSession.playerSessions.first { it.uuid == playerUuid}

        val game = gameEngines[gameSession]!!.game
        val player = game.allPlayers.first { it.uuid == playerSession.uuid }

        return Pair(game, player)
    }
}