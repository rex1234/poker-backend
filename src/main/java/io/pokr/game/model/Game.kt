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
    var smallBlind = 20
    var bigBlind = 40

    var round = 0
    var gameStart: Long = 0
    var roundState = RoundState.ACTIVE
    lateinit var cardStack: CardStack
    lateinit var cards: CardList

    val activePlayers
        get() = players.filter { !it.finished }

    val playerOnMove
        get() = players.first { it.isOnMove }

    val nextPlayer
        get() = (activePlayers + activePlayers).drop(activePlayers.indexOf(playerOnMove) + 1).first()
}