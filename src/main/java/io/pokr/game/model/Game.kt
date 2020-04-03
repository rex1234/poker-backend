package io.pokr.game.model

class Game private constructor(
    val uuid: String,
    val gameConfig: GameConfig,
    var gameState: State,
    val gameStart: Long = System.currentTimeMillis()
) {

    companion object {
        fun withConfig(uuid:String, config: GameConfig) =
            Game(uuid, config, State.CREATED)
    }

    enum class RoundState {
        ACTIVE,
        FINISHED
    }

    enum class State {
        CREATED,
        ACTIVE,
        FINISHED
    }

    val cardStack = CardStack.create()

    var round = 1
    var roundState = RoundState.ACTIVE
    var players = mutableListOf<Player>()
    var smallBlind = 20
    var bigBlind = 40

    var midCards: CardList = CardList() // debug TODO remove
}