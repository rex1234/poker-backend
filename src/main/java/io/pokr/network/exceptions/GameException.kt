package io.pokr.network.exceptions

import java.lang.Exception

class GameException(
    val code: Int,
    message: String
): Exception(message)