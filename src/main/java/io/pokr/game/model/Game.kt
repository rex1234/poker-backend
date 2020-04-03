package io.pokr.game.model

class Game private constructor(
    val uuid: String,
    val gameConfig: GameConfig,
    var gameState: State
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

    var players = mutableListOf<Player>()
    var targetBet = 0
    var smallBlind = 20
    var bigBlind = 40

    var round = 0
    var gameStart: Long = 0
    var roundState = RoundState.ACTIVE

    lateinit var cardStack: CardStack
    lateinit var tableCards: CardList

    val activePlayers
        get() = players.filter { !it.isFinished }

    val currentDealer
        get() = players.first { it.isDealer }

    val nextDealer
        get() = (activePlayers + activePlayers).run { get(indexOf(currentDealer) + 1) }

    val currentPlayerOnMove
        get() = players.first { it.isOnMove }

    val nextPlayerOnMove
        get() = (activePlayers + activePlayers).run { get(indexOf(currentPlayerOnMove) + 1) }
}