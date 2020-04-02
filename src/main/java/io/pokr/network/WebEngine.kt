package io.pokr.network

import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.files
import io.ktor.http.content.static
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.pokr.network.responses.GameResponse
import java.util.*

class WebEngine {
    fun start() {
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


        embeddedServer(Netty, 8080) {
            routing {
                route("api") {
                    get("/game_state") {
                        call.respondText(Gson().toJson(gameState), ContentType.Text.Plain)
                    }
                }

                static {
                    files("web")
                }
            }
        }.start(wait = true)
    }
}