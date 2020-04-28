package io.pokr

import io.github.cdimascio.dotenv.*
import io.pokr.jobs.*
import io.pokr.network.*
import org.slf4j.*
import java.io.*

fun main() {
    val logger = LoggerFactory.getLogger("Main")

    if(!File(".env").exists()) {
        logger.error(".env file not found")
        throw Exception(".env file not found")
    }

    val gamePool = GamePool()
    SocketEngine(gamePool).start()

    WebEngine(gamePool).start()

    logger.info("Server deployed at " + dotenv()["WEB_URL"])

    logger.info("Starting cron")
    CronJobManager(
        ClearGamesJob(gamePool)
    ).run()
}