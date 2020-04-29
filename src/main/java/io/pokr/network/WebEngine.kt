package io.pokr.network


import io.github.cdimascio.dotenv.*
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.*
import java.io.*
import java.security.KeyStore.*
import kotlin.concurrent.*

class WebEngine(
    val gamePool: GamePool
) {
    val logger = LoggerFactory.getLogger(WebEngine::class.java)

    fun start() {
        thread {
            embeddedServer(Netty, applicationEngineEnvironment {
                module {
                    main()
                }

                connector {
                    port = dotenv()["WEB_PORT"]!!.toInt()
                }

                val keyStoreFile = File(dotenv()["KEYSTORE_PATH"] ?: "")

                if(keyStoreFile.exists()) {
                    val keystorePw = dotenv()["KEYSTORE_PASSWORD"]!!.toCharArray()
                    val keyStoreAlias = dotenv()["KEYSTORE_ALIAS"]!!

                    val keyStore = getInstance("JKS").apply {
                        FileInputStream(keyStoreFile).use {
                            load(it, keystorePw)
                        }
                    }

                    sslConnector(
                        keyStore = keyStore,
                        keyAlias = keyStoreAlias,
                        keyStorePassword = { keystorePw },
                        privateKeyPassword = { keystorePw }) {
                        port = 443
                        keyStorePath = keyStoreFile
                    }

                    logger.info("WebEngine initialized with SSL")
                } else {
                    logger.info("WebEngine initialized without SSL")
                }
            }).start(wait = true)
        }
    }

    fun Application.main() {
        if(File(dotenv()["KEYSTORE_PATH"] ?: "").exists()) {
            install(HttpsRedirect)
        }

        install(Authentication) {
            basic(name = "admin") {
                realm = "Ktor Server"
                validate { credentials ->
                    if (credentials.name == "admin" && credentials.password == dotenv()["ADMIN_PW"]) {
                        UserIdPrincipal(credentials.name)
                    } else {
                        null
                    }
                }
            }
        }

        routing {
            route("api") {
                get("/game_state") {
                }
            }

            authenticate("admin") {
                route("admin") {
                    get("v") {
                        call.respondText("v1.0")
                    }

                    get("log") {
                        call.respondFile(File("logs/pokrio.log"))
                    }

                    get("games") {
                        call.respondText(
                            gamePool.gameSessions.map {
                                "Game Session:" + it.uuid + "<br/> Players: <ul>" +
                                        it.playerSessions.map {
                                            "<li>" + gamePool.getGameDataForPlayerUuid(it.uuid).second.name + "</li>"
                                        }.joinToString("") + "</ul>"
                            }.joinToString("<hr>"), ContentType.parse("text/html")
                        )
                    }
                }
            }

            static {
                files("web")
                file("/", "web/game.html")
            }
        }

    }
}