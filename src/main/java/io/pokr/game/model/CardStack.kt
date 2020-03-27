package io.pokr.game.model

class CardStack private constructor(
    var stack: List<Card>
) {

    companion object {
        fun create(): CardStack = CardStack(
            Card.Color.values().map { color ->
                Card.Value.values()
                    .map { value -> Card(color, value) }
            }.flatten().shuffled().toMutableList()
        )
    }

    fun takeCards(n: Int = 1) = CardList(
        stack.take(n).also {
            stack = stack.drop(n)
        })
}