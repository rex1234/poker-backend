package io.pokr

import io.pokr.jobs.*
import io.pokr.network.*
import org.slf4j.*
import sun.misc.*
import java.io.*
import kotlin.system.*

lateinit var socketEngine: SocketEngine
lateinit var webEngine: WebEngine

fun main() {
    val logger = LoggerFactory.getLogger("Pokrio")

    logger.info("Starting Pokrio server. Current commit: " + BuildConfig.LAST_COMMIT)

    if (!File(".env").exists()) {
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
