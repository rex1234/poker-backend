package io.pokr.game.model

/**
 * Shuffled stack from which cards can be drawn
 */
class CardStack(
    var stack: List<Card>, // TODO: use CardList instead
) {

    companion object {
        fun create() = CardStack(
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