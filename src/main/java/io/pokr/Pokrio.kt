package io.pokr

import io.pokr.network.WebEngine
import io.pokr.network.GamePool
import io.pokr.network.SocketEngine

fun main() {
    println("Initializing GamePool")
    val gamePool = GamePool()

    println("Initializing sockets")
    SocketEngine(gamePool).start()

    println("Initializing Web")
    WebEngine().start()
}