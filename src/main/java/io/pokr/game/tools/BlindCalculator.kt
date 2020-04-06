package io.pokr.game.tools

class BlindCalculator {

    companion object {
        val smallBlinds = listOf(10, 20, 30, 50, 75, 100, 150, 250, 400, 600, 800, 1000)

        // TODO: Implement blind calculation
        fun nextBlind(currentBlind: Int) =
            smallBlinds.firstOrNull { it > currentBlind } ?: currentBlind * 2
    }
}