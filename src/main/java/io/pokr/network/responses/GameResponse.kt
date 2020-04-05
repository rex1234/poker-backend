package io.pokr.network.responses

import io.pokr.game.model.Game
import io.pokr.game.model.Player

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
        val pot: Int?,
        val cards: String,
        val winningCards: String?,
        val nextBlinds: Long,
        val smallBlind: Int,
        val bigBlind: Int
    )

    class PlayerState(
        val uuid: String?,
        val index: Int,
        val connected: Boolean,
        val name: String,
        val isDealer: Boolean,
        val isOnMove: Boolean,
        val moveStart: Long,
        val action: String,
        val cards: String?,
        val hand: String?,
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
                    cards = if(game.gameState == Game.State.ACTIVE) game.tableCards.toString() else "",
                    winningCards = game.winningCards?.toString(),
                    pot = game.players.sumBy { it.currentBet },
                    smallBlind = game.smallBlind,
                    bigBlind = game.bigBlind,
                    nextBlinds = game.nextBlinds,
                    players = (game.players - player).map {
                        it.playerState(false, game)
                    }
                )

            fun Player.playerState(forSelf: Boolean, game: Game) = PlayerState(
                uuid = if (forSelf) uuid else null,
                index = index,
                connected = isConnected,
                name = name,
                isDealer = isDealer,
                isOnMove = isOnMove,
                moveStart = moveStart,
                action = action.toString().toLowerCase(),
                cards = if (forSelf || (game.roundState == Game.RoundState.FINISHED && showCards)) cards.toString() else null,
                hand = if (forSelf || (game.roundState == Game.RoundState.FINISHED && showCards)) hand?.handName else null,
                chips = chips,
                currentBet = currentBet
            )
        }
    }
}