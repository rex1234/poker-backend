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

    thread {
        WebEngine().start()
    }

    println("Create game at http://localhost:8080/socket_create_game.html")
    println("Connect to game at http://localhost:8080/socket_connect_game.html")
}