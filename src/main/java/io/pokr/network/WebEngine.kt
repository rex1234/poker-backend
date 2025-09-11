package io.pokr.network

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.thymeleaf.*
import io.pokr.config.*
import org.slf4j.*
import org.thymeleaf.templateresolver.*
import org.thymeleaf.templatemode.*
import java.io.*
import kotlin.concurrent.*

class WebEngine(
    private val gamePool: GamePool,
) {

    lateinit var engine: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>

    val logger = LoggerFactory.getLogger(WebEngine::class.java)

    fun start() {
        engine = embeddedServer(Netty, port = PokrioConfig.webPort) {
            main()
        }

        logger.info("WebEngine initialized without SSL")
        logger.info("Server deployed at " + PokrioConfig.webUrl)

        thread {
            engine.start(wait = true)
        }
    }

    fun stop() {
        engine.stop(1000, 1000)
    }

    fun Application.main() {
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
            setTemplateResolver(ClassLoaderTemplateResolver().apply {
                prefix = "web/"
                suffix = ".html"
                characterEncoding = "UTF-8"
                templateMode = TemplateMode.HTML
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

            staticResources("/", PokrioConfig.webDir)

            get("/") {
                if(PokrioConfig.webUrl.contains("www.") && !call.request.host().startsWith("www.")) {
                    call.respondRedirect(PokrioConfig.webUrl)
                    return@get
                }

                call.respond(ThymeleafContent("game.html", mapOf(
                    "socketsPort" to PokrioConfig.socketsPortOutside,
                    "version" to PokrioConfig.version,
                )))
            }
        }

    }
}
