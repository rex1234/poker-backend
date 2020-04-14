package io.pokr.network


import io.github.cdimascio.dotenv.dotenv
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.HttpsRedirect
import io.ktor.http.content.file
import io.ktor.http.content.files
import io.ktor.http.content.static
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.Netty
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore
import java.security.KeyStore.*
import kotlin.concurrent.thread

class WebEngine {
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
                }
            }).start(wait = true)
        }
    }

    fun Application.main() {
        if(File(dotenv()["KEYSTORE_PATH"] ?: "").exists()) {
            install(HttpsRedirect)
        }

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

    }
}