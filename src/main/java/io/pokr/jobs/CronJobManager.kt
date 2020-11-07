package io.pokr.jobs

import org.slf4j.*
import kotlin.concurrent.*

class CronJobManager(
    vararg val jobs: CronJob,
) {

    private val logger = LoggerFactory.getLogger(CronJobManager::class.java)

    fun run() {
        logger.info("Initializing CRON jobs")

        jobs.forEach {
            thread {
                while (true) {
                    logger.info("Executing job " + it.javaClass.simpleName)
                    it.execute()

                    Thread.sleep(it.interval)
                }
            }
        }
    }
}