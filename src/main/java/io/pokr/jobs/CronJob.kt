package io.pokr.jobs

abstract class CronJob(
    val interval: Long
) {
    abstract fun execute()
}