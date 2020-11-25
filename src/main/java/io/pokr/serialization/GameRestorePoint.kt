package io.pokr.serialization

import io.pokr.game.model.*

data class GameRestorePoint(
    val uuid: String,
    val gameConfig: GameConfig,
    val gameState: GameData.State,
    val allPlayers: List<Player>,
    val targetBet: Int,
    val lastFullRaiseDiff: Int,
    val previousStreetTargetBet: Int,
    val minRaiseTo: Int,
    val smallBlind: Int,
    val bigBlind: Int,
    val nextSmallBlind: Int,
    val nextBlindsChangeAt: Long,
    val round: Int,
    val gameStart: Long,
    val roundState: GameData.RoundState,
    val pauseStart: Long,
    val totalPauseTime: Long,
) {

   companion object {
       fun fromGameData(gameData: GameData) =
           GameRestorePoint(
               uuid = gameData.uuid,
               gameConfig = gameData.config,
               gameState = GameData.State.PAUSED,
               allPlayers = gameData.allPlayers.map { it.copy() },
               targetBet = gameData.targetBet,
               lastFullRaiseDiff = gameData.lastFullRaiseDiff,
               previousStreetTargetBet = gameData.previousStreetTargetBet,
               minRaiseTo = gameData.minRaiseTo,
               smallBlind = gameData.smallBlind,
               bigBlind = gameData.bigBlind,
               nextSmallBlind = gameData.nextSmallBlind,
               nextBlindsChangeAt = gameData.nextBlindsChangeAt,
               round = gameData.round,
               gameStart = gameData.gameStart,
               roundState = gameData.roundState,
               pauseStart = gameData.pauseStart ?: System.currentTimeMillis(),
               totalPauseTime = gameData.totalPauseTime,
           )
   }

    fun toGameData() =
        GameData(uuid).also {
            it.config = gameConfig
            it.gameState = gameState
            it.allPlayers.addAll(allPlayers)
            it.targetBet = targetBet
            it.lastFullRaiseDiff = lastFullRaiseDiff
            it.previousStreetTargetBet = previousStreetTargetBet
            it.minRaiseTo = minRaiseTo
            it.smallBlind = smallBlind
            it.bigBlind = bigBlind
            it.nextSmallBlind = nextSmallBlind
            it.nextBlindsChangeAt = nextBlindsChangeAt
            it.round = round
            it.gameStart = gameStart
            it.roundState = roundState
            it.pauseStart = pauseStart
            it.totalPauseTime = totalPauseTime
        }
}