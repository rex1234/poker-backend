package jobs

import io.pokr.network.*

class ClearGamesJob(
    val gamePool: GamePool
): CronJob(3600 * 1000) {

    override fun execute() {
        gamePool.gameSessions.filter {
            System.currentTimeMillis() - it.created > 24 * 3600 * 1000
        }.forEach {
            gamePool.discardGame(it.uuid)
        }
    }
}