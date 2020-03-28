package io.pokr.game.model

data class Card (
    val color: Color,
    val value: Value
) {

    companion object {
        fun parse(code: String) =
            Card(
                Color.values().first { it.code == code[1].toString().toUpperCase() },
                Value.values().first { it.code == code[0].toString().toUpperCase() }
            )
    }

    enum class Color(
        val code: String
    ) {
        CLUBS("C"), DIAMONDS("D"), HEARTS("H"), SPADES("S")
    }

    enum class Value(
        val code: String
    ) {
        N_2("2"), N_3("3"), N_4("4"), N_5("5"), N_6("6"), N_7("7"), N_8("8"), N_9("9"), N_10("0"), JACK("J"), QUEEN("Q"), KING("K"), ACE("A")
    }

    operator fun compareTo(other: Card): Int =
        value.compareTo(other.value)

    override fun toString() =
        "${value.code}${color.code}"
}