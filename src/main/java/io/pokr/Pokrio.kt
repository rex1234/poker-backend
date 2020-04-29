package io.pokr

import io.pokr.jobs.*
import io.pokr.network.*
import org.slf4j.*
import java.io.*

fun main() {
    val logger = LoggerFactory.getLogger("Pokrio")

    logger.info("Starting Pokrio server. Current commit: " + BuildConfig.LAST_COMMIT)

    if(!File(".env").exists()) {
        logger.error(".env file not found")
        throw Exception(".env file not found")
    }

    val gamePool = GamePool()

    SocketEngine(gamePool).start()

    WebEngine(gamePool).start()

    CronJobManager(
        ClearGamesJob(gamePool)
    ).run()
}