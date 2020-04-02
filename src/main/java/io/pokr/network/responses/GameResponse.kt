package io.pokr.network.responses

import io.pokr.game.model.Game
import io.pokr.game.model.Player
import java.util.*

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
        val uuid: String?,
        val connected: Boolean,
        val name: String,
        val isOnMove: Boolean,
        val cards: String?,
        val chips: Int,
        val currentBet: Int
    )

    class GameStateFactory {

        companion object {

            fun from(game: Game, player: Player) =
                GameState(
                    uuid = game.uuid,
                    gameStart = game.gameStart,
                    round = game.round,
                    user = player.playerState(true, game),
                    players = (game.players - player).map {
                        it.playerState(false, game)
                    },
                    cards = game.midCards.toString(),
                    smallBlind = game.smallBlind,
                    bigBlind = game.bigBlind
                )

            fun Player.playerState(forSelf: Boolean, game: Game) = PlayerState(
                uuid = if (forSelf) uuid else null,
                connected = connected,
                name = name,
                isOnMove = false,
                cards = if (forSelf || game.roundState == Game.RoundState.FINISHED) cards.toString() else null,
                chips = chips,
                currentBet = currentBet
            )
        }
    }
}