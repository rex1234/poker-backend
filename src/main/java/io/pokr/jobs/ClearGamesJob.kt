package io.pokr.jobs

import io.pokr.network.*
import org.slf4j.*

class ClearGamesJob(
    val gamePool: GamePool
): CronJob(3600 * 1000) {

    val logger = LoggerFactory.getLogger(ClearGamesJob::class.java)

    override fun execute() {
        gamePool.gameSessions.filter {
            val age = System.currentTimeMillis() - it.created

            age > 24 * 3600 * 1000 || (it.playerSessions.size == 1 && age > 3590 * 1000)
        }.forEach {
            logger.info("Discarding game session " + it.uuid)

            gamePool.discardGame(it.uuid)
        }
    }
}