package io.pokr.jobs

import io.pokr.network.*

class ClearGamesJob(
    val gamePool: GamePool
): CronJob(3600 * 1000) {

    override fun execute() {
        gamePool.gameSessions.filter {
            val age = System.currentTimeMillis() - it.created

            age > 24 * 3600 * 1000 || (it.playerSessions.size == 1 && age > 3590 * 1000)
        }.forEach {
            gamePool.discardGame(it.uuid)
        }
    }
}