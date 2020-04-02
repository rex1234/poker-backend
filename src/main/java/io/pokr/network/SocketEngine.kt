package io.pokr.network

import com.corundumstudio.socketio.*
import io.netty.channel.ChannelHandlerContext
import io.pokr.network.requests.ConnectionRequest

import io.pokr.network.requests.PlayerActionRequest
import io.pokr.network.responses.ErrorResponse
import io.pokr.network.responses.GameResponse
import java.lang.Exception
import java.lang.IllegalArgumentException

import java.util.*

class SocketEngine(
    val gamePool: GamePool
) {

    enum class Events(
        val key: String
    ) {
        // inbound
        ACTION("action"),
        CONNECT("connectGame"),
        ERROR("error"),

        // outbound
        GAME_REQUEST("gameRequest"),
        GAME_STATE("gameState")
    }

    lateinit var server: SocketIOServer

    fun start() {
        val config = Configuration().apply {
            hostname = "127.0.0.1"
            port = 9092
            origin = "http://localhost:8080"

            exceptionListener = object: com.corundumstudio.socketio.listener.ExceptionListener {
                override fun onConnectException(e: Exception, client: SocketIOClient?) {
                    e.printStackTrace()
                }

                override fun onEventException(e: Exception, args: MutableList<Any>?, client: SocketIOClient?) {
                    e.printStackTrace()
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

            pingTimeout

        }

        server = SocketIOServer(config).apply {

            addEventListener(Events.CONNECT.key, ConnectionRequest::class.java) { client, data, ackRequest ->
                System.err.println("Connection request: " + data.name)

                try {
                    if(data.gameUUID == null) {
                        gamePool.createGame(
                            client.sessionId.toString(), data.name
                        )
                    } else {
                        gamePool.connectToGame(
                            client.sessionId.toString(), data.gameUUID, data.playerUUID, data.name
                        )
                    }

                    respondGameState(client)
                } catch (e: IllegalArgumentException) {
                    client.sendEvent(Events.ERROR.key, ErrorResponse(
                        -100, e.message.toString()
                    ))
                }
            }


            addEventListener(Events.ACTION.key, PlayerActionRequest::class.java) { client, data, ackRequest ->
                System.err.println("Action request: ")
                gamePool.executePlayerActionOnSession(client.sessionId.toString(), data.playerAction)

                respondGameState(client)
            }

            addEventListener(Events.GAME_REQUEST.key, String::class.java) { client, data, ackRequest ->
                System.err.println("Game state request")

                client.sendEvent(Events.GAME_STATE.key, gameState)
            }

            addDisconnectListener { client ->
                gamePool.playerDisconnected(client.sessionId.toString())
                respondGameState(client)
            }

            start()
            println("socket.io server is running")
        }
    }

    private fun respondGameState(client: SocketIOClient) {
        gamePool.getGroupSessions(client.sessionId.toString()).forEach { playerSession ->

            val data = gamePool.getGameDataForPlayerUuid(playerSession.uuid)

            server.allClients.filter { it.sessionId == UUID.fromString(playerSession.sessionId) }.forEach {
                it.sendEvent(Events.GAME_STATE.key, GameResponse.GameStateFactory.from(data.first, data.second))
            }
        }
    }

    // DEBUG
    val gameState = GameResponse.GameState(
        UUID.randomUUID().toString(),
        System.currentTimeMillis(),
        1,
        GameResponse.PlayerState(
            UUID.randomUUID().toString(),
            true,
            "Hadr",
            true,
            "KC QH",
            5000,
            200
        ),
        listOf(
            GameResponse.PlayerState(
                UUID.randomUUID().toString(),
                true,
                "Rex",
                false,
                null,
                5000,
                200
            ),
            GameResponse.PlayerState(
                UUID.randomUUID().toString(),
                true,
                "Gregor",
                false,
                null,
                5000,
                200
            )
        ),
        "KD KH",
        40,
        80
    )

}