package jobs

import kotlin.concurrent.*

class CronJobManager(
    vararg val jobs: CronJob
) {
    fun run() {
        jobs.forEach {
            thread {
                it.execute()
                Thread.sleep(it.interval)
            }
        }
    }
}