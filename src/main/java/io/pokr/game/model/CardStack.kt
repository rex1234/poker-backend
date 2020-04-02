package io.pokr.game.model

/**
 * Shuffled stack from which cards can be drawn
 */
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

    fun drawCards(n: Int = 1) = CardList(
        stack.take(n).also {
            stack = stack.drop(n)
        })
}