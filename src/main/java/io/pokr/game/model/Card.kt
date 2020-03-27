package io.pokr.game.model

data class Card (
    val color: Color,
    val value: Value
) {

    enum class Color {
        CLUBS, DIAMONDS, HEARTS, SPADES
    }

    enum class Value {
        N_2, N_3, N_4, N_5, N_6, N_7, N_8, N_9, N_10, JACK, QUEEN, KING, ACE
    }

    operator fun compareTo(other: Card): Int =
        value.compareTo(other.value)
}