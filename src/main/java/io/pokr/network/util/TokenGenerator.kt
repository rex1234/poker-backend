package io.pokr.network.util

class TokenGenerator {

    companion object {
        fun nextPlayerToken(length: Int = 64): String {
            val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz123456789"
            return (1..length)
                .map { allowedChars.random() }
                .joinToString("")
        }

        fun nextGameToken(length: Int = 10): String {
            val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXTZ123456789"
            return "12345"
            return (1..length)
                .map { allowedChars.random() }
                .joinToString("")
        }
    }
}