package io.pokr.game

import io.pokr.game.model.*
import io.pokr.game.tools.BlindCalculator
import io.pokr.game.exceptions.GameException
import io.pokr.game.model.PlayerAction
import io.pokr.game.tools.GameTimer
import io.pokr.game.tools.HandComparator
import io.pokr.game.tools.WinningsCalculator
import kotlin.concurrent.thread

class HoldemTournamentGameEngine(
    gameUuid: String,
    val updateStateListener: (HoldemTournamentGameEngine) -> Unit,
    val gameFinishedListener: (HoldemTournamentGameEngine) -> Unit
) {
    private val handComparator = HandComparator()

    val game = Game(gameUuid)

    val gameTimer = GameTimer {
        gameTick()
    }

    fun initGame(config: GameConfig) {
        game.config = config
    }

    fun addPlayer(playerUUID: String) {
        if(game.players.size == 9) {
            throw GameException(10, "Game is already full")
        }

        if(game.gameState != Game.State.CREATED && !game.isLateRegistrationEnabled) {
            throw GameException(11, "Late registration is not possible")
        }

        game.players.add(Player(playerUUID).apply {
            // assign random table index for a player
            index = ((1..9).toList() - game.players.map { it.index }).shuffled().first()

            // first connected player is an admin
            if(game.players.isEmpty()) {
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

        if(game.players.size == 1) {
            throw GameException(12, "Cannot start a game with only 1 player")
        }

        System.err.println("Game ${game.uuid} started")

        // set initial game state
        game.gameState = Game.State.ACTIVE
        game.gameStart = System.currentTimeMillis()

        game.smallBlind = game.config.startingBlinds
        game.bigBlind = game.smallBlind * 2
        game.nextBlinds = game.gameStart + game.config.blindIncreaseTime * 1000

        game.players.sortBy { it.index }
        game.players.shuffled().first().isDealer = true // chose random dealer

        // add chips to the players
        game.players.forEach {
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
        game.apply {
            round++
            cardStack = CardStack.create()
            targetBet = game.bigBlind
            previousTargetBet = 0
            roundState = Game.RoundState.ACTIVE
            winningCards = null
            tableCards = CardList()
        }

        // we will add chips to players that rebought
        game.players.filter { it.isRebuyNextRound }.forEach {
            it.isFinished = false
            it.isRebuyNextRound = false
            it.chips = game.config.startingChips
        }

        // reset players' states
        game.players.forEach {
            it.showCards = false
            it.isOnMove = false
            it.hand = null
            it.action = PlayerAction.Action.NONE
        }

        // draw cards for each non-finished player
        game.activePlayers.forEach {
            it.cards = game.cardStack.drawCards(2)
        }

        // move dealer to the next position
        val currentDealer = game.currentDealer
        val nextDealer = game.nextDealer

        currentDealer.isDealer = false
        nextDealer.isDealer = true

        // assign blinds and players on move
        (game.activePlayers + game.activePlayers).apply { // we have to merge arrays so the order can be "cyclic"

            // special case if there are only 2 players
            // dealer is SB and is on move, the other player is BB
            if(game.activePlayers.size == 2) {
                game.currentDealer.apply {
                    currentBet = kotlin.math.min(game.currentDealer.chips, game.smallBlind)
                    isOnMove = true
                    moveStart = System.currentTimeMillis()
                }

                get(indexOf(game.currentDealer) + 1).apply {
                    currentBet = kotlin.math.min(game.currentDealer.chips, game.bigBlind)
                }
            } else {
                // otherwise, the next player to the dealer is SB
                get(indexOf(game.currentDealer) + 1).apply {
                    currentBet = kotlin.math.min(chips, game.smallBlind)
                }

                // the next one BB
                get(indexOf(game.currentDealer) + 2).apply {
                    currentBet = kotlin.math.min(chips, game.bigBlind)
                }

                // and the next one is on move
                get(indexOf(game.currentDealer) + 3).apply {
                    isOnMove = true
                    moveStart = System.currentTimeMillis()
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
                player.currentBet = kotlin.math.min(game.targetBet, player.chips)
                true
            }

            // we can check only if we match the target bet
            PlayerAction.Action.CHECK ->
                player.currentBet == game.targetBet

            // raise and reset other players' actions
            PlayerAction.Action.RAISE -> {
                val raiseAmount = playerAction.numericValue!!
                if (raiseAmount > game.bigBlind) {// TODO: implement raise rules
                    if (player.chips - player.currentBet > raiseAmount) {
                        player.currentBet += raiseAmount
                        game.targetBet += raiseAmount

                        // we reset other player's action so they have to go again on this street
                        (game.players - player).forEach {
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
            if(game.players.count { !it.isFinished && it.action != PlayerAction.Action.FOLD } == 1) {
                finishRound()
                return
            }

            // everyone passed an action we will go to the next round
            if(game.players.none { it.action == PlayerAction.Action.NONE }) {

                // 5 cards already, we will finish the round
                if(game.tableCards.cards.size == 5) {
                    finishRound()
                } else {
                    // or we will draw a card and reset actions
                    drawCards()

                    // this is for implementing raise rules
                    game.previousTargetBet = game.targetBet

                    // non folded players will start with a NONE action next round
                    game.activePlayers.forEach {
                        it.action = PlayerAction.Action.NONE
                        it.hand = handComparator.findHighestHand(it.cards, game.tableCards)
                    }

                    // after cards are drawn, the next player from a dealer is on move
                    game.currentPlayerOnMove.isOnMove = false
                    (game.activePlayers + game.activePlayers).apply {
                        get(indexOf(game.currentDealer) + 1).isOnMove = true
                        get(indexOf(game.currentDealer) + 1).moveStart = System.currentTimeMillis()
                    }
                }
            } else {
                // go to the next player
                nextPlayer()
            }
        }
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
        nextPlayerOnMove.isOnMove = true
        nextPlayerOnMove.moveStart = System.currentTimeMillis()
    }

    private fun finishRound() {
        // calculate winnings before round ended (all folded)
        // we just take the pot and put it to non-folded player's chips
        if(game.players.count { !it.isFinished && it.action != PlayerAction.Action.FOLD } == 1) {
            val winner = game.players.first { it.action != PlayerAction.Action.FOLD }
            val pot = game.players.sumBy { it.currentBet }
            game.players.forEach {
                it.chips -= it.currentBet
                it.currentBet = 0
            }
            winner.chips += pot
        } else {
            // calculate winning after regular round
            val ranks = handComparator.evalPlayers(game.players, game.tableCards)
            game.winningCards = ranks[0].bestCards
            WinningsCalculator.calculateWinnings(ranks)
        }

        // if a player has 0 chips, he is finished and won't play anymore (unless he rebuys)
        game.players.filter { it.chips == 0 }.forEach { it.isFinished = true }

        // switch to the FINISHED state, no actions and be performed anymore and the results of the round are shown
        game.roundState = Game.RoundState.FINISHED

        // if there is only one player with chips we will finish the game
        if(game.players.count { it.chips > 0 } == 1) {
            finishGame()
        } else {
            // otherwise we will wait some time and start a new round
            thread {
                Thread.sleep(3_000)
                startNewRound()
                updateStateListener(this)
            }
        }
    }

    private fun finishGame() {
        // TODO: add player ranks

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

        // we need to increase the blinds
        if(System.currentTimeMillis() > game.nextBlinds) {
            increaseBlinds()
        }

        checkCurrentPlayerMoveTimeLimit()
    }

    private fun increaseBlinds() =
        game.apply {
            nextBlinds = System.currentTimeMillis() + config.blindIncreaseTime * 1000
            smallBlind = BlindCalculator.nextBlind(smallBlind)
            bigBlind = smallBlind * 2

            updateStateListener(this@HoldemTournamentGameEngine)
        }

    private fun checkCurrentPlayerMoveTimeLimit() {
        // we want to check player's timelimits only when he is on move and the round is in active state
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
            if(!game.isLateRegistrationEnabled) {
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
                    game.pause(false)
                    startNewRound()
                }
            }
        }

    fun kickPlayer(playerUuid: String, playerIndex: Int) =
        applyOnAdminPlayer(playerUuid) {
            if (game.gameState == Game.State.ACTIVE && game.roundState == Game.RoundState.FINISHED) {
                game.players.firstOrNull { it.index == playerIndex }?.apply {
                    isFinished = true
                    chips = 0
                }
            } else {
                throw GameException(15, "Player can be kicked a round is finished")
            }
        }

    private fun applyOnPlayer(uuid: String, action: (Player) -> Unit) =
        game.players.firstOrNull { it.uuid == uuid }?.let {
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