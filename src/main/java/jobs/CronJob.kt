package jobs

abstract class CronJob(
    val interval: Long
) {
    abstract fun execute()
}