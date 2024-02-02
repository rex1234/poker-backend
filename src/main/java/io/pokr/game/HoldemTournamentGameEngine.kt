package io.pokr.game

import io.pokr.game.exceptions.*
import io.pokr.game.model.*
import io.pokr.game.tools.*
import io.pokr.serialization.*
import org.slf4j.*
import kotlin.concurrent.*
import kotlin.math.*

class HoldemTournamentGameEngine(
    gameUuid: String,
    gameConfig: GameConfig,
    initialGameData: GameData? = null,
    private val updateStateListener: (HoldemTournamentGameEngine) -> Unit,
    private val gameFinishedListener: (HoldemTournamentGameEngine) -> Unit,
    private val playerKickedListener: (HoldemTournamentGameEngine, Player) -> Unit,
    private val restorePointCreatedListener: (HoldemTournamentGameEngine) -> Unit,
) {

    val gameData: GameData

    // game data stored after each round
    var gameRestorePoint: GameRestorePoint? = null

    init {
        gameData = initialGameData ?: GameData(gameUuid)

        gameData.config = gameConfig
    }

    private val handComparator = HandComparator()

    private val gameTimer = GameTimer {
        gameTick()
    }

    private val logger = LoggerFactory.getLogger(HoldemTournamentGameEngine::class.java)

    // extra time that is added after round finishes so that the animations can be performed
    private var extraRoundTime = 0L

    // whether we the timer at the round's end is running
    private var isRoundEndThreadRunning = false

    val gameUuid
        get() = gameData.uuid


    fun addPlayer(playerUUID: String, playerName: String) {
        if (gameData.allPlayers.firstOrNull { it.uuid == playerUUID } != null) {
            throw GameException(22, "Cannot rejoin a game you have left or been kicked from")
        }

        if (gameData.allPlayers.size == 9) {
            throw GameException(10, "The game is already full")
        }

        if (gameData.gameState != GameData.State.CREATED && !gameData.isLateRegistrationPossible) {
            throw GameException(11, "Late registration is not possible")
        }

        val player = Player(playerUUID, playerName).apply {
            // first connected player is an admin
            if (gameData.allPlayers.isEmpty()) {
                isAdmin = true
            }

            // if we connect to an active game we set a player's rebuy flag to true and he will be added next round
            if (gameData.gameState == GameData.State.ACTIVE || gameData.gameState == GameData.State.PAUSED) {
                isFinished = true
                isRebuyNextRound = true
                connectedToRound = gameData.round
            }
        }

        gameData.addPlayer(player)
    }

    private fun setDealerAndBlindPositions() {
        if (gameData.round == 1) {
            val randomDealer = gameData.allPlayers.shuffled().first()

            // by Texas HoldEm rules, if there are just two players, the dealer is also SB
            val smallBlind = if (gameData.allPlayers.size == 2) randomDealer else gameData.nextActivePlayerFrom(randomDealer)

            randomDealer.isDealer = true
            smallBlind.isSmallBlind = true
            gameData.nextActivePlayerFrom(smallBlind).isBigBlind = true

            return
        }

        val previousDealer = gameData.currentDealer
        val previousSmallBlind = gameData.currentSmallBlindPlayer
        val previousBigBlind = gameData.currentBigBlindPlayer

        val doesButtonGoBackwards = previousDealer.isFinished && previousSmallBlind != null && previousSmallBlind.isFinished
        val doesButtonStay = !previousDealer.isFinished && (previousSmallBlind == null || previousSmallBlind.isFinished)

        val newDealer = when {
            gameData.players.size == 2 -> gameData.nextActivePlayerFrom(previousDealer)
            doesButtonGoBackwards -> gameData.previousActivePlayerFrom(previousDealer)
            doesButtonStay -> previousDealer
            else -> gameData.nextActivePlayerFrom(previousDealer)
        }
        val newSmallBlind = when {
            gameData.players.size == 2 -> newDealer
            previousBigBlind.isFinished -> null // only big blind in the next round
            else -> gameData.nextActivePlayerFrom(newDealer)
        }
        val newBigBlind = when {
            gameData.players.size == 2 || previousBigBlind.isFinished -> gameData.nextActivePlayerFrom(newDealer, true)
            else -> gameData.nextActivePlayerFrom(newSmallBlind!!, true)
        }

        newDealer.isDealer = true
        if (newSmallBlind != null) {
            newSmallBlind.isSmallBlind = true
        }
        newBigBlind.isBigBlind = true

        if (previousDealer != newDealer) {
            previousDealer.isDealer = false
        }
        if (previousSmallBlind != null && previousSmallBlind != newSmallBlind) {
            previousSmallBlind.isSmallBlind = false
        }
        if (previousBigBlind != newBigBlind) {
            previousBigBlind.isBigBlind = false
        }
    }

    fun startGame(playerUuid: String) {
        // only admin can start a game
        applyOnAdminPlayer(playerUuid) {}

        if (gameData.gameState != GameData.State.CREATED) {
            throw GameException(17, "The game has already started")
        }

        if (gameData.allPlayers.size == 1) {
            throw GameException(12, "Cannot start a game with only 1 player")
        }

        logger.info("Game ${gameData.uuid} started")

        // set initial game state
        gameData.gameState = GameData.State.ACTIVE
        gameData.gameStart = System.currentTimeMillis()

        gameData.smallBlind = gameData.config.startingBlinds
        gameData.nextSmallBlind = BlindCalculator.nextBlind(gameData.smallBlind)
        gameData.bigBlind = gameData.smallBlind * 2
        gameData.nextBlindsChangeAt = gameData.gameStart + gameData.config.blindIncreaseTime * 1000

        // add chips to the players
        gameData.allPlayers.forEach {
            it.chips = gameData.config.startingChips
        }

        startNewRound()

        gameTimer.start()
    }

    private fun startNewRound() {
        if (gameData.gameState == GameData.State.PAUSED) {
            return
        }

        // init the round, set target bet to big blind, set players' blinds and draw cards
        increaseBlinds()
        gameData.apply {
            round++
            cardStack = CardStack.create()
            targetBet = gameData.bigBlind
            minRaiseTo = 2 * gameData.bigBlind
            lastFullRaiseDiff = 0
            previousStreetTargetBet = 0
            roundState = GameData.RoundState.ACTIVE
            bestCards = null
            tableCards = CardList()
        }

        // reset players' states
        gameData.allPlayers.forEach {
            it.cards = CardList()
            it.bestCards = null
            it.showCards = false
            it.isOnMove = false
            it.hand = null
            it.action = PlayerAction.Action.NONE
            it.pendingAction = true
            it.lastWin = 0
            it.currentBet = 0
            it.canRaise = true
            it.isWinner = false
            it.chipsAtStartOfTheRound = it.chips
        }

        if (gameData.activePlayers.size <= 2) {
            rebuyPlayers()
            setDealerAndBlindPositions()
        } else {
            setDealerAndBlindPositions()
            rebuyPlayers()
        }

        val currentSmallBlindPlayer = gameData.currentSmallBlindPlayer

        currentSmallBlindPlayer?.postBlind(gameData.smallBlind)
        if (gameData.activePlayers.size == 2 && currentSmallBlindPlayer != null && currentSmallBlindPlayer.isAllIn) {
            // in just 2 players, the blind made SB go all in -> BB bets whatever SB bet and we go straight to showdown
            gameData.currentBigBlindPlayer.apply {
                postBlind(currentSmallBlindPlayer.currentBet)
                pendingAction = false
            }
        } else {
            gameData.currentBigBlindPlayer.postBlind(gameData.bigBlind)
        }

        // draw cards for each non-finished player
        gameData.players.forEach {
            it.cards = gameData.cardStack.drawCards(2)
        }

        gameData.nextActivePlayerFrom(gameData.currentBigBlindPlayer).startMove()

        // check players that are all in after cards are dealt (so we can showdown automatically)
        gameData.players.filter { it.isAllIn }.forEach {
            it.action = PlayerAction.Action.CHECK
        }

        tryShowdown()
    }

    fun nextPlayerMove(playerUuid: String, playerAction: PlayerAction) {
        if (gameData.roundState != GameData.RoundState.ACTIVE) {
            throw GameException(18, "The round is not in an active state", "State: ${gameData.roundState}")
        }

        val player = gameData.currentPlayerOnMove

        if (player.uuid != playerUuid) {
            throw GameException(16, "It is not your turn", "It is the turn of ${player.name}")
        }

        val passedMove = when (playerAction.action) {
            PlayerAction.Action.CALL -> {
                // call to the target bet or go all in
                player.currentBet = min(gameData.targetBet, player.chips)
                true
            }

            // we can check only if we match the target bet
            PlayerAction.Action.CHECK ->
                player.currentBet == gameData.targetBet || player.isAllIn

            // raise and reset other players' actions
            PlayerAction.Action.RAISE -> {
                val raiseAmount = playerAction.numericValue!!
                val totalPlayerBet = player.currentBet + raiseAmount

                val isFullRaise = totalPlayerBet >= gameData.minRaiseTo
                val isAllIn = totalPlayerBet == player.chips

                if ((isFullRaise || isAllIn) && player.chips >= totalPlayerBet) {
                    if (isFullRaise) {
                        // e.g. a player raised from 370 to 625, the next min. raise is to 880 (2 * 625 - 370)
                        gameData.apply {
                            minRaiseTo = 2 * totalPlayerBet - gameData.targetBet
                            lastFullRaiseDiff = totalPlayerBet - gameData.targetBet
                        }

                        // a full raise has been made, all players can raise again
                        gameData.activePlayers.forEach {
                            it.canRaise = true
                        }
                    } else {
                        // e.g. with a min. raise to 570, a player went all in from 370 to 490 (not a full raise),
                        // the next min. raise is to 690 (570 + 490 - 370)
                        gameData.minRaiseTo = gameData.minRaiseTo + totalPlayerBet - gameData.targetBet

                        (gameData.activePlayers - player).forEach {
                            /*
                            current player went all in but it was not a full raise - the other active players
                            will not be allowed to raise unless:
                                a) they were already allowed to raise, or
                                b) their raise was followed by at least one player's all in together with current
                                player's all in and the all in's together added up to a full raise, e.g.
                                BB is 100, UTG raises to 300, UTG+1 goes all in for 400 (not a full raise - UTG
                                can't raise again), current player goes all in for 550 (not a full raise either
                                but together with UTG+1's, they make up for a full raise and UTG can raise again)
                                (550 - 300 > 300 - 100)
                            */
                            it.canRaise = it.canRaise || totalPlayerBet - it.currentBet >= gameData.lastFullRaiseDiff
                        }
                    }

                    player.currentBet = totalPlayerBet
                    gameData.targetBet = totalPlayerBet

                    // we make the other active players go again on this street
                    (gameData.activePlayers - player).forEach {
                        it.pendingAction = true
                    }

                    true
                } else {
                    false
                }
            }

            // just fold
            PlayerAction.Action.FOLD -> true

            else -> false
        }

        // if the action was a success we finish this player's action
        if (passedMove) {
            player.action = playerAction.action
            player.pendingAction = false
            player.canRaise = false

            // all but one folded
            if (gameData.players.count { it.action != PlayerAction.Action.FOLD } == 1) {
                finishRound()
                return
            }

            // S H O W D O W N bitch
            if (gameData.tableCards.cards.size < 5) {
                if (tryShowdown()) {
                    return
                }
            }

            // everyone passed an action we will go to the next street
            if (gameData.players.none { it.pendingAction == true }) {

                // 5 cards already, we will finish the round
                if (gameData.tableCards.cards.size == 5) {
                    finishRound()
                } else {
                    // or we will draw a card and reset actions
                    drawCards()

                    gameData.apply {
                        previousStreetTargetBet = targetBet
                        minRaiseTo = targetBet + bigBlind
                    }

                    // non folded players will start with a NONE action next street
                    gameData.activePlayers.forEach {
                        it.apply {
                            action = PlayerAction.Action.NONE
                            canRaise = true
                            pendingAction = true
                        }
                    }

                    //calculate hands
                    gameData.players.forEach {
                        it.hand = handComparator.findHighestHand(it.cards, gameData.tableCards)
                    }

                    // after cards are drawn, the next player from a dealer is on move
                    gameData.currentPlayerOnMove.isOnMove = false
                    gameData.nextActivePlayerFrom(gameData.currentDealer).startMove()
                }
            } else {
                // go to the next player
                nextPlayer()
            }
        } else {
            throw GameException(8, "Action '${playerAction.action.key}' could not be performed")
        }
    }


    private fun tryShowdown(): Boolean {
        val pendingActionPlayersCount = gameData.players.count { it.pendingAction }
        val allInPlayersCount = gameData.players.count { it.isAllIn }
        val foldedPlayersCount = gameData.players.count { it.action == PlayerAction.Action.FOLD }

        val lastPendingActionPlayer = if (pendingActionPlayersCount == 1)
            gameData.players.firstOrNull { it.pendingAction } else null
        val highestAllInBet = gameData.players.filter { it.isAllIn }.map { it.currentBet }.maxOrNull()

        val canShowdown =
            gameData.players.size - allInPlayersCount - foldedPlayersCount <= 1 && (
                pendingActionPlayersCount == 0 ||
                gameData.players.all { it.isAllIn } || (
                    // it would still be one last player's turn but their current bet is
                    // already higher than any other all in -> we can go to showdown
                    lastPendingActionPlayer != null &&
                    highestAllInBet != null &&
                    lastPendingActionPlayer.currentBet >= highestAllInBet
                )
            )

        if (canShowdown) {
            logger.debug("Showdown")

            // add extra round time for showdown card animation
            extraRoundTime = when (gameData.tableCards.size) {
                0 -> 4200L
                3 -> 2700L
                else -> 1700L
            }

            gameData.tableCards =
                gameData.tableCards.with(gameData.cardStack.drawCards(5 - gameData.tableCards.cards.size))
            gameData.players.filter { it.action != PlayerAction.Action.FOLD }.forEach { it.showCards = true }

            finishRound()

            return true
        } else {
            return false
        }
    }

    private fun finishRound() {
        calculateWinnings()

        // calculate hand strengths
        if (gameData.tableCards.cards.size >= 3) {
            gameData.players.forEach {
                it.hand = handComparator.findHighestHand(it.cards, gameData.tableCards)
            }
        }

        // show cards of non folded players (if there is more than 1)
        if (gameData.players.count { it.action != PlayerAction.Action.FOLD } > 1) {
            gameData.players.filter { it.action != PlayerAction.Action.FOLD }.forEach { it.showCards = true }
        }

        gameData.allPlayers.forEach {
            if (it.isLeaveNextRound) {
                it.isLeaveNextRound = false
                it.chips = 0
                it.isAdmin = false

                if (it.isAdmin && gameData.players.isNotEmpty()) {
                    gameData.players.first().isAdmin = true
                }
            }
        }

        // switch to the FINISHED state, no actions can be performed anymore and the results of the round are shown
        gameData.roundState = GameData.RoundState.FINISHED

        thread {
            // we will wait some time and start a new round
            // there can be bug when a player unpauses before this time limit (and startNewRound will be called 2x)
            // isRoundEndThreadRunning prevents this

            isRoundEndThreadRunning = true
            // recap timer - the more cards there are, the longer we will show the recap
            Thread.sleep(2_000 + min(3_000 + max((gameData.players.count { it.showCards } - 1), 0) * 1500L,
                6000L) + extraRoundTime)
            extraRoundTime = 0L

            calculateFinalRanks()

            gameRestorePoint = GameRestorePoint.fromGameData(gameData)
            restorePointCreatedListener(this)

            // if there is only one player with chips we will finish the game
            if (gameData.allPlayers.count { it.chips > 0 || it.isRebuyNextRound } == 1) {
                gameData.allPlayers.first { it.chips > 0 }.finalRank = 1
                finishGame()
            } else {
                startNewRound()
                updateStateListener(this)

            }
            isRoundEndThreadRunning = false
        }
    }

    /**
     * Calculates rank of players that won't play the next round (either left or finished with 0 chips)
     * Sorting criteria are:
     *  1. players that finished the game without leaving with more chips at the beginning of the round
     *  2. players that left with more chips at the end of the round
     *  3. players that left with more chips at the beginning of the round
     */
    fun calculateFinalRanks() {
        val previouslyFinishedPlayerCount = gameData.allPlayers.count { it.finalRank != 0 }
        val finishingPlayers = gameData.allPlayers.filter {
            it.finalRank == 0 && (it.chips == 0 || it.isLeaveNextRound)
        }
        val nonFinishingPlayerCount = gameData.allPlayers.size - finishingPlayers.size - previouslyFinishedPlayerCount

        finishingPlayers.sortedWith(
            compareBy({ it.isLeaveNextRound }, { -it.chips }, { -it.chipsAtStartOfTheRound })
        ).forEachIndexed { i, player ->
            player.finalRank = nonFinishingPlayerCount + i + 1
            player.isFinished = true
        }
    }

    private fun calculateWinnings() {
        // calculate winnings before round ended (all folded)
        // we just take the pot and put it to non-folded player's chips
        if (gameData.players.count { it.action != PlayerAction.Action.FOLD } == 1) {
            val winner = gameData.players.first { it.action != PlayerAction.Action.FOLD }
            val pot = gameData.players.sumBy { it.currentBet }
            gameData.players.forEach {
                it.chips -= it.currentBet
            }
            winner.lastWin = pot
            winner.chips += pot
        } else {
            // calculate winning after regular round
            // TODO: put PlayerHandComparisonResult directly to Player (we need it there anyway)
            val ranks = handComparator.evalPlayers(gameData.players, gameData.tableCards)
            ranks.forEach { it.player.bestCards = it.bestCards!!.first }
            gameData.bestCards = ranks[0].bestCards!!.second

            ranks.filter { it.rank == ranks[0].rank }.forEach { it.player.isWinner = true }

            WinningsCalculator.calculateWinnings(ranks)
        }
    }

    fun finishGame() {
        logger.info(
            "Game ${gameData.uuid} finished. {}",
            gameData.allPlayers.sortedBy { it.finalRank }.joinToString(" ") {
                "[${it.finalRank} - ${it.name} (${it.rebuyCount})]"
            })

        gameTimer.stop()

        gameData.gameState = GameData.State.FINISHED
        updateStateListener(this)
        gameFinishedListener(this)
    }

    private fun gameTick() {
        if (gameData.gameState == GameData.State.PAUSED) {
            return
        }

        checkCurrentPlayerMoveTimeLimit()
    }

    private fun drawCards() {
        if (gameData.tableCards.cards.size == 0) {
            gameData.tableCards = gameData.tableCards.with(gameData.cardStack.drawCards(3))
        } else if (gameData.tableCards.cards.size < 5) {
            gameData.tableCards = gameData.tableCards.with(gameData.cardStack.drawCards(1))
        }
    }

    // switches move to the next player
    private fun nextPlayer() {
        val playerOnMove = gameData.currentPlayerOnMove
        val nextPlayerOnMove = gameData.nextPlayerOnMove

        playerOnMove.isOnMove = false
        nextPlayerOnMove.startMove()
    }

    private fun increaseBlinds() {
        if (System.currentTimeMillis() > gameData.nextBlindsChangeAt) {
            val sb = BlindCalculator.nextBlind(gameData.smallBlind)
            val nextSb = BlindCalculator.nextBlind(sb)
            gameData.apply {
                nextBlindsChangeAt = System.currentTimeMillis() + config.blindIncreaseTime * 1000
                smallBlind = sb
                nextSmallBlind = nextSb
                bigBlind = smallBlind * 2

                //updateStateListener(this@HoldemTournamentGameEngine)
            }
        }
    }

    // automatically executes default action on player when he did not play in the time limit
    private fun checkCurrentPlayerMoveTimeLimit() {
        // we want to check player's time limits only when he is on move and the round is in active state
        if (gameData.roundState != GameData.RoundState.ACTIVE) {
            return
        }

        val currentPlayerOnMove = gameData.currentPlayerOnMove

        // if player has not played in his time limit, we will perform the default action on him
        if (System.currentTimeMillis() - currentPlayerOnMove.moveStart > gameData.config.playerMoveTime * 1000) {
            nextPlayerMove(
                currentPlayerOnMove.uuid,
                // if he can check he will check, otherwise he will fold
                if (currentPlayerOnMove.currentBet == gameData.targetBet)
                    PlayerAction(PlayerAction.Action.CHECK, null, null)
                else {
                    PlayerAction(PlayerAction.Action.FOLD, null, null)
                }
            )
            updateStateListener(this)
        }
    }

    /**
     * Determines whether a player can join the game in the next round.
     *
     * If a player joins the game or rebuys in a position between the current dealer and BB,
     * they have to wait another round.
     *
     * Important: the function assumes that the dealer, small blind and big blind
     * have already been assigned for the round.
     */
    fun canPlayerJoinNextRound(player: Player): Boolean {
        if (gameData.players.size <= 2 || player.isBigBlind) {
            return true
        }

        val playerPosition = player.index
        val dealerPosition = gameData.allPlayers.first { it.isDealer }.index
        val bbPosition = gameData.allPlayers.first { it.isBigBlind }.index

        if (bbPosition > dealerPosition && playerPosition > dealerPosition && playerPosition < bbPosition) {
            return false
        }
        if (bbPosition < dealerPosition && (playerPosition > dealerPosition || playerPosition < bbPosition)) {
            return false
        }

        return true
    }

    fun rebuyPlayers() {
        gameData.allPlayers.filter { it.isRebuyNextRound }.forEach { player ->
            if (canPlayerJoinNextRound(player)) {
                // adjust ranks of other players
                gameData.allPlayers.filter { it.finalRank != 0 && it.finalRank < player.finalRank }.forEach {
                    it.finalRank++
                }

                player.isFinished = false
                player.isRebuyNextRound = false
                player.finalRank = 0
                player.chips = gameData.config.startingChips
            }
        }
    }

    fun rebuy(playerUuid: String) =
        applyOnPlayer(playerUuid) {
            if (!gameData.isLateRegistrationPossible || gameData.config.maxRebuys == 0 || it.isKicked) {
                throw GameException(14, "Rebuy is not possible")
            }

            if (it.rebuyCount >= gameData.config.maxRebuys) {
                throw GameException(14, "Max rebuy count reached")
            }

            if (it.isFinished) {
                it.rebuyCount++
                it.finalRank = 0
                it.isRebuyNextRound = true
            } else {
                throw GameException(19, "Cannot rebuy, you have not lost yet")
            }
        }

    fun showCards(playerUuid: String) =
        applyOnPlayer(playerUuid) {
            it.showCards = true
        }

    fun changeName(playerUuid: String, name: String) =
        applyOnPlayer(playerUuid) {
            if (!InputValidator.validatePlayerName(name)) {
                throw GameException(7, "Invalid name", "Name: $name")
            }
            it.name = name.trim()
        }

    fun playerConnected(playerUuid: String, isConnected: Boolean) =
        applyOnPlayer(playerUuid) {
            it.isConnected = isConnected
        }

    fun leave(playerUuid: String) =
        applyOnPlayer(playerUuid) { kickedPlayer ->
            kickedPlayer.isLeaveNextRound = true
            kickedPlayer.isKicked = true

            playerKickedListener(this, kickedPlayer)

            if (gameData.gameState == GameData.State.CREATED) {
                if (gameData.allPlayers.size == 1) {
                    gameFinishedListener(this)
                } else {
                    gameData.allPlayers.remove(kickedPlayer)
                    gameData.allPlayers.first().isAdmin = true
                    updateStateListener(this)
                }
            } else {
                if (kickedPlayer.isOnMove) {
                    nextPlayerMove(kickedPlayer.uuid, PlayerAction(PlayerAction.Action.FOLD))
                } else {
                    kickedPlayer.action = PlayerAction.Action.FOLD
                }
            }

            logger.info("Player ${kickedPlayer.name} has been kicked or has left")
        }

    fun pause(playerUuid: String, pause: Boolean) =
        applyOnAdminPlayer(playerUuid) {
            if (pause) {
                if (gameData.gameState == GameData.State.ACTIVE && gameData.roundState == GameData.RoundState.FINISHED) {
                    gameData.pause(true)
                } else {
                    throw GameException(15, "The game can be paused only when a round is finished")
                }
            } else {
                if (gameData.gameState == GameData.State.PAUSED) {
                    if (!isRoundEndThreadRunning) {
                        gameData.pause(false)
                        startNewRound()
                    }
                }
            }
        }

    fun kickPlayer(playerUuid: String, playerIndex: Int) =
        applyOnAdminPlayer(playerUuid) {
            gameData.allPlayers.firstOrNull { it.index == playerIndex }?.let { player ->
                leave(player.uuid)
            }
        }

    private fun applyOnPlayer(uuid: String, action: (Player) -> Unit) =
        gameData.allPlayers.firstOrNull { it.uuid == uuid }?.let {
            action(it)
        } ?: throw GameException(13, "Invalid player UUID", "UUID: $uuid")

    private fun applyOnAdminPlayer(playerUuid: String, action: (Player) -> Unit) =
        applyOnPlayer(playerUuid) {
            if (!it.isAdmin) {
                throw GameException(5, "Only an admin can perform this action")
            }

            action(it)
        }
}