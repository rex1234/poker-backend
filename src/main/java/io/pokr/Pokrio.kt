package io.pokr

import io.github.cdimascio.dotenv.*
import io.pokr.network.*
import jobs.*
import java.io.*

fun main() {
    if(!File(".env").exists()) {
        System.err.println(".env file not found")
        return
    }

    println("Initializing GamePool")
    val gamePool = GamePool()

    println("Initializing sockets")
    SocketEngine(gamePool).start()

    println("Initializing Web")

    WebEngine(gamePool).start()

    println("Server deployed at " + dotenv()["WEB_URL"])

    println("Starting cron jobs")
    CronJobManager(
        ClearGamesJob(gamePool)
    ).run()
}