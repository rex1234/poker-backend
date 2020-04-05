package io.pokr

import io.pokr.network.WebEngine
import io.pokr.network.GamePool
import io.pokr.network.SocketEngine
import kotlin.concurrent.thread

fun main() {
    println("Initializing GamePool")
    val gamePool = GamePool()

    println("Initializing sockets")
    SocketEngine(gamePool).start()

    println("Initializing Web")

    WebEngine().start()

    println("Server deployed at http://localhost:8080/")
}