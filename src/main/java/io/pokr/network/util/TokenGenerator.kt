package io.pokr.network.util

object TokenGenerator {

    @Deprecated("Token generation moved to FE")
    fun nextPlayerToken(length: Int = 64): String {
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz1234567890"
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    fun nextGameToken(length: Int = 6): String {
        val allowedChars = "ABCDEFGHJKLMNPQRSTUVWXTZ23456789"
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }
}