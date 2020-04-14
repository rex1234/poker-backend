package io.pokr

import io.pokr.network.*
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

    println("Server deployed at http://localhost:8080/")
}