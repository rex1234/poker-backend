package io.pokr.network.responses

import io.pokr.game.model.*
import org.apache.commons.text.*

class GameResponse(
    val game: GameState,
) {

    class GameState(
        val time: Long,
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
        val bestCards: String?,
        val nextBlinds: Long,
        val smallBlind: Int,
        val bigBlind: Int,
        val targetBet: Int,
        var previousTargetBet: Int,
        val isLateRegistrationEnabled: Boolean,
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
        val bestCards: String?,
        val chips: Int,
        val currentBet: Int,
        val hasLeft: Boolean,
        val isRebuyNextRound: Boolean,
        val rebuyCount: Int,
        val lastWin: Int,
        val isWinner: Boolean,
        val finalRank: Int,
    )

    class GameStateFactory {

        companion object {

            fun from(
                game: GameData,
                currentPlayerUuid: String,
            ) = GameState(
                time = System.currentTimeMillis(),
                uuid = game.uuid,
                config = game.config,
                state = game.gameState.toString().toLowerCase(),
                gameStart = if (game.gameState == GameData.State.ACTIVE) game.gameStart else null,
                round = game.round,
                roundState = game.roundState.toString().toLowerCase(),
                user = game.allPlayers.first {
                    it.uuid == currentPlayerUuid
                }.playerState(true, game),
                cards = if (game.gameState == GameData.State.ACTIVE) game.tableCards.toString() else "",
                bestCards = game.bestCards?.toString(),
                pot = game.allPlayers.sumBy { it.currentBet },
                smallBlind = game.smallBlind,
                bigBlind = game.bigBlind,
                nextBlinds = game.nextBlinds,
                players = game.allPlayers.filter {
                    it.uuid != currentPlayerUuid
                }.map { it.playerState(false, game) },
                targetBet = game.targetBet,
                previousTargetBet = game.previousTargetBet,
                isLateRegistrationEnabled = game.isLateRegistrationEnabled
            )

            fun Player.playerState(forSelf: Boolean, game: GameData) = PlayerState(
                uuid = if (forSelf) uuid else null,
                index = index,
                isConnected = isConnected,
                isAdmin = isAdmin,
                name = StringEscapeUtils.escapeHtml4(name),
                isDealer = isDealer,
                isOnMove = isOnMove,
                hasLeft = isKicked || isLeaveNextRound,
                isRebuyNextRound = isRebuyNextRound,
                rebuyCount = rebuyCount,
                moveStart = moveStart,
                action = action.toString().toLowerCase(),
                cards = if (forSelf || (game.roundState == GameData.RoundState.FINISHED && showCards)) cards.toString() else null,
                hand = if (forSelf || (game.roundState == GameData.RoundState.FINISHED && showCards)) hand?.handName else null,
                bestCards = if (forSelf || (game.roundState == GameData.RoundState.FINISHED && showCards)) bestCards?.toString() else null,
                chips = chips,
                currentBet = currentBet,
                lastWin = lastWin,
                isWinner = isWinner,
                finalRank = finalRank
            )
        }
    }
}