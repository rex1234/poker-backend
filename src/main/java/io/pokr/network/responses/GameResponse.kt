package io.pokr.network.responses

import io.pokr.game.model.Game

class GameResponse(
    val game: GameState
) {

    class GameState(
        val uuid: String,
        val gameStart: Long,
        val round: Int,
        val user: PlayerState,
        val players: List<PlayerState>,
        val cards: String,
        val smallBlind: Int,
        val bigBlind: Int
    )

    class PlayerState(
        val uuid: String,
        val name: String,
        val isOnMove: Boolean,
        val cards: String?,
        val chips: Int,
        val currentBet: Int
    )

    class GameStateFactory {
        fun from(game: Game): GameState {
            return TODO()
        }
    }
}