package io.pokr.game.exceptions

class GameException(
    val code: Int,
    message: String,
    val additionalInfo: String = "",
) : Exception(message)
