package io.pokr.network

import com.corundumstudio.socketio.Configuration
import com.corundumstudio.socketio.SocketIOClient
import com.corundumstudio.socketio.SocketIOServer
import io.github.cdimascio.dotenv.dotenv
import io.netty.channel.ChannelHandlerContext
import io.pokr.game.exceptions.GameException
import io.pokr.game.model.PlayerAction
import io.pokr.network.requests.ChatRequest
import io.pokr.network.requests.ConnectionRequest
import io.pokr.network.requests.PlayerActionRequest
import io.pokr.network.responses.ChatResponse
import io.pokr.network.responses.ErrorResponse
import io.pokr.network.responses.GameResponse
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.*

/**
 * Class handling socket.io communication
 */
class SocketEngine(
    val gamePool: GamePool
) {

    enum class Events(
        val key: String
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
        REACT("react"),
        CHAT_MESSAGE("chat")
    }

    lateinit var server: SocketIOServer

    fun start() {
        val config = Configuration().apply {
            hostname = "0.0.0.0"
            port = dotenv()["SOCKETS_PORT"]!!.toInt()
            origin = dotenv()["WEB_URL"]!!

            val keyStoreFile = File(dotenv()["KEYSTORE_PATH"])
            if(keyStoreFile.exists()) {
                keyStore = FileInputStream(keyStoreFile)
                keyStoreFormat = "jks"
                keyStorePassword = dotenv()["KEYSTORE_PASSWORD"]!!
            }

            exceptionListener = object: com.corundumstudio.socketio.listener.ExceptionListener {
                override fun onConnectException(e: Exception, client: SocketIOClient?) {
                    e.printStackTrace()
                }

                override fun onEventException(e: Exception, args: MutableList<Any>?, client: SocketIOClient?) {
                    if(e is GameException) {
                        client?.sendEvent(Events.ERROR.key, ErrorResponse(
                            e.code, e.message ?: ""
                        ))
                    } else {
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
                System.err.println("Connection request: " + data.name)
                if(data.gameUUID == null) {
                    if(data.gameConfig == null) {
                        throw GameException(30, "Missing game config param")
                    }

                    gamePool.createGame(data.gameConfig!!, client.sessionId.toString(), data.name                        )
                } else {
                    gamePool.connectToGame(client.sessionId.toString(), data.gameUUID, data.playerUUID, data.name)
                }

                sendGameState(client)
            }

            addEventListener(Events.ACTION.key, PlayerActionRequest::class.java) { client, data, ackRequest ->
                gamePool.executePlayerActionOnSession(
                    client.sessionId.toString(),
                    PlayerAction(
                        PlayerAction.Action.values().first { it.key == data.action },
                        data.numericValue,
                        data.textValue
                    )
                )

                sendGameState(client)
            }

            addEventListener(Events.GAME_REQUEST.key, String::class.java) { client, data, ackRequest ->
                System.err.println("Game state request")

                sendGameState(client)
            }

            addEventListener(Events.CHAT_MESSAGE.key, ChatRequest::class.java) { client, data, ackRequest ->
                System.err.println("Game state request")

                sendMessage(client, data.message, data.flash)
            }


            addDisconnectListener { client ->
                gamePool.playerDisconnected(client.sessionId.toString())
                sendGameState(client)
            }

            start()
            println("socket.io server is running")
        }

        gamePool.gameDisbandedListener = { sessions ->
            server.allClients.filter { it.sessionId.toString() in sessions.map { it.sessionId } }.forEach {
                it.sendEvent(Events.GAME_DISBANDED.key)
            }
        }

        // TODO: merge with sendGameState
        gamePool.updateStateListener = { sessions ->
            server.allClients.filter { it.sessionId.toString() in sessions.map { it.sessionId } }.firstOrNull()?.also {
                sendGameState(it)
            }
        }
    }

    /**
     * Sends respective game states to all players in a game session
     */
    private fun sendGameState(client: SocketIOClient) {
        gamePool.getGroupSessions(client.sessionId.toString()).forEach { playerSession ->
            server.allClients.filter { it.sessionId == UUID.fromString(playerSession.sessionId) }.forEach {
                val data = gamePool.getGameDataForPlayerUuid(playerSession.uuid)
                it.sendEvent(Events.GAME_STATE.key, GameResponse.GameStateFactory.from(data.first, data.second))
            }
        }
    }

    /**
     * Sends message to all players in a game session
     */
    private fun sendMessage(client: SocketIOClient, message: String, flash: Boolean) {
        gamePool.getGroupSessions(client.sessionId.toString()).forEach { playerSession ->
            server.allClients.filter { it.sessionId == UUID.fromString(playerSession.sessionId) }.forEach {
                val playerData = gamePool.getGameDataForPlayerUuid(playerSession.uuid).second
                it.sendEvent(Events.CHAT_MESSAGE.key, ChatResponse(
                    playerData.name,
                    playerData.index.toString(),
                    System.currentTimeMillis(),
                    message,
                    flash
                ))
            }
        }
    }
}