package io.pokr.game.tools

import kotlin.concurrent.thread

class GameTimer(
    val tickListener: () -> Unit
) {

    var running = false

    fun start() {
        running = true
        thread {
            while (true) {
                if(!running) {
                    return@thread
                }

                tickListener()
                Thread.sleep(100)
            }

        }
    }

    fun stop() {
        running = false
    }
}