package io.pokr.game

import io.pokr.game.exceptions.*
import io.pokr.game.model.*
import io.pokr.game.tools.*
import org.slf4j.*
import kotlin.concurrent.*
import kotlin.math.*

class HoldemTournamentGameEngine(
    gameUuid: String,
    gameConfig: GameConfig,
    val updateStateListener: (HoldemTournamentGameEngine) -> Unit,
    val gameFinishedListener: (HoldemTournamentGameEngine) -> Unit,
    val playerKickedListener: (HoldemTournamentGameEngine, Player) -> Unit,
) {

    private val handComparator = HandComparator()

    private val gameTimer = GameTimer {
        gameTick()
    }

    private val logger = LoggerFactory.getLogger(HoldemTournamentGameEngine::class.java)

    // extra time that is added after round finishes so that the animations can be performed
    private var extraRoundTime = 0L

    // whether we the timer at the round's end is running
    private var isRoundEndThreadRunning = false

    val gameData: GameData

    init {
        gameData = GameData(gameUuid)
        gameData.config = gameConfig
    }

    fun addPlayer(playerUUID: String) {
        if (gameData.allPlayers.size == 9) {
            throw GameException(10, "The game is already full")
        }

        if (gameData.gameState != GameData.State.CREATED && !gameData.isLateRegistrationEnabled) {
            throw GameException(11, "Late registration is not available")
        }

        gameData.allPlayers.add(Player(playerUUID).apply {
            // assign random table index for a player
            index = ((1..9).toList() - gameData.allPlayers.map { it.index }).shuffled().first()

            // first connected player is an admin
            if (gameData.allPlayers.isEmpty()) {
                isAdmin = true
            }

            // if we connect to an active game we set a player's rebuy flag to true and he will be added next round
            if (gameData.gameState == GameData.State.ACTIVE) {
                isFinished = true
                isRebuyNextRound = true
            }
        })
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

        gameData.allPlayers.sortBy { it.index }

        gameData.allPlayers.shuffled().first().isDealer = true // chose random dealer

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

        // we will add chips to players that rebought
        gameData.allPlayers.filter { it.isRebuyNextRound }.forEach { player ->

            // we have to increase ranks of players that are not playing anymore and were in front of the rebought player
            gameData.allPlayers.filter { it.finalRank != 0 && it.finalRank < player.finalRank }.forEach {
                it.finalRank++
            }

            player.isFinished = false
            player.isRebuyNextRound = false
            player.finalRank = 0
            player.chips = gameData.config.startingChips
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
        }

        // draw cards for each non-finished player
        gameData.activePlayers.forEach {
            it.cards = gameData.cardStack.drawCards(2)
        }

        // move dealer to the next position
        nextDealer()

        // assign blinds and players on move
        (gameData.players + gameData.players).apply {
            // we have to merge arrays so the order can be "cyclic"

            // special case if there are only 2 players
            // dealer is SB and is on move, the other player is BB
            if (gameData.activePlayers.size == 2) {
                gameData.currentDealer.apply {
                    currentBet = min(chips, gameData.smallBlind)
                    startMove()
                }

                get(indexOf(gameData.currentDealer) + 1).apply {
                    currentBet = min(chips, gameData.bigBlind)
                }
            } else {
                // otherwise, the next player to the dealer is SB
                get(indexOf(gameData.currentDealer) + 1).apply {
                    currentBet = min(chips, gameData.smallBlind)
                }

                // the next one is BB
                get(indexOf(gameData.currentDealer) + 2).apply {
                    currentBet = min(chips, gameData.bigBlind)
                }

                // and the next one is on move
                get(indexOf(gameData.currentDealer) + 3).apply {
                    startMove()
                }
            }
        }

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
        // ((if one player is not all in AND all the rest is all in) OR everyone is all in)
        // AND all other players are folded
        if (gameData.players.size
            - gameData.players.count { it.isAllIn }
            - gameData.players.count { it.action == PlayerAction.Action.FOLD } <= 1 &&
            (gameData.players.none { it.pendingAction } || gameData.players.all { it.isAllIn })
        ) {

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
        gameData.players.map { it.chipsAtStartOfTheRound = it.chips }

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

        calculateFinalRanks()

        // switch to the FINISHED state, no actions can be performed anymore and the results of the round are shown
        gameData.roundState = GameData.RoundState.FINISHED

        thread {
            // otherwise we will wait some time and start a new round
            // there can be bug when a player unpauses before this time limit (and startNewRound will be called 2x)
            // isRoundEndThreadRunning prevents this

            isRoundEndThreadRunning = true
            // recap timer - the more cards there are, the longer we will show the recap
            Thread.sleep(2_000 + min(3_000 + max((gameData.players.count { it.showCards } - 1), 0) * 1500L,
                6000L) + extraRoundTime)
            extraRoundTime = 0L

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

        val nonFinishingPlayerCount = gameData.allPlayers.count {
            it.finalRank == 0 && it.chips > 0 && !it.isLeaveNextRound
        }

        gameData.allPlayers.filter {
            it.finalRank == 0 && (it.chips == 0 || it.isLeaveNextRound)
        }.sortedWith(
            compareBy({ it.isLeaveNextRound }, { -it.chips }, { -it.chipsAtStartOfTheRound })
        ).forEachIndexed { i, player ->
            player.finalRank = nonFinishingPlayerCount + i + 1
            player.isFinished = true

            if(player.isLeaveNextRound) {
                player.isLeaveNextRound = false
                player.chips = 0
                player.isAdmin = false

                if (player.isAdmin && gameData.players.isNotEmpty()) {
                    gameData.players.first().isAdmin = true
                }
            }
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
                it.currentBet = 0
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

    // moves dealer token to the next player
    fun nextDealer() {
        val currentDealer = gameData.currentDealer
        val nextDealer = gameData.nextDealer

        currentDealer.isDealer = false
        nextDealer.isDealer = true
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

    fun rebuy(playerUuid: String) =
        applyOnPlayer(playerUuid) {
            if (!gameData.isLateRegistrationEnabled || it.isKicked) {
                throw GameException(11, "Rebuy is not possible")
            }

            if (it.rebuyCount >= gameData.config.maxRebuys) {
                throw GameException(11, "Max rebuy count exceeded")
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
            if (name.length > 10 || name.trim().isEmpty()) {
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