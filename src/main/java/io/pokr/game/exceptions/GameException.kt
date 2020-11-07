package io.pokr.game.exceptions

class GameException(
    val code: Int,
    message: String,
) : Exception(message)