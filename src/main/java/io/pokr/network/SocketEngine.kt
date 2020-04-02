package io.pokr.network

import com.corundumstudio.socketio.*
import io.pokr.game.model.Player
import io.pokr.network.model.PlayerSession
import io.pokr.network.requests.ConnectionRequest

import io.pokr.network.requests.GameRequest
import io.pokr.network.requests.PlayerActionRequest
import io.pokr.network.responses.GameResponse
import java.lang.IllegalArgumentException

import java.util.*

class SocketEngine(
    val gamePool: GamePool
) {
    val gameState = GameResponse.GameState(
        UUID.randomUUID().toString(),
        System.currentTimeMillis(),
        1,
        GameResponse.PlayerState(
            UUID.randomUUID().toString(),
            "Hadr",
            true,
            "KC QH 4D 5D 7D",
            5000,
            200
        ),
        listOf(
            GameResponse.PlayerState(
                UUID.randomUUID().toString(),
                "Rex",
                false,
                null,
                5000,
                200
            ),
            GameResponse.PlayerState(
                UUID.randomUUID().toString(),
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

    fun start() {
        val config = Configuration().apply {
            hostname = "127.0.0.1"
            port = 9092
            origin = "http://localhost:8080"
        }

        SocketIOServer(config).apply {


            addEventListener("connect", ConnectionRequest::class.java) { client, data, ackRequest ->
                if(data.gameUUID == null) {
                    gamePool.createGame(
                        PlayerSession(client.sessionId.toString(), UUID.randomUUID().toString())
                    )
                } else {
                    try {
                        gamePool.connectToGame(
                            data.gameUUID, client.sessionId.toString(), UUID.randomUUID().toString()
                        )
                    } catch (e: IllegalArgumentException) {
                        // reply error
                    }
                }
                // reply ok
            }


            addEventListener("gameRequest", PlayerActionRequest::class.java) { client, data, ackRequest ->
                gamePool.executePlayerAction(client.sessionId.toString(), data.playerAction)
            }

            addEventListener("gameState", String::class.java) { client, data, ackRequest ->
                System.err.println("Game state request")
                client.sendEvent("gameEvent", gameState)
            }

            start()
            println("socket.io server is running")
        }
    }
}