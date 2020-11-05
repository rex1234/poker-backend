package io.pokr.game

import io.pokr.game.model.*

interface GameEngine {

    val updateStateListener: (HoldemTournamentGameEngine) -> Unit

    val gameFinishedListener: (HoldemTournamentGameEngine) -> Unit

    val playerKickedListener: (HoldemTournamentGameEngine, Player) -> Unit

    val gameData: GameData

    fun startGame(playerUuid: String)

    fun addPlayer(playerUUID: String)

    fun playerConnected(playerUuid: String, isConnected: Boolean)

    fun nextPlayerMove(playerUuid: String, playerAction: PlayerAction)

    fun rebuy(playerUuid: String)

    fun showCards(playerUuid: String)

    fun changeName(playerUuid: String, name: String)

    fun leave(playerUuid: String)

    fun pause(adminPlayerUuid: String, pause: Boolean)

    fun kickPlayer(adminPlayerUuid: String, kickedPlayerIndex: Int)
}