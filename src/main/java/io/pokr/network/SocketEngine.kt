package io.pokr.network

import com.corundumstudio.socketio.*
import io.netty.channel.*
import io.pokr.config.*
import io.pokr.game.exceptions.*
import io.pokr.game.model.*
import io.pokr.network.requests.*
import io.pokr.network.responses.*
import org.slf4j.*
import java.io.*

/**
 * Class handling socket.io communication
 */
class SocketEngine(
    private val gamePool: GamePool,
) {

    enum class Events(
        val key: String,
    ) {
        // inbound
        ACTION("action"),
        CONNECT("connectGame"),
        GAME_REQUEST("gameRequest"),
        ERROR("error"),

        // outbound
        GAME_STATE("gameState"),
        GAME_DISBANDED("gameDisbanded"),

        // both ways
        CHAT_MESSAGE("chat")
    }

    lateinit var server: SocketIOServer

    private val logger = LoggerFactory.getLogger(SocketEngine::class.java)

    fun start() {
        val config = Configuration().apply {
            hostname = "0.0.0.0"
            port = PokrioConfig.socketsPort
            origin = PokrioConfig.webUrl
            socketConfig.isReuseAddress = true

            val keyStoreFile = File(PokrioConfig.keyStorePath ?: "")
            if (keyStoreFile.exists()) {
                keyStore = FileInputStream(keyStoreFile)
                keyStorePassword = PokrioConfig.keyStorePassword

                logger.info("Socket.io server with SSL")
            } else {
                logger.info("Socket.io server without SSL")
            }

            exceptionListener = object : com.corundumstudio.socketio.listener.ExceptionListener {
                override fun onConnectException(e: Exception, client: SocketIOClient?) {
                    e.printStackTrace()
                }

                override fun onEventException(e: Exception, args: MutableList<Any>?, client: SocketIOClient?) {
                    if (e is GameException) {
                        val additionalInfo = if (e.additionalInfo == null) null else " (${e.additionalInfo})"
                        logger.debug("Game exception: ${e.message}${additionalInfo.orEmpty()}")
                        client?.sendEvent(
                            Events.ERROR.key, ErrorResponse(
                                e.code, e.message ?: ""
                            )
                        )
                    } else {
                        logger.error("Event exception", e)
                        e.printStackTrace()
                    }
                }

                override fun onPingException(e: Exception, client: SocketIOClient?) {
                    e.printStackTrace()
                }

                override fun onDisconnectException(e: Exception, client: SocketIOClient?) {
                    e.printStackTrace()
                }

                override fun exceptionCaught(ctx: ChannelHandlerContext?, e: Throwable): Boolean {
                    e.printStackTrace()
                    return false
                }
            }
        }

        server = SocketIOServer(config).apply {

            // called when players connects to the server (after sending CONNECT event)
            addEventListener(Events.CONNECT.key, ConnectionRequest::class.java) { client, data, ackRequest ->
                logger.info("{}", data)

                if (data.gameUUID == null) {
                    if (data.gameConfig == null) {
                        throw GameException(30, "Missing game config param")
                    }

                    gamePool.createGame(data.gameConfig!!, client.sessionId.toString(), data.name)
                } else {
                    gamePool.connectToGame(
                        client.sessionId.toString(),
                        data.gameUUID.toUpperCase(),
                        data.playerUUID,
                        data.name
                    )
                }
            }

            addEventListener(Events.ACTION.key, PlayerActionRequest::class.java) { client, data, ackRequest ->
                logger.debug("{}", data)

                PlayerAction.Action.values().firstOrNull { it.key == data.action }?.let { action ->
                    gamePool.executePlayerActionOnSession(
                        client.sessionId.toString(),
                        PlayerAction(
                            action,
                            data.numericValue,
                            data.textValue
                        )
                    )

                    if (action == PlayerAction.Action.LEAVE) {
                        client.disconnect()
                    }
                }
            }

            addEventListener(Events.GAME_REQUEST.key, String::class.java) { client, data, ackRequest ->
                gamePool.requestGameState(client.sessionId.toString())
            }

            addEventListener(Events.CHAT_MESSAGE.key, ChatRequest::class.java) { client, data, ackRequest ->
                gamePool.sendChatMessage(client.sessionId.toString(), data.message, data.isFlash)
            }

            addDisconnectListener { client ->
                gamePool.playerDisconnected(client.sessionId.toString())
            }

            start()
            logger.info("socket.io server is running")
        }

        gamePool.gameDisbandedListener = this::sendGameDisbandedToPlayers
        gamePool.gameStateUpdatedListener = this::sendGameStateToPlayer
    }

    fun stop() {
        server.stop()
    }

    private fun sendGameDisbandedToPlayers(playerSessionIds: List<String>) {
        server.allClients.filter {
            it.sessionId.toString() in playerSessionIds
        }.forEach {
            it.sendEvent(Events.GAME_DISBANDED.key)
        }

    }

    private fun sendGameStateToPlayer(playerSessionId: String, gameState: GameResponse.GameState) {
        server.allClients.filter {
            it.sessionId.toString() == playerSessionId
        }.forEach {
            it.sendEvent(Events.GAME_STATE.key, gameState)
        }
    }

    /**
     * Sends message to all players in a game session
     */
//    private fun sendMessage(client: SocketIOClient, message: String, flash: Boolean) {
//        gamePool.getGroupSessions(client.sessionId.toString()).forEach { playerSession ->
//            server.allClients.filter { it.sessionId == UUID.fromString(playerSession.sessionId) }.forEach {
//                val playerData = gamePool.getGameDataForPlayerUuid(playerSession.uuid).second
//                it.sendEvent(Events.CHAT_MESSAGE.key, ChatResponse(
//                    StringEscapeUtils.escapeHtml4(playerData.name),
//                    playerData.index.toString(),
//                    System.currentTimeMillis(),
//                    StringEscapeUtils.escapeHtml4(message),
//                    flash
//                ))
//            }
//        }
//    }
}
