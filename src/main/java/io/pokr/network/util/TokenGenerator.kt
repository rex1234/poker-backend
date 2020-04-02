package io.pokr.network.util

class TokenGenerator {

    companion object {
        fun nextToken(length: Int = 64): String {
            val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz123456789"
            return (1..length)
                .map { allowedChars.random() }
                .joinToString("")
        }
    }
}