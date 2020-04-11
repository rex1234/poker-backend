package io.pokr.network.util

class TokenGenerator {

    companion object {
        fun nextPlayerToken(length: Int = 64): String {
            val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz1234567890"
            return (1..length)
                .map { allowedChars.random() }
                .joinToString("")
        }

        fun nextGameToken(length: Int = 10): String {
            val allowedChars = "ABCDEFGHJKLMNPQRSTUVWXTZ23456789"
            return (1..length)
                .map { allowedChars.random() }
                .joinToString("")
        }
    }
}