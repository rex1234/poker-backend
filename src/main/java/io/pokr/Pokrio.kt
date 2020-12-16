package io.pokr

import io.pokr.config.*
import io.pokr.database.*
import io.pokr.jobs.*
import io.pokr.network.*
import org.slf4j.*
import sun.misc.*
import kotlin.system.*

lateinit var socketEngine: SocketEngine
lateinit var webEngine: WebEngine

fun main() {
    val logger = LoggerFactory.getLogger("Pokrio")

    logger.info("Starting Pokrio server. Current commit: " + PokrioConfig.version)

    if (!PokrioConfig.exists()) {
        logger.error(".env file not found")
        throw Exception(".env file not found")
    }

    val gamePool = GamePool()

    socketEngine = SocketEngine(gamePool)
    socketEngine.start()

    webEngine = WebEngine(gamePool)
    webEngine.start()

    CronJobManager(
        ClearGamesJob(gamePool)
    ).run()

    DatabaseManager().let {
        it.init()
        it.insertGame()
    }

    Signal.handle(Signal("INT")) {
        handleExit()
    }

    Signal.handle(Signal("TERM")) {
        handleExit()
    }
}

fun handleExit() {
    socketEngine.stop()
    webEngine.stop()
    exitProcess(0)
}
