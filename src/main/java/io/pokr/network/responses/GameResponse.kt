package io.pokr.network.responses

import io.pokr.game.model.Game
import io.pokr.game.model.Player
import java.util.*

class GameResponse(
    val game: GameState
) {

    class GameState(
        val uuid: String,
        val state: Game.State,
        val gameStart: Long?,
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
                    state = game.gameState,
                    gameStart =  if(game.gameState == Game.State.ACTIVE) game.gameStart else null,
                    round = game.round,
                    user = player.playerState(true, game),
                    players = (game.players - player).map {
                        it.playerState(false, game)
                    },
                    cards = if(game.gameState == Game.State.ACTIVE) game.cards.toString() else "",
                    smallBlind = game.smallBlind,
                    bigBlind = game.bigBlind
                )

            fun Player.playerState(forSelf: Boolean, game: Game) = PlayerState(
                uuid = if (forSelf) uuid else null,
                connected = connected,
                name = name,
                isOnMove = isOnMove,
                cards = if (forSelf || game.roundState == Game.RoundState.FINISHED || showCards) cards.toString() else null,
                chips = chips,
                currentBet = currentBet
            )
        }
    }
}