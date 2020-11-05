package io.pokr.network

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.thymeleaf.*
import io.pokr.chat.*
import io.pokr.config.*
import org.slf4j.*
import org.thymeleaf.templateresolver.*
import java.io.*
import java.security.KeyStore.*
import kotlin.concurrent.*

class WebEngine(
    private val gamePool: GamePool,
) {

    lateinit var engine: NettyApplicationEngine

    val logger = LoggerFactory.getLogger(WebEngine::class.java)

    fun start() {
        engine = embeddedServer(Netty, applicationEngineEnvironment {
            module {
                main()
            }

            connector {
                port = PokrioConfig.webPort
            }

            val keyStoreFile = File(PokrioConfig.keyStorePath ?: "")

            if (keyStoreFile.exists()) {
                val keystorePw = PokrioConfig.keyStorePassword.toCharArray()
                val keyStoreAlias = PokrioConfig.keyStoreAlias

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
            logger.info("Server deployed at " + PokrioConfig.webUrl)
        })

        thread {
            engine.start(wait = true)
        }
    }

    fun stop() {
        engine.stop(1000, 1000)
    }

    fun Application.main() {
        if (File(PokrioConfig.keyStorePath ?: "").exists()) {
            install(HttpsRedirect)
        }

        install(Authentication) {
            basic(name = "admin") {
                realm = "Ktor Server"
                validate { credentials ->
                    if (credentials.name == "admin" && credentials.password == PokrioConfig.adminPassword) {
                        UserIdPrincipal(credentials.name)
                    } else {
                        null
                    }
                }
            }
        }

        install(Thymeleaf) {
            setTemplateResolver(FileTemplateResolver().apply {
                prefix = "${PokrioConfig.webDir}/"
                suffix = ".html"
            })
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
                }
            }

            get("/") {
                if(PokrioConfig.webUrl.contains("www.") && !call.request.host().startsWith("www.")) {
                    call.respondRedirect(PokrioConfig.webUrl)
                    return@get
                }
                
                call.respond(ThymeleafContent("game.html", mapOf(
                    "socketsPort" to PokrioConfig.socketsPort,
                    "version" to PokrioConfig.version,
                    "reactions" to ChatEngine.validReactions,
                )))
            }

            static {
                files(PokrioConfig.webDir)
            }
        }

    }
}
