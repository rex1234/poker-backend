package io.pokr.game

import io.pokr.game.exceptions.*
import io.pokr.game.model.*
import io.pokr.game.tools.*
import kotlin.concurrent.*
import kotlin.math.*

class HoldemTournamentGameEngine(
    gameUuid: String,
    val updateStateListener: (HoldemTournamentGameEngine) -> Unit,
    val gameFinishedListener: (HoldemTournamentGameEngine) -> Unit
) {
    private val handComparator = HandComparator()

    val game = Game(gameUuid)

    var mockCardStack: CardStack? = null

    private val gameTimer = GameTimer {
        gameTick()
    }

    // extra time that is added after round finishes so that the animations can be performed
    private var extraRoundTime = 0L
    // whether we the timer at the round's end is running
    private var isRoundEndThreadRunning = false

    fun initGame(config: GameConfig) {
        game.config = config
    }

    fun addPlayer(playerUUID: String) {
        if(game.allPlayers.size == 9) {
            throw GameException(10, "Game is already full")
        }

        if(game.gameState != Game.State.CREATED && !game.isLateRegistrationEnabled) {
            throw GameException(11, "Late registration is not possible")
        }

        game.allPlayers.add(Player(playerUUID).apply {
            // assign random table index for a player
            if(mockCardStack == null) {
                index = ((1..9).toList() - game.allPlayers.map { it.index }).shuffled().first()
            } else {
                index = game.allPlayers.size
            }

            // first connected player is an admin
            if(game.allPlayers.isEmpty()) {
                isAdmin = true
            }

            // if we connect to an active game we set a player's rebuy flag to true and he will be added next round
            if(game.gameState == Game.State.ACTIVE) {
                isFinished = true
                isRebuyNextRound = true
            }
        })
    }

    fun startGame(playerUuid: String) {
        // only admin can start a game
        applyOnAdminPlayer(playerUuid) {}

        if(game.gameState != Game.State.CREATED) {
            throw GameException(17, "Game already started")
        }

        if(game.allPlayers.size == 1) {
            throw GameException(12, "Cannot start a game with only 1 player")
        }

        System.err.println("Game ${game.uuid} started")

        // set initial game state
        game.gameState = Game.State.ACTIVE
        game.gameStart = System.currentTimeMillis()

        game.smallBlind = game.config.startingBlinds
        game.bigBlind = game.smallBlind * 2
        game.nextBlinds = game.gameStart + game.config.blindIncreaseTime * 1000

        game.allPlayers.sortBy { it.index }

        if(mockCardStack == null) {
            game.allPlayers.shuffled().first().isDealer = true // chose random dealer
        } else {
            game.allPlayers.first().isDealer = true
        }

        // add chips to the players
        game.allPlayers.forEach {
            it.chips = game.config.startingChips
        }

        startNewRound()

        gameTimer.start()
    }

    private fun startNewRound() {
        if(game.gameState == Game.State.PAUSED) {
            return
        }

        // init the round, set target bet to big blind, set players' blinds and draw cards
        increaseBlinds()
        game.apply {
            round++
            cardStack = if(mockCardStack != null) mockCardStack!! else CardStack.create()
            targetBet = game.bigBlind
            previousTargetBet = 0
            roundState = Game.RoundState.ACTIVE
            bestCards = null
            tableCards = CardList()
        }

        // we will discard players that have left
        game.allPlayers.filter { it.isLeaveNextRound }.forEach {
            it.finalRank = game.players.size

            it.isFinished = true
            it.isLeaveNextRound = false
            it.chips = 0
            it.isAdmin = false

            if(it.isAdmin && game.players.isNotEmpty()) {
                game.players.first().isAdmin = true
            }
        }

        // we will add chips to players that rebought
        game.allPlayers.filter { it.isRebuyNextRound }.forEach {
            it.isFinished = false
            it.isRebuyNextRound = false
            it.chips = game.config.startingChips
        }

        // reset players' states
        game.allPlayers.forEach {
            it.cards = CardList()
            it.bestCards = null
            it.showCards = false
            it.isOnMove = false
            it.hand = null
            it.action = PlayerAction.Action.NONE
            it.lastWin = 0
            it.currentBet = 0
            it.isWinner = false
        }

        // draw cards for each non-finished player
        game.activePlayers.forEach {
            it.cards = game.cardStack.drawCards(2)
        }

        // move dealer to the next position
        nextDealer()

        // assign blinds and players on move
        (game.players + game.players).apply { // we have to merge arrays so the order can be "cyclic"

            // special case if there are only 2 players
            // dealer is SB and is on move, the other player is BB
            if(game.activePlayers.size == 2) {
                game.currentDealer.apply {
                    currentBet = min(chips, game.smallBlind)
                    startMove()
                }

                get(indexOf(game.currentDealer) + 1).apply {
                    currentBet = min(chips, game.bigBlind)
                }
            } else {
                // otherwise, the next player to the dealer is SB
                get(indexOf(game.currentDealer) + 1).apply {
                    currentBet = min(chips, game.smallBlind)
                }

                // the next one BB
                get(indexOf(game.currentDealer) + 2).apply {
                    currentBet = min(chips, game.bigBlind)
                }

                // and the next one is on move
                get(indexOf(game.currentDealer) + 3).apply {
                    startMove()
                }
            }
        }
    }

    fun nextPlayerMove(playerUuid: String, playerAction: PlayerAction) {
        if(game.roundState != Game.RoundState.ACTIVE) {
            throw GameException(18, "Round is not in an active state")
        }

        val player = game.currentPlayerOnMove

        if(player.uuid != playerUuid) {
            throw GameException(16, "Player is not on move")
        }

        val passedMove = when(playerAction.action) {
            PlayerAction.Action.CALL -> {
                // call to the target bet or go all in
                player.currentBet = min(game.targetBet, player.chips)
                true
            }

            // we can check only if we match the target bet
            PlayerAction.Action.CHECK ->
                player.currentBet == game.targetBet

            // raise and reset other players' actions
            PlayerAction.Action.RAISE -> {
                val raiseAmount = playerAction.numericValue!!
                // if we can raise or if we raise to an all in
                if (raiseAmount >= game.smallBlind * 2 || raiseAmount + player.currentBet == player.chips) {// TODO: implement raise rules
                    if (player.chips - player.currentBet >= raiseAmount) {
                        game.targetBet = player.currentBet + raiseAmount
                        player.currentBet = game.targetBet

                        // we reset other player's action so they have to go again on this street
                        (game.activePlayers - player).forEach {
                            it.action = PlayerAction.Action.NONE
                        }

                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            }

            // just fold
            PlayerAction.Action.FOLD -> true

            else -> false
        }

        // if the action was a success we finish this player's action
        if(passedMove) {
            player.action = playerAction.action

            // all but one folded
            if(game.players.count { it.action != PlayerAction.Action.FOLD } == 1) {
                finishRound()
                return
            }

            // S H O W D O W N bitch
            if(game.tableCards.cards.size < 5) {
                // ((if one player is not all in AND all the rest is all in) OR everyone is all in)
                // AND all other players are folded
                if(game.players.none { it.action == PlayerAction.Action.NONE } &&
                    (game.players.size - game.players.count { it.isAllIn } - game.players.count { it.action == PlayerAction.Action.FOLD } <= 1)) {
                    showdown()
                    return
                }
            }

            // everyone passed an action we will go to the next street
            if(game.players.none { it.action == PlayerAction.Action.NONE }) {

                // 5 cards already, we will finish the round
                if(game.tableCards.cards.size == 5) {
                    finishRound()
                } else {
                    // or we will draw a card and reset actions
                    drawCards()

                    // this is for implementing raise rules
                    game.previousTargetBet = game.targetBet

                    // non folded players will start with a NONE action next street
                    game.activePlayers.forEach {
                        it.action = PlayerAction.Action.NONE
                    }

                    //calculate hands
                    game.players.forEach {
                        it.hand = handComparator.findHighestHand(it.cards, game.tableCards)
                    }

                    // after cards are drawn, the next player from a dealer is on move
                    game.currentPlayerOnMove.isOnMove = false
                    game.nextActivePlayerFrom(game.currentDealer).startMove()
                }
            } else {
                // go to the next player
                nextPlayer()
            }
        } else {
            throw GameException(8, "Action '${playerAction.action.key}' could not be performed")
        }
    }


    private fun showdown() {
        System.err.println("Showdown")

        extraRoundTime = when(game.tableCards.size) {
            0 -> 4200L
            3 -> 2700L
            else -> 1700L
        }

        game.tableCards = game.tableCards.with(game.cardStack.drawCards( 5 - game.tableCards.cards.size))
        game.players.filter { it.action != PlayerAction.Action.FOLD }.forEach { it.showCards = true }

        finishRound()
    }

    private fun finishRound() {
        calculateWinnings()

        // calculate hands
        if(game.tableCards.cards.size >= 3) {
            game.players.forEach {
                it.hand = handComparator.findHighestHand(it.cards, game.tableCards)
            }
        }

        // show cards of non folded players (if there is more than 1)
        if(game.players.count { it.action != PlayerAction.Action.FOLD } > 1) {
            game.players.filter { it.action != PlayerAction.Action.FOLD }.forEach { it.showCards = true }
        }

        // if a player has 0 chips, he is finished and won't play anymore (unless he rebuys)
        game.allPlayers.filter { it.chips == 0 }.forEach {
            it.isFinished = true
        }

        // set player's final rank (if it was not already set before)
        game.allPlayers.filter { it.finalRank == 0 && it.chips == 0 }.forEach {
            it.finalRank = game.players.size + 1

            // TODO: player with more chips at the round end should have better final rank
        }

        // switch to the FINISHED state, no actions and be performed anymore and the results of the round are shown
        game.roundState = Game.RoundState.FINISHED

        thread {
            // otherwise we will wait some time and start a new round
            // there can be bug when a player unpauses before this time limit (and startNewRound will be called 2x)
            // isRoundEndThreadRunning prevents this

            isRoundEndThreadRunning = true
            // recap timer - the more cards there are, the longer we will show the recap
            Thread.sleep(min(3_000 + max((game.players.count { it.showCards } - 1), 0) * 1500L, 6000L) + extraRoundTime)
            extraRoundTime = 0L

            // if there is only one player with chips we will finish the game
            if (game.allPlayers.count { it.chips > 0 } == 1) {
                game.allPlayers.first { it.chips > 0 }.finalRank = 1
                finishGame()
            } else {
                startNewRound()
                updateStateListener(this)

            }
            isRoundEndThreadRunning = false
        }
    }

    private fun calculateWinnings() {
        // calculate winnings before round ended (all folded)
        // we just take the pot and put it to non-folded player's chips
        if(game.allPlayers.count { !it.isFinished && it.action != PlayerAction.Action.FOLD } == 1) {
            val winner = game.allPlayers.first { it.action != PlayerAction.Action.FOLD }
            val pot = game.allPlayers.sumBy { it.currentBet }
            game.allPlayers.forEach {
                it.chips -= it.currentBet
                it.currentBet = 0
            }
            winner.lastWin = pot
            winner.chips += pot
        } else {
            // calculate winning after regular round
            // TODO: put PlayerHandComparisonResult directly to Player (we need it there anyway)
            val ranks = handComparator.evalPlayers(game.players, game.tableCards)
            ranks.forEach { it.player.bestCards = it.bestCards!!.first }
            game.bestCards = ranks[0].bestCards!!.second

            ranks.filter { it.rank == ranks[0].rank }.map { it.player }.forEach { it.isWinner = true }

            WinningsCalculator.calculateWinnings(ranks)
        }
    }

    fun finishGame() {
        System.err.println("Game ${game.uuid} finished")

        gameTimer.stop()

        game.gameState = Game.State.FINISHED
        updateStateListener(this)
        gameFinishedListener(this)
    }

    private fun gameTick() {
        if(game.gameState == Game.State.PAUSED) {
            return
        }

        checkCurrentPlayerMoveTimeLimit()
    }

    private fun drawCards() {
        if(game.tableCards.cards.size == 0) {
            game.tableCards = game.tableCards.with(game.cardStack.drawCards(3))
        } else if(game.tableCards.cards.size < 5) {
            game.tableCards = game.tableCards.with(game.cardStack.drawCards(1))
        }
    }

    private fun nextPlayer() {
        val playerOnMove = game.currentPlayerOnMove
        val nextPlayerOnMove = game.nextPlayerOnMove

        playerOnMove.isOnMove = false
        nextPlayerOnMove.startMove()
    }

    fun nextDealer() {
        val currentDealer = game.currentDealer
        val nextDealer = game.nextDealer

        currentDealer.isDealer = false
        nextDealer.isDealer = true
    }

    private fun increaseBlinds() {
        if (System.currentTimeMillis() > game.nextBlinds) {
            game.apply {
                nextBlinds = System.currentTimeMillis() + config.blindIncreaseTime * 1000
                smallBlind = BlindCalculator.nextBlind(smallBlind)
                bigBlind = smallBlind * 2

                //updateStateListener(this@HoldemTournamentGameEngine)
            }
        }
    }

    private fun checkCurrentPlayerMoveTimeLimit() {
        // we want to check player's time limits only when he is on move and the round is in active state
        if(game.roundState != Game.RoundState.ACTIVE) {
            return
        }

        val currentPlayerOnMove = game.currentPlayerOnMove

        // if player has not played in his time limit, we will perform the default action on him
        if(System.currentTimeMillis() - currentPlayerOnMove.moveStart > game.config.playerMoveTime * 1000) {
            nextPlayerMove(
                currentPlayerOnMove.uuid,
                // if he can check he will check, otherwise he will fold
                if(currentPlayerOnMove.currentBet == game.targetBet)
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
            if(!game.isLateRegistrationEnabled || it.isKicked) {
                throw GameException(11, "Rebuy is not possible")
            }

            if(it.isFinished) {
                it.rebuyCount++
                it.isRebuyNextRound = true
            } else {
                throw GameException(19, "Player has not finished yet")
            }
        }

    fun showCards(playerUuid: String) =
        applyOnPlayer(playerUuid) {
            it.showCards = true
        }

    fun changeName(playerUuid: String, name: String) =
        applyOnPlayer(playerUuid) {
            it.name = name
        }

    fun leave(playerUuid: String) =
        applyOnPlayer(playerUuid) {
            it.isLeaveNextRound = true
            it.isKicked = true

            if(it.isOnMove) {
                nextPlayerMove(it.uuid, PlayerAction(PlayerAction.Action.FOLD))
            } else {
                it.action = PlayerAction.Action.FOLD
            }
        }

    fun pause(playerUuid: String, pause: Boolean) =
        applyOnAdminPlayer(playerUuid) {
            if (pause) {
                if (game.gameState == Game.State.ACTIVE && game.roundState == Game.RoundState.FINISHED) {
                    game.pause(true)
                } else {
                    throw GameException(15, "Game can be paused only when a round is finished")
                }
            } else {
                if (game.gameState == Game.State.PAUSED) {
                    if(!isRoundEndThreadRunning) {
                        game.pause(false)
                        startNewRound()
                    }
                }
            }
        }

    fun kickPlayer(playerUuid: String, playerIndex: Int) =
        applyOnAdminPlayer(playerUuid) { admin ->
            game.allPlayers.firstOrNull { it.index == playerIndex }?.let { player->
                leave(player.uuid)
            }
        }

    private fun applyOnPlayer(uuid: String, action: (Player) -> Unit) =
        game.allPlayers.firstOrNull { it.uuid == uuid }?.let {
            action(it)
        } ?: throw GameException(13, "Invalid player UUID")

    private fun applyOnAdminPlayer(playerUuid: String, action: (Player) -> Unit) =
        applyOnPlayer(playerUuid) {
            if(!it.isAdmin) {
                throw GameException(5, "Only admin can perform this action")
            }

            action(it)
        }
}