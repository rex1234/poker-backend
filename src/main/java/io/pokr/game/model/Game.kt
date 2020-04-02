package io.pokr.game.model

class Game private constructor(
    val gameConfig: GameConfig,
    var gameState: State
) {

    companion object {
        fun withConfig(config: GameConfig) =
            Game(config, State.CREATED)
    }

    enum class State {
        CREATED,
        ACTIVE,
        FINISHED
    }

    val cardStack = CardStack.create()

    var round = 1
    var players = mutableListOf<Player>()

    lateinit var midCards: CardList
}