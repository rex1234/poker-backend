package io.pokr.network.responses

import io.pokr.game.model.Game
import io.pokr.game.model.GameConfig
import io.pokr.game.model.Player

class GameResponse(
    val game: GameState
) {

    class GameState(
        val uuid: String,
        val config: GameConfig,
        val state: String,
        val gameStart: Long?,
        val roundState: String,
        val round: Int,
        val user: PlayerState,
        val players: List<PlayerState>,
        val pot: Int?,
        val cards: String,
        val winningCards: String?,
        val nextBlinds: Long,
        val smallBlind: Int,
        val bigBlind: Int,
        val targetBet: Int,
        var previousTargetBet: Int
    )

    class PlayerState(
        val uuid: String?,
        val index: Int,
        val isConnected: Boolean,
        val name: String,
        val isAdmin: Boolean,
        val isDealer: Boolean,
        val isOnMove: Boolean,
        val moveStart: Long,
        val action: String,
        val cards: String?,
        val hand: String?,
        val chips: Int,
        val currentBet: Int,
        val rebuyCount: Int
    )

    class GameStateFactory {

        companion object {

            fun from(game: Game, player: Player) =
                GameState(
                    uuid = game.uuid,
                    config = game.config,
                    state = game.gameState.toString().toLowerCase(),
                    gameStart =  if(game.gameState == Game.State.ACTIVE) game.gameStart else null,
                    round = game.round,
                    roundState = game.roundState.toString().toLowerCase(),
                    user = player.playerState(true, game),
                    cards = if(game.gameState == Game.State.ACTIVE) game.tableCards.toString() else "",
                    winningCards = game.winningCards?.toString(),
                    pot = game.allPlayers.sumBy { it.currentBet },
                    smallBlind = game.smallBlind,
                    bigBlind = game.bigBlind,
                    nextBlinds = game.nextBlinds,
                    players = (game.allPlayers - player).map {
                        it.playerState(false, game)
                    },
                    targetBet = game.targetBet,
                    previousTargetBet = game.previousTargetBet
                )

            fun Player.playerState(forSelf: Boolean, game: Game) = PlayerState(
                uuid = if (forSelf) uuid else null,
                index = index,
                isConnected = isConnected,
                isAdmin = isAdmin,
                name = name,
                isDealer = isDealer,
                isOnMove = isOnMove,
                rebuyCount = rebuyCount,
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