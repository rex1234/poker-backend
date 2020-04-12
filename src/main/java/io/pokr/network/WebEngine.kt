package io.pokr.network


import io.github.cdimascio.dotenv.dotenv
import io.ktor.application.call
import io.ktor.http.content.file
import io.ktor.http.content.files
import io.ktor.http.content.static
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlin.concurrent.thread

class WebEngine {
    fun start() {
        thread {
            embeddedServer(Netty, dotenv()["WEB_PORT"]!!.toInt()) {
                routing {
                    route("api") {
                        get("/game_state") {
                        }

                        get("/version") {
                            call.respondText("v1.0")
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