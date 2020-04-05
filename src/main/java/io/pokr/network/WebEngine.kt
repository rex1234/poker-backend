package io.pokr.network

import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.file
import io.ktor.http.content.files
import io.ktor.http.content.static
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.pokr.network.responses.GameResponse
import java.util.*
import kotlin.concurrent.thread

class WebEngine {
    fun start() {
        thread {
            embeddedServer(Netty, 8080) {
                routing {
                    route("api") {
                        get("/game_state") {
                        }
                    }

                    static {
                        files("web")
                        file("/", "web/game.html")
                    }
                }
            }.start(wait = true)
        }
    }
}