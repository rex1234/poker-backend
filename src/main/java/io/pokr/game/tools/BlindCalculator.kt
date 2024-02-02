package io.pokr.game.tools

object BlindCalculator {

    private val smallBlinds = listOf(1, 2, 3, 5, 10, 20, 30, 50, 75, 100, 150, 250, 400, 600, 800, 1000, 1500, 2000)

    fun nextBlind(currentBlind: Int) =
        smallBlinds.firstOrNull { it > currentBlind } ?: (currentBlind * 2)

}
