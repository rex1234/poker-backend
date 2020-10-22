package io.pokr.game.model

/**
 * Shuffled stack from which cards can be drawn
 */
class CardStack (
    var stack: List<Card>
) {

    companion object {
        fun create(): CardStack = CardStack(
            Card.Color.values().flatMap { color ->
                Card.Value.values().map { value -> Card(color, value) }
            }.shuffled().toMutableList()
        )
    }

    fun drawCards(n: Int = 1) = CardList(
        stack.take(n).also {
            stack = stack.drop(n)
        })
}