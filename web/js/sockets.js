const socket = io.connect(location.protocol + '//' + window.location.hostname + ':' + socketsPort);

let disconnectedToast;

//reconnection variable
let reconnected = false;

//data
let gameState;
let finishedData;
let finishedPrev;
let prevData;
let roundTurn = 0;
let roundAfterReconnect = 0;
let cardChanged = false;
let street = 'preflop';
let prevStreet = 'none';
let prevRoundState = 'none';
let lastAction = 'none';
let soundOn = false;
let messageShown = false;
let switchedTab = false;
let winningAnimationInProgress = false;
let showedCards = 0;
let currentSmallBlind;

let timerBlinds = -1;
let timerRebuys = -1;
let showCardsDelay = 0;
let rebuyRound = -1;
let cardsSettings = '';

let showCardsInProgress = false;

let responseTimer;

socket.on('connect', () => {
    if (disconnectedToast) {
        disconnectedToast.reset();
        disconnectedToast = null;

        $.toast({
            heading: 'Reconnected',
            text: 'Reconnected to the game server.',
            loader: false,
            bgColor: '#55AA00',
            textColor: 'white',
            icon: 'success',
        });

        const nick = localStorage.getItem('nick');
        if (nick && localStorage.getItem('gameStarted') === 'true') {
            connectToGame(nick);
        }
    }
});

socket.on('disconnect', () => {
    if (disconnectedToast) {
        disconnectedToast.reset();
    }
    disconnectedToast = $.toast({
        heading: 'Disconnected',
        text: 'Trying to reconnect...',
        loader: false,
        allowToastClose: false,
        hideAfter: false,
        bgColor: '#FF5500',
        textColor: 'white',
        icon: 'error',
    });
});

// inbound events
socket.on('gameState', function (data) {
    clearTimeout(responseTimer);
    responseTimer = setTimeout(requestGameState, data.config.playerMoveTime * 1000 * 0.8);

    setStreet(data);

    if (shouldRefreshView(data)) {
        gameState = data.state;
        if (gameState !== 'finished') {
            localStorage.setItem('player_uuid', data.user.uuid);
            localStorage.setItem('game_uuid', data.uuid);
            localStorage.setItem('nick', $('<textarea/>').html(data.user.name).text());
        }
        initializeVars(data);
        //user is in game
        $('#loader').hide();
        loader.pause();
        $('#settings').hide();
        $('.left-container').hide();
        $('#main-screen').hide();
        $('.game-container').show();
        $('.errmsg').html('');

        if (gameState === 'created') {
            $('.pregame').show();
            $('#code').text(data.uuid);
            const $adminText = $('.admin-text');
            //user is admin
            if (data.user.admin) {
                if (data.players.length > 0) {
                    $adminText.html('');
                    $('#start').show();
                } else {
                    $adminText.html('You will be able to start the game when there are 2 or more players.');
                    $('#start').hide();
                }
            } else {
                $adminText.html('Waiting for admin to start the game.');
            }

            if (prevData && data.players.length < prevData.players.length) {
                const playersToRemove = prevData.players.filter(prevPlayer =>
                    !data.players.some(({index}) => index === prevPlayer.index));
                const positionsToClear = playersToRemove.map(({index}) => getPlayerPosition(data, index));
                positionsToClear.forEach(pos => {
                    $(`#player${pos}`).removeClass('created none').addClass('seatopen');
                    $(`#player${pos} .info-box .player-name`).text('');
                    $(`#player${pos} .info-box .player-chips`).text('');
                });
            }
        }

        if (gameState === 'active' || gameState === 'paused') {
            $('.pregame').hide();
            $('.game-info').show();

            //show blinds and other info
            currentSmallBlind = data.smallBlind;
            $('.blinds-state .current').html(`${data.smallBlind} / ${data.smallBlind * 2}`);
            if ($(window).width() > 1023) {
                $('.blinds-state .next').html(`${data.nextSmallBlind} / ${data.nextSmallBlind * 2}`);
            }

            if (gameState === 'paused') {
                $('.level-time span').html('The game is paused.');
                if (timerBlinds !== -1) {
                    window.clearInterval(timerBlinds);
                }
                if (timerRebuys !== -1) {
                    window.clearInterval(timerRebuys);
                }
            } else {
                blindsTimer(data.nextBlindsChangeAt, gameState);
                lateRegTimer(data.config.maxRebuys, data.config.rebuyTime, data.gameStart, gameState);
            }
            updateLeaderboard(data);
            assignTags(data);
        }

        if (data.user.admin) {
            if (gameState === 'active') {
                if (data.roundState === 'finished') {
                    $('#pause').removeClass('disabled');
                    $('#unpause').addClass('disabled');
                } else {
                    $('#pause').addClass('disabled');
                }
            }
            if (gameState === 'paused') {
                refreshCards();
                $('.showCards').removeClass('showCards');
                $('#unpause').removeClass('disabled');
                $('#pause').addClass('disabled');
            }
        }

        if (gameState === 'finished') {
            $('.game-info').hide();
            $('#pot').hide();
            $('#total-pot').hide();
            $('#start').hide();
            $('#rebuys-btn').hide();

            $('.postgame').show();

            if (data.user.admin) {
                ga('send', 'event', 'Action', 'Game finished');
                ga('send', {
                    hitType: 'timing',
                    timingCategory: 'Game',
                    timingVar: 'Total duration',
                    timingValue: data.time - data.gameStart,
                });
            }
        }

        if (data.roundState === 'finished') {
            roundAfterReconnect++;
        }

        //if user reconnects, set rebuy round to current round (so user don't show the rebuy button again)
        if (reconnected) {
            rebuyRound = data.round;
        }
        console.log(data);

        printPlayers(data);

        if (prevRoundState === 'finished') {
            $('#pot').hide();
        }

        showCardsInProgress = false;

        //results of the game
        if (gameState === 'finished') {
            updateLeaderboard(data);
            showResults(data);
            localStorage.removeItem('player_uuid');
            localStorage.removeItem('game_uuid');
        }
    }

    prevData = data;
    prevRoundState = data.roundState;
});

socket.on('error', function (data) {
    console.log(data);

    clearTimeout(responseTimer);

    //hide loader if err
    $('#loader').hide();

    if (data.code === 20) { // invalid game UUID
        localStorage.setItem('gameStarted', 'false');
        localStorage.removeItem('game_uuid');
        if ($('#gameid').val().length > 0) {
            joinInputValidated[1] = false;
            const $gameIdErr = $('#gameid ~ .errmsginput');
            $gameIdErr.show();
            $gameIdErr.text('Invalid game ID.');
            $('#gameid').addClass('invalid');
            $('#join-err').html('Some of the fields do not have correct value.').show();
        }
    } else if (data.code === 7) {
        joinInputValidated[0] = false;
        createInputValidated[0] = false;
        $('#userid-create ~ .errmsginput').text('Invalid name.').show();
        $('#userid-join ~ .errmsginput').text('Invalid name.').show();
        $('#userid-create').addClass('invalid');
        $('#userid-join').addClass('invalid');
        $('#join-err').html('Some of the fields do not have correct value.').show();
        $('#create-err').html('Some of the fields do not have correct value.').show();
    } else if ([10, 11, 22].includes(data.code)) {
        localStorage.setItem('gameStarted', 'false');
        localStorage.removeItem('game_uuid');
        $('#join-err').html(`${data.message}.`).show();
    } else if (data.code === 21) { // No such player in any game session
        $.toast({
            heading: 'Error',
            text: 'Couldn\'t get game state. Try refreshing.',
            loader: false,
            bgColor: '#FF5500',
            textColor: 'white',
            icon: 'error',
        });
      } else if (!(data instanceof Error)) {
        // show only messages from the game server, don't bother users with underlying errors (e.g. "xhr poll error")
        $.toast({
            heading: 'Error',
            text: `${data.message}.`,
            loader: false,
            bgColor: '#FF5500',
            textColor: 'white',
            icon: 'error',
        });
    }
});

socket.on('gameDisbanded', function () {
    clearTimeout(responseTimer);
    gameState = 'finished';
    localStorage.setItem('gameStarted', 'false');
});

socket.on('chat', function (data) {
    console.log(data);

    const message = data.message;
    const flash = data.flash; // if the message is a react or regular message

    if (flash) {
        console.log('[' + data.time + ']' + data.name + ': ' + message);
    } else {
        // TODO: add message to the chat window
    }
});


// outbound events

function createGame(nickname, gameConfig) {
    localStorage.setItem('gameStarted', 'true');
    responseTimer = setTimeout(requestGameState, 3000);

    socket.emit('createGame', {
        name: nickname,
        playerUUID: localStorage.getItem('player_uuid'),
        gameConfig: gameConfig,
    });

    ga('send', 'event', 'Action', 'Game created');
}

function connectToGame(nickname, gameUuid = null) {
    localStorage.setItem('gameStarted', 'true');
    responseTimer = setTimeout(requestGameState, 3000);

    socket.emit('connectGame', {
        name: nickname,
        gameUUID: gameUuid,
        playerUUID: localStorage.getItem('player_uuid'),
    });

    ga('send', 'event', 'Action', 'Connected to game');
}

function requestGameState() {
    // TODO: differentiate between calls from connect / create game and automatic requests during the game
    ga('send', 'event', 'System', 'Manual state request');

    socket.emit('gameRequest');
}

// player actions

function sendAction(action, numericValue = null, textValue = null) {
    socket.emit('action', {
        action: action,
        numericValue: numericValue,
        textValue: textValue,
    });
}

function leave() {
    if (gameState !== 'finished') {
        sendAction('leave');
    }

    localStorage.setItem('gameStarted', 'false');
    localStorage.removeItem('game_uuid');

    ga('send', 'event', 'Action', 'Leave');

    window.location.reload();
}

function gameCall() {
    uncheckAutoActions();
    sendAction('call');
}

function gameCheck() {
    uncheckAutoActions();
    sendAction('check');
}

function gameFold() {
    uncheckAutoActions();
    sendAction('fold');
}

function gameRaise(amount) {
    sendAction('raise', amount, null);
}

function rebuy() {
    sendAction('rebuy');
}

function showCards() {
    showedCards = finishedData.round;
    sendAction('showCards');

    ga('send', 'event', 'Game', 'Showed cards');
}

// Admin actions

function startGame() {
    sendAction('startGame');

    ga('send', {
        hitType: 'event',
        eventCategory: 'Action',
        eventAction: 'Game started',
        eventLabel: prevData ? prevData.players.length + 1 : 0,
    });
}

function kick(playerIndex) {
    sendAction('kick', playerIndex, null);
}

function pause() {
    sendAction('pause', 1, null);
}

function unpause() {
    sendAction('pause', 0, null);
}

// chat

function sendReaction(reaction) {
    socket.emit('chat', {
        message: reaction,
        flash: true,
    });
}

function sendChatMessage(msg) {
    socket.emit('chat', {
        message: msg,
        flash: false,
    });
}

// MISC

const setPlayerChips = (data, player, prevDataPlayer, playerPosition) => {
    const $chips = $(`#${playerPosition} .player-chips`);

    let extraShowdownPotChips = 0;

    if (data.roundState === 'finished') {
        // if data contain info about the same finished round as previous data, we do not update chips
        if (!(prevData && prevData.roundState === 'finished' && prevData.round === data.round)) {
            if (prevDataPlayer) {
                const playerBets = [
                    data.user.currentBet,
                    ...data.players.map(player => player.currentBet)
                ].sort((a, b) => b - a);

                if (player.currentBet === playerBets[0]) {
                    // if the highest betting player's bet isn't matched by the second highest betting player,
                    // we return the extra chips to the highest betting player before going through with the showdown
                    extraShowdownPotChips = playerBets[0] - playerBets[1];
                }
                $chips.html(prevDataPlayer.chips - player.currentBet + extraShowdownPotChips);
            } else {
                $chips.html(player.chips);
            }
        }
    } else {
        $chips.html(player.chips - player.currentBet);
    }

    return extraShowdownPotChips;
};

//TODO refactor, split into multiple functions
function printPlayers(data) {
    let cards;
    giveCSSClasses(data, 1, -1);
    playSound(data);

    let playerBets = 0;
    let pot = data.user.currentBet;
    const players = [[1, data.user.dealer]];

    $('#player1 .player-name').html(data.user.name);

    let extraPotChips = setPlayerChips(data, data.user, prevData ? prevData.user : null, 'player1');
    pot -= extraPotChips;

    showRebuyControls(data);
    showRebuyAndAddonsStats(data);
    updateLastPlayedHand(data);

    const userCurrentStreetBet = data.user.currentBet > data.previousStreetTargetBet
        ? data.user.currentBet - data.previousStreetTargetBet
        : 0;
    playerBets += userCurrentStreetBet;

    assignChipsImg(userCurrentStreetBet, 'player1', data);

    if (data.user.cards.length > 0 && (reconnected || prevData.user.cards !== data.user.cards)) {
        cards = data.user.cards.split(' ');
        $('#player1 .card-1').html('<img src="img/cards/' + cardsSettings + cards[0] + '.svg"/>');
        $('#player1 .card-2').html('<img src="img/cards/' + cardsSettings + cards[1] + '.svg"/>');
    }

    if (showCardsInProgress === false) {
        dealCards(data);
    }

    //show cards button
    if (
        (data.roundState === 'finished' && data.user.action === 'fold') ||
        (everyoneElseFoldedOrBusted(data) && data.state === 'active')
    ) {
        setTimeout(function () {
            $('#additional').removeClass('disabled');
        }, showCardsDelay);
        $('#additional')
            .html('Show cards')
            .delay(showCardsDelay + 3000).hide(0)
            .show();
    } else {
        $('#additional').addClass('disabled');
    }
    showCardsDelay = 0;

    //timer functionality
    if (data.user.onMove) {
        playerCountdown(data.user.moveStart, 1, data.config.playerMoveTime, data.time);
    }

    const positionedPlayers = new Map([[1, data.user]]);

    let $player;
    for (let i = 0; i < data.players.length; i++) {
        const position = getPlayerPosition(data, data.players[i].index);
        positionedPlayers.set(position, data.players[i]);

        $player = $('#player' + position);
        $player.removeClass('seatopen');

        players.push([position, data.players[i].dealer]);

        giveCSSClasses(data, position, i);
        $('#player' + position + ' .player-name').html(data.players[i].name);

        extraPotChips = setPlayerChips(data, data.players[i], prevData ? prevData.players[i] : null, `player${position}`);
        pot -= extraPotChips;

        if (data.roundState === 'finished' && data.state === 'active' && typeof data.players[i].cards !== 'undefined') {
            //showCard fuctionality
            $player.removeClass('fold');
        }

        const playerCurrentStreetBet = data.players[i].currentBet > data.previousStreetTargetBet
            ? data.players[i].currentBet - data.previousStreetTargetBet
            : 0;
        assignChipsImg(playerCurrentStreetBet, 'player' + position, data);
        playerBets += playerCurrentStreetBet;

        //showdown
        if (typeof data.players[i].cards !== 'undefined' && data.roundState === 'finished' && data.state !== 'finished') {
            if (data.players[i].cards.length > 0) {
                cards = data.players[i].cards.split(' ');
                $('#player' + position + ' .card-1').html('<img src="img/cards/' + cardsSettings + cards[0] + '.svg"/>');
                $('#player' + position + ' .card-2').html('<img src="img/cards/' + cardsSettings + cards[1] + '.svg"/>');
                $('#player' + position).addClass('showdown');
            }
        } else {
            $('#player' + position + ' .card-1').html('');
            $('#player' + position + ' .card-2').html('');
            $('#player' + position).removeClass('showdown');
        }

        if (data.players[i].onMove) {
            playerCountdown(data.players[i].moveStart, position, data.config.playerMoveTime, data.time);
        }

        pot += data.players[i].currentBet;
    }

    //just a ugly hack to get the slider's handle to the left
    $('#range-slider')[0].value = 0;

    //show or hide auto actions
    const autoaction = autoControls(data);

    //show controls
    if (data.user.onMove === true && data.roundState !== 'finished' && autoaction === false) {
        showControls(data);
    } else {
        hideControls();
    }

    //hide inactive users
    for (let i = 1; i <= 9; i++) {
        const player = positionedPlayers.get(i);
        if (data.state === 'active' && player && player.finalRank === 0 && !player.rebuyNextRound && !player.hasLeft) {
            $(`#player${i}`).removeClass('folded');
        } else {
            $(`#player${i}`).addClass('folded');
        }
    }

    //Determine, who is the Dealer
    players.sort();
    let dealer;
    for (let i = 0; i < players.length; i++) {
        if (players[i][1] === true) {
            dealer = players[i][0];
        }
        $('#player' + players[i][0] + ' .dealer').removeClass('is-dealer');
    }
    $('#player' + dealer + ' .dealer').addClass('is-dealer');

    if (data.roundState === 'finished' && data.state !== 'paused' && data.state !== 'finished') {

        //Highlight cards at the end of the round, only if the showdown was at the river (no earlier allin runout)
        if ($('.dealt-cards-5').css('opacity') === '1' && typeof data.bestCards !== 'undefined') {
            highlightCards(finishedData);
            winningAnimationHandler();
        }

        //Show winner, but exclude showdowns
        if (typeof data.bestCards === 'undefined') {
            winningAnimationHandler();
        }
    }

    //hide timers in showdown
    if (data.roundState === 'finished') {
        $('.player-timer-running').hide();
    } else {
        $('.player-timer-running').show();
    }

    //display pot
    if (data.roundState === 'finished') {
        // if the round is finished, the pot is computed from prevData; without it, better not display anything
        if (prevData) {
            assignChipsImg(pot, 'pot', data);
        }
    } else if (data.roundState === 'active') {
        const centerPot = pot - playerBets;
        if (centerPot > 0) {
            assignChipsImg(centerPot, 'pot', data);
        }
    }

    // if the round is finished, the pot is computed from prevData; without it, better not display anything
    if (data.state === 'active' && (prevData || data.roundState !== 'finished')) {
        $('#total-pot').html('Pot: ' + pot);
    }
}

function showControls(data) {
    if (data.user.currentBet === data.user.chips) {
        gameCall();
    } else {
        $('#fold').removeClass('disabled');

        if (data.targetBet === data.user.currentBet) {
            $('#check').removeClass('disabled');
        }

        //show call or check if can
        if (data.targetBet > data.user.currentBet) {
            $('#call')
                .removeClass('disabled')
                .html('Call<br>' + Math.min(data.user.chips - data.user.currentBet, data.targetBet - data.user.currentBet));
        }

        //show raise if can
        if (data.user.canRaise && data.targetBet < data.user.chips) {
            const $raise = $('#raise');
            $raise.removeClass('disabled');

            const currentBetThisStreet = data.user.currentBet - data.previousStreetTargetBet;

            const minRaiseTo = Math.min(data.minRaiseTo, data.user.chips);
            const minRaiseBy = minRaiseTo - data.user.currentBet;

            const minRaiseToThisStreet = minRaiseTo - data.previousStreetTargetBet;
            const maxRaiseToThisStreet = data.user.chips - data.previousStreetTargetBet;

            let buttonDesc;

            if (data.targetBet === data.previousStreetTargetBet) {
                buttonDesc = 'Bet<br>';
            } else {
                buttonDesc = 'Raise to<br>';
            }

            //adjust the raise in the button
            $raise
                .off('click')
                .on('click', () => gameRaise(minRaiseBy))
                .html(buttonDesc + minRaiseToThisStreet);

            //if there are not enough chips to raise more, don't show the slider and input
            if (data.user.chips > data.minRaiseTo) {
                $('.raise-slider').removeClass('disabled');
            }

            //don't show raise if everyone is all in and you cover them
            if (isEveryoneElseAllin(data)) {
                $('.raise-slider').addClass('disabled');
                $('#raise').addClass('disabled');
            }

            const $raiseInput = $('.raise-input');
            //affect slider and input accordingly
            $raiseInput.attr({
                'min': minRaiseToThisStreet,
                'max': maxRaiseToThisStreet,
                'value': minRaiseToThisStreet,
            });

            let changingInput = false;
            $raiseInput.val(minRaiseToThisStreet);
            $raiseInput.on('keyup', function (e) {
                //min value is min raise, max is max raise
                let value = Math.min(maxRaiseToThisStreet, Math.max(minRaiseToThisStreet, e.target.value));
                const $raise = $('#raise');
                $raise.off('click').on('click', () => gameRaise(value - currentBetThisStreet));

                //treat empty input as 0
                $raise.html(buttonDesc + value);
                if (value == '') {
                    value = 0;
                }
                changingInput = true;

                //affect slider
                $('#range-slider')[0].value = value;
            });

            const $rangeSlider = $('#range-slider');
            $rangeSlider.attr({
                'min': minRaiseToThisStreet,
                'max': maxRaiseToThisStreet,
                'value': minRaiseToThisStreet,
            });

            $rangeSlider[0].value = minRaiseToThisStreet;

            $rangeSlider[0].oninput = function () {
                const value = Math.min(maxRaiseToThisStreet, Math.max(minRaiseToThisStreet, this.value));
                //Round to 10s, but exclude max value
                let roundedVal = parseInt(value / 10) * 10;
                if (roundedVal + 9 >= maxRaiseToThisStreet) {
                    roundedVal = maxRaiseToThisStreet;
                }
                if (roundedVal < minRaiseToThisStreet) {
                    roundedVal = minRaiseToThisStreet;
                }
                $('#raise')
                    .off('click')
                    .on('click', () => gameRaise(roundedVal - currentBetThisStreet))
                    .html(buttonDesc + roundedVal);
                $('.raise-input').val(roundedVal);
            };

            //change bet sizes buttons
            //if preflop, show 2.5 / 3 / 3.5 Buttons, else 33 / 50 / 66 prc
            const $betsizeFirst = $('.betsizes.first');
            const $betsizeSecond = $('.betsizes.second');
            const $betsizeThird = $('.betsizes.third');
            const $betsizeLast = $('.betsizes.last');

            $betsizeFirst.removeClass('disabled');
            $betsizeSecond.removeClass('disabled');
            $betsizeThird.removeClass('disabled');

            if (street === 'preflop') {
                if (data.pot <= 3 * data.smallBlind) {
                    $betsizeFirst
                        .html('2.5BB')
                        .off('click')
                        .on('click', () => raiseChange(Math.min(5 * data.smallBlind, maxRaiseToThisStreet)));
                    $betsizeSecond
                        .html('3BB')
                        .off('click')
                        .on('click', () => raiseChange(Math.min(6 * data.smallBlind, maxRaiseToThisStreet)));
                    $betsizeThird
                        .html('3.5BB')
                        .off('click')
                        .on('click', () => raiseChange(Math.min(7 * data.smallBlind, maxRaiseToThisStreet)));
                } else {
                    if (data.pot > minRaiseToThisStreet && data.pot < maxRaiseToThisStreet) {
                        $betsizeFirst
                            .html('Pot')
                            .off('click')
                            .on('click', () => raiseChange(Math.min(data.pot, maxRaiseToThisStreet)));
                    } else {
                        $betsizeFirst.addClass('disabled');
                    }
                    if (1.5 * data.pot > minRaiseToThisStreet && 1.5 * data.pot < maxRaiseToThisStreet) {
                        $betsizeSecond
                            .html('1.5Pot')
                            .off('click')
                            .on('click', () => raiseChange(Math.min(1.5 * data.pot, maxRaiseToThisStreet)));
                    } else {
                        $betsizeSecond.addClass('disabled');
                    }
                    if (2 * data.pot > minRaiseToThisStreet && 2 * data.pot < maxRaiseToThisStreet) {
                        $betsizeThird
                            .html('2Pot')
                            .off('click')
                            .on('click', () => raiseChange(Math.min(2 * data.pot, maxRaiseToThisStreet)));
                    } else {
                        $betsizeThird.addClass('disabled');
                    }
                }
                $betsizeLast.off('click').on('click', () => raiseChange(maxRaiseToThisStreet));
            } else {
                const bigBlind = 2 * data.smallBlind;
                const potThird = parseInt(data.pot / 3);
                const potHalf = parseInt(data.pot / 2);
                const potTwoThirds = parseInt(2 * data.pot / 3);

                if (potThird > bigBlind && potThird > minRaiseToThisStreet && potThird < maxRaiseToThisStreet) {
                    $betsizeFirst
                        .html('33%')
                        .off('click')
                        .on('click', () => raiseChange(Math.min(potThird, maxRaiseToThisStreet)));
                } else {
                    $betsizeFirst.addClass('disabled');
                }

                if (potHalf > bigBlind && potHalf > minRaiseToThisStreet && potHalf < maxRaiseToThisStreet) {
                    $betsizeSecond
                        .html('50%')
                        .off('click')
                        .on('click', () => raiseChange(Math.min(potHalf, maxRaiseToThisStreet)));
                } else {
                    $betsizeSecond.addClass('disabled');
                }

                if (potTwoThirds > bigBlind && potTwoThirds > minRaiseToThisStreet && potTwoThirds < maxRaiseToThisStreet) {
                    $betsizeThird
                        .html('66%')
                        .off('click')
                        .on('click', () => raiseChange(Math.min(potTwoThirds, maxRaiseToThisStreet)));
                } else {
                    $betsizeThird.addClass('disabled');
                }

                $betsizeLast.off('click').on('click', () => raiseChange(maxRaiseToThisStreet));
            }

        }
    }
}

function hideControls() {
    $('#fold').addClass('disabled');
    $('#call').addClass('disabled');
    $('#check').addClass('disabled');
    $('#raise').addClass('disabled');
    $('.raise-slider').addClass('disabled');
}

function uncheckAutoActions() {
    $('#autocheck').prop('checked', false);
    $('#autofold').prop('checked', false);
}

function autoControls(data) {
    let autoaction = false;

    if ($('#autocheck').prop('checked') && data.user.currentBet === checkHighestBet(data)) {
        autoaction = true;
        if (data.user.onMove === true) {
            wait(800);
            gameCheck();
        }
    }

    if ($('#autofold').prop('checked')) {
        autoaction = true;
        if (data.user.onMove === true) {
            wait(800);
            gameFold();
        }
    }
    $('.autocheck').addClass('disabled');
    $('.autofold').addClass('disabled');

    if (data.user.onMove === true && data.roundState !== 'finished' && autoaction === false) {

    } else {

        //show autofold button when out of turn and cannot check
        if (data.roundState !== 'finished' && (data.user.currentBet < checkHighestBet(data)) && data.user.action === 'none' && data.user.chips > 0) {
            $('.autofold').removeClass('disabled');
        }

        //show autocheck button when out of turn and can check
        if (data.roundState !== 'finished' && (data.user.currentBet >= checkHighestBet(data)) && data.user.action === 'none' && data.state === 'active') {
            $('.autocheck').removeClass('disabled');
        }
    }

    return autoaction;
}

//checks highest bet on a street
function checkHighestBet(data) {
    let result = data.user.currentBet;
    for (let i = 0; i < data.players.length; i++) {
        result = Math.max(result, data.players[i].currentBet);
    }
    return result;
}

//returns true if everyone is folded or allin
function isEveryoneElseAllin(data) {
    let rtn = true;
    for (let i = 0; i < data.players.length; i++) {
        if (data.players[i].action !== 'fold') {
            rtn = rtn && (data.players[i].currentBet === data.players[i].chips);
        }
    }
    return rtn;
}

//show cards
function dealCards(data) {
    const cards = data.cards.split(' ');
    cards.reverse();

    function clearBoard() {
        for (let i = 1; i <= 5; i++) {
            $(`.dealt-cards-${i}`).html('').css('opacity', 0);
        }
    }

    function addFlop() {
        $('.dealt-cards-1').html('<img src="img/cards/' + cardsSettings + cards[0] + '.svg"/>');
        $('.dealt-cards-2').html('<img src="img/cards/' + cardsSettings + cards[1] + '.svg"/>');
        $('.dealt-cards-3').html('<img src="img/cards/' + cardsSettings + cards[2] + '.svg"/>');
    }

    function addTurn() {
        $('.dealt-cards-4').html('<img src="img/cards/' + cardsSettings + cards[3] + '.svg"/>');
    }

    function addRiver() {
        $('.dealt-cards-5').html('<img src="img/cards/' + cardsSettings + cards[4] + '.svg"/>');
    }

    //delete cards from previous game
    if (cards[0] === '') {
        clearBoard();
    }

    if (reconnected || switchedTab) {
        if (street === 'flop') {
            addFlop();
            animationFlopInstant();
        }
        if (street === 'turn') {
            addFlop();
            addTurn();
            animationFlopInstant();
            animationTurnInstant();
        }
        if (street === 'river' || (street === 'preflopShow' && data.cards.length === 14)) {
            addFlop();
            addTurn();
            addRiver();
            animationFlopInstant();
            animationTurnInstant();
            animationRiverInstant();
        }
        $('.dealt-cards-1').css('opacity', 0);
        reconnected = false;

    } else {
        //animate allins streets = preflop allin
        if (switchedTab) {
            if ((street === 'turn' || switchedTab) && $('.dealt-cards-3').css('opacity') === 0) {
                addFlop();
                addTurn();
                animationFlopInstant();
                animationTurnInstant();
            }
            if ((street === 'river' || switchedTab) && $('.dealt-cards-4').css('opacity') === 0) {
                addFlop();
                addTurn();
                addRiver();
                animationFlopInstant();
                animationTurnInstant();
                animationRiverInstant();
            }
        }
        if (street === 'preflopShow' && data.cards.length === 14) {
            showCardsDelay = 5000;
            clearBoard();
            addFlop();
            addTurn();
            addRiver();
            animationAll();
        }

        if (street === 'flopShow' && data.cards.length === 14) {
            showCardsDelay = 3500;
            $('.dealt-cards-4').css('opacity', 0);
            $('.dealt-cards-5').css('opacity', 0);
            addTurn();
            addRiver();
            animationTurnAndRiver();
        }

        if (street === 'turnShow' && data.cards.length === 14) {
            showCardsDelay = 2500;
            addRiver();
            animationRiverShowdown();
        }

        if (street === 'flop' && cardChanged) {
            addFlop();
            animationFlop();
        }

        if (street === 'turn' && cardChanged) {
            addTurn();
            if (switchedTab && $('.dealt-cards-3').css('opacity') === 0) {
                animationFlopInstant();
            }
            animationTurn();
        }

        if (street === 'river' && cardChanged) {
            addRiver();
            if (switchedTab && $('.dealt-cards-3').css('opacity') === 0) {
                animationFlopInstant();
            }
            if (switchedTab && $('.dealt-cards-4').css('opacity') === 0) {
                animationTurnInstant();
            }
            animationRiver();
        }
    }
}

//changes input and slider to a value
function raiseChange(value) {
    const $raiseInput = $('.raise-input');
    $raiseInput.val(value);
    $('#range-slider')[0].value = value;
    $raiseInput.keyup();
}

//get css classes for certain player
function giveCSSClasses(data, position, i) {
    const $player = $('#player' + position);
    $player.removeClass('created active paused finished ractive rfinished none call raise check fold onMove');

    let onm = false;
    if (position === 1) {
        onm = data.user.onMove;
        $('#player1').addClass(data.state + ' r' + data.roundState + ' ' + data.user.action);
    } else {
        onm = data.players[i].onMove;
        $player.addClass(data.state + ' r' + data.roundState + ' ' + data.players[i].action);
    }

    if (onm === true) {
        $player.addClass('onMove');
    }
}

//highlight winning cards
function highlightCards(data) {
    const $card1 = $('.card-1');
    const $card2 = $('.card-2');
    const $dealtCards = $('.dealt-cards div');

    //turn off animations for other players when user reconnects
    if (isConnectionChangeData(data)) {
        $card1.addClass('notransition');
        $card2.addClass('notransition');
        $dealtCards.addClass('notransition');
        $card1[0].offsetHeight; // Trigger a reflow, flushing the CSS changes
        $card2[0].offsetHeight;
        $dealtCards[0].offsetHeight;
    } else {
        $card1.removeClass('notransition');
        $card2.removeClass('notransition');
        $dealtCards.removeClass('notransition');
    }
    const winners = [];
    const arrPos = [];

    if (data.user.winner === true) {
        winners.push(data.user.index);
        arrPos.push(-1);
    }

    //TODO Replace finalRank by handRank when implemented

    //determine who won
    for (let i = 0; i < data.players.length; i++) {
        if (data.players[i].winner === true) {
            winners.push(data.players[i].index);
            arrPos.push(i);
        }
    }

    $card1.addClass('notPlaying');
    $card2.addClass('notPlaying');

    //hide cards in players hands
    for (let i = 0; i < winners.length; i++) {
        const position = getPlayerPosition(data, winners[i]);
        let cardsPl;
        let bestPl;

        if (position === 1) {
            cardsPl = data.user.cards.split(' ');
            bestPl = data.user.bestCards.split(' ');
        } else {
            cardsPl = data.players[arrPos[i]].cards.split(' ');
            bestPl = data.players[arrPos[i]].bestCards.split(' ');
        }

        for (let k = 0; k < cardsPl.length; k++) {
            const contains = (bestPl[0] === cardsPl[k]) || (bestPl[1] === cardsPl[k]);
            if (contains) {
                $('#player' + position + ' .cards .card-' + (k + 1)).removeClass('notPlaying');
            }
        }
    }

    const cards = data.cards.split(' ');
    const bestCards = data.bestCards.split(' ');
    let pos = bestCards.length;
    //partly hide cards that does not won
    for (let i = 5; i > 0; i--) {
        if (cards[i - 1] !== bestCards[pos - 1]) {
            $('.dealt-cards-' + (6 - i)).addClass('notPlaying');
        } else if (pos === 0) {
            $('.dealt-cards-' + (6 - i)).addClass('notPlaying');
        } else {
            pos--;
        }
    }
}

//resets all css on the cards
function refreshCards() {
    $('.notPlaying').removeClass('notPlaying');
}

//returns true if noone played yet
function noOnePlayed(data) {
    if (data.user.action !== 'none' && data.user.action !== 'fold') {
        return false;
    }
    for (let i = 0; i < data.players.length; i++) {
        if (data.players[i].action !== 'none' && data.players[i].action !== 'fold') {
            return false;
        }
    }
    return true;
}

function getPlayerPosition(data, index) {
    let position;
    if (data.user.index === index) {
        return 1;
    }
    if (data.user.index < index) {
        position = index - data.user.index + 1;
    } else {
        position = index - data.user.index + 10;
    }
    return position;
}

//returns biggest winner index, the function works with effective chipcounts and only on finished state
function getBiggestWinner(data) {
    let index = [];
    let amount = 0;
    if (data.user.action !== 'fold') {
        index.push(data.user.index);
        amount = data.user.chips - finishedPrev.user.chips;
    }
    for (let i = 0; i < data.players.length; i++) {
        if (data.players[i].finalRank === 0 && data.players[i].action !== 'fold') {
            if (data.players[i].chips - finishedPrev.players[i].chips > amount) {
                index = [];
                amount = data.players[i].chips - finishedPrev.players[i].chips;
                index.push(data.players[i].index);
            } else if (data.players[i].chips - finishedPrev.players[i].chips === amount) {
                index.push(data.players[i].index);
            }
        }
    }
    return index;
}

function blindsTimer(nextBlindsChangeAt, state) {
    updateBlindsTime(nextBlindsChangeAt, state);

    const intervalID = setInterval(function () {
        if (timerBlinds !== intervalID) {
            if (timerBlinds !== -1) {
                window.clearInterval(timerBlinds);
            }
            timerBlinds = intervalID;
        }

        updateBlindsTime(nextBlindsChangeAt, state, intervalID);
    }, 1000);
}

function updateBlindsTime(nextBlindsChangeAt, state, intervalID) {
    if (state !== 'paused') {
        const remaining = nextBlindsChangeAt - Date.now();
        let minutes = parseInt(remaining / 1000 / 60);
        let seconds = parseInt(remaining / 1000 - minutes * 60);
        if (minutes < 10) {
            minutes = '0' + minutes;
        }
        if (seconds < 10) {
            seconds = '0' + seconds;
        }
        if (remaining < 0) {
            minutes = '00';
            seconds = '00';
        }
        $('.level-time span').html(minutes + ':' + seconds);
        if (remaining <= 0) {
            if (intervalID) {
                window.clearInterval(intervalID);
                timerBlinds = -1;
            }
        }
    }
}

const makeLeaderboardTableRow = (name, rank, chips, rebuyCount, state) =>
    `<tr>
        <td>${rank}</td>
        <td>${name}${rebuyCount ? `<div class="leaderboard-rebuys">${rebuyCount}</div>` : ''}</td>
        <td>${state ? state : chips}</td>
    </tr>`;

const updateLeaderboard = data => {
    if (data.roundState !== 'finished' || data.state === 'finished') {
        const players = [data.user, ...data.players].sort((a, b) => {
            if (a.finalRank === b.finalRank) {
                return b.chips - a.chips;
            }
            return a.finalRank - b.finalRank;
        });

        const $leaderboardTable = $('#leaderboard .inside table');
        $leaderboardTable.html('');

        players.forEach((player, idx) => {
            let state;
            if (player.rebuyNextRound) {
                state = player.rebuyCount === 0 ? 'Late reg.' : 'Rebuy';
            } else if (player.chips <= 0) {
                state = 'Busted!'
            }
            $leaderboardTable.append(
                makeLeaderboardTableRow(player.name, player.finalRank || idx + 1, player.chips, player.rebuyCount, state)
            );
        })
    }
};

function updateLastPlayedHand(data) {
    if ((data.roundState !== 'finished' && roundAfterReconnect !== 0) || data.state === 'finished') {
        $('#last-hand-h').html('LAST HAND (' + prevData.round + ')');

        //dealt cards
        const cards = finishedData.cards.split(' ');
        let cardsStr = '';
        if (cards.length >= 3) {
            for (let i = cards.length - 1; i >= 0; i--) {
                cardsStr += '<img src="img/cards/' + cardsSettings + cards[i] + '.svg" width="30" height="47">';
            }
        }
        for (let i = cards.length; i < 5; i++) {
            cardsStr += '<img src="img/cards/unknown.svg" width="30" height="47">';
        }
        if (cards.length === 1) {
            cardsStr += '<img src="img/cards/unknown.svg" width="30" height="47">';
        }
        $('.lh-dealt').html(cardsStr);

        //get players who were in the pot and sort them by winnings
        const pls = [];
        const foldedPls = [];

        if (finishedData.user.action !== 'fold' && finishedData.user.finalRank === 0) {
            pls.push([finishedData.user.lastWin, finishedData.user.cards, finishedData.user.hand, finishedData.user.name]);
            //check uf user folded and showed
        } else if (finishedData.user.action === 'fold' && showedCards === data.round - 1) {
            foldedPls.push([finishedData.user.lastWin, finishedData.user.cards, finishedData.user.hand, finishedData.user.name]);
        }
        for (let i = 0; i < finishedData.players.length; i++) {
            if (finishedData.players[i].action !== 'fold' && finishedData.players[i].finalRank === 0) {
                pls.push([finishedData.players[i].lastWin, finishedData.players[i].cards, finishedData.players[i].hand, finishedData.players[i].name]);
            } else if (finishedData.players[i].action === 'fold' && typeof finishedData.players[i].cards !== 'undefined') {
                foldedPls.push([finishedData.players[i].lastWin, finishedData.players[i].cards, finishedData.players[i].hand, finishedData.players[i].name]);
            }
        }
        pls.sort(function (a, b) {
            return b - a;
        });
        let totalWinnings = 0;
        let plsStr = '';
        for (let i = pls.length - 1; i >= 0; i--) {
            plsStr += '<div class="inside">';

            let winStr = ' ';
            if (pls[i][0] > 0) {
                winStr += 'wins ' + pls[i][0];
                totalWinnings += pls[i][0];
            } else {
                winStr += 'loses';
            }
            if (typeof pls[i][1] === 'undefined' && typeof pls[i][2] === 'undefined') {
                //player made everyone folded
                plsStr += '<div class="lh-messageplayer"><b>' + pls[i][3] + winStr + ' </b><br>w/o showdown</div><div class="lh-cardsplayer"><img src="img/cards/unknown.svg" width="30" height="47"><img src="img/cards/unknown.svg" width="30" height="47"></div>';

            } else if (typeof finishedData.bestCards === 'undefined') {
                //user made everyone folded or player showed cards after everyone folded
                plsStr += '<div class="lh-messageplayer"><b>' + pls[i][3] + winStr + ' </b><br>w/o showdown</div><div class="lh-cardsplayer"><img src="img/cards/' + cardsSettings + pls[i][1].split(' ')[0] + '.svg" width="30" height="47"><img src="img/cards/' + cardsSettings + pls[i][1].split(' ')[1] + '.svg" width="30" height="47"></div>';
            } else {
                plsStr += '<div class="lh-messageplayer"><b>' + pls[i][3] + winStr + ' </b><br>with ' + pls[i][2] + '</div><div class="lh-cardsplayer"><img src="img/cards/' + cardsSettings + pls[i][1].split(' ')[0] + '.svg" width="30" height="47"><img src="img/cards/' + cardsSettings + pls[i][1].split(' ')[1] + '.svg" width="30" height="47"></div>';
            }
            plsStr += '</div>';
        }

        //show players who showed their cards
        for (let i = foldedPls.length - 1; i >= 0; i--) {
            plsStr += '<div class="inside">';
            plsStr += '<div class="lh-messageplayer"><b>' + foldedPls[i][3] + ' </b><br>folds and shows</div><div class="lh-cardsplayer"><img src="img/cards/' + cardsSettings + foldedPls[i][1].split(' ')[0] + '.svg" width="30" height="47"><img src="img/cards/' + cardsSettings + foldedPls[i][1].split(' ')[1] + '.svg" width="30" height="47"></div>';
            plsStr += '</div>';
        }
        $('.last-hand-report').html(plsStr);

        $('.lh-message').html('<b>Cards:</b><br>Pot ' + totalWinnings);
    }
}

function lateRegTimer(maxRebuys, rebuyTime, gameStart, state) {
    updateLateRegTime(maxRebuys, rebuyTime, gameStart, state);

    const intervalID = setInterval(function () {
        if (timerRebuys !== intervalID) {
            if (timerRebuys !== -1) {
                window.clearInterval(timerRebuys);
            }
            timerRebuys = intervalID;
        }

        updateLateRegTime(maxRebuys, rebuyTime, gameStart, state);
    }, 1000);
}

function updateLateRegTime(maxRebuys, rebuyTime, gameStart, state, intervalID) {
    if (state !== 'paused') {
        const lateReg = rebuyTime * 1000 + gameStart;
        const remaining = lateReg - Date.now();
        const hours = parseInt(remaining / 1000 / 60 / 60);
        let minutes = parseInt(remaining / 1000 / 60);
        let seconds = parseInt(remaining / 1000 - minutes * 60);

        const isSmallScreen = $(window).width() < 1024;

        let txt = `${maxRebuys > 0 ? `Rebuys (${maxRebuys}), ` : ''}Late reg. end: `;
        if ($(window).width() < 1024) {
            txt = maxRebuys > 0 ? 'Rebuys: ' : 'Late reg.: ';
        }

        if (minutes < 10) {
            minutes = '0' + minutes;
        }
        if (seconds < 10) {
            seconds = '0' + seconds;
        }

        if (hours < 1) {
            $('.rebuys-late-addon').html(txt + minutes + ':' + seconds);
        } else {
            $('.rebuys-late-addon').html(txt + hours + '.' + (minutes - hours * 60) + ':' + seconds);
        }

        if (remaining <= 0) {
            let endedtxt = `${maxRebuys > 0 ? 'Rebuys, ' : ''}Late reg. period ended.`;
            if (isSmallScreen) {
                endedtxt = maxRebuys > 0 ? 'Rebuys ended.' : 'Late reg. ended.';
            }
            $('.rebuys-late-addon').html(endedtxt);
            if (intervalID) {
                window.clearInterval(intervalID);
                timerRebuys = -1;
            }
        }
    }
}

function showRebuyAndAddonsStats(data) {
    if (data.user.rebuyCount > 0) {
        $('#player1 .player-rebuys')
            .removeClass('disabled')
            .html(data.user.rebuyCount);
    }
    for (let i = 0; i < data.players.length; i++) {
        if (data.players[i].rebuyCount > 0) {
            $('#player' + getPlayerPosition(data, data.players[i].index) + ' .player-rebuys')
                .removeClass('disabled')
                .html(data.players[i].rebuyCount);
        }
    }
}

function assignTags(data) {
    if (prevData && prevData.players.length === data.players.length) {
        const players = [...data.players];
        players.push(data.user);
        const prevPlayers = [...prevData.players];
        prevPlayers.push(prevData.user);
        for (let i = 0; i < players.length; i++) {
            let prevAction = 'new';
            if (typeof prevPlayers[i] !== 'undefined') {
                prevAction = prevPlayers[i].action;
            }
            const action = players[i].action;
            const position = getPlayerPosition(data, players[i].index);
            const $playerTag = $('#player' + position + ' .player-tag');
            $playerTag.removeClass('check call raise');
            if (players[i].chips !== players[i].currentBet) {
                $playerTag
                    .removeClass('allin')
                    .hide();
            }

            const checkLast = prevPlayers[i].currentBet === prevData.targetBet;
            const checkShowdown = data.roundState === 'finished' && players[i].onMove && action === 'check';
            const callLast = prevPlayers[i].currentBet < prevData.targetBet;
            const callShowdown = data.roundState === 'finished' && players[i].onMove && action === 'call';

            //check
            if ((action === 'check' && prevAction !== action) || (action === 'none' && prevPlayers[i].onMove && checkLast) || checkShowdown) {
                $playerTag
                    .addClass('check')
                    .html('Check')
                    .show()
                    .delay(600)
                    .queue(function (n) {
                        $(this).fadeOut();
                        n();
                    });
            }

            //call
            if ((action === 'call' && prevAction !== action) || (action === 'none' && prevPlayers[i].onMove && callLast) || callShowdown) {
                $playerTag
                    .addClass('call')
                    .html('Call')
                    .show()
                    .delay(600)
                    .queue(function (n) {
                        $(this).fadeOut();
                        n();
                    });
            }

            //bet or raise TODO add animation for allin
            if ((action === 'raise' && prevAction !== action)) {
                if (players[i].chips === players[i].currentBet && players[i].finalRank === 0) {
                    $playerTag
                        .addClass('allin')
                        .html('All in')
                        .show();
                } else {
                    $playerTag
                        .addClass('raise')
                        .html(prevData.previousStreetTargetBet === prevData.targetBet ? 'Bet' : 'Raise')
                        .show()
                        .delay(600)
                        .queue(function (n) {
                            $(this).fadeOut();
                            n();
                        });
                }
            }

            if (data.roundState === 'finished') {
                if ($playerTag.hasClass('allin')) {
                    $playerTag
                        .removeClass('allin')
                        .hide();
                }
            }
        }
    }
}

function assignChipsImg(chipcount, player, data) {
    //clear chips from before
    if (player === 'pot') {
        if (player === 'pot' && prevData && prevData.round !== data.round) {
            for (let i = 1; i <= 5; i++) {
                $(`#${player} .stack-${i}`).html('');
            }
        }
        $('#pot').show();
        $('#pot .amount').html(chipcount);
    } else {
        if (chipcount <= 0 || data.roundState === 'finished') {
            $('#' + player + ' .bet .amount').html('');
        } else {
            $('#' + player + ' .bet .amount').html(chipcount);
        }
        for (let i = 1; i <= 5; i++) {
            $(`#${player} .stack-${i}`).html('');
        }
    }

    if (chipcount > 0) {

        // for players, do not render when the round is finished
        if (player === 'pot' || (player !== 'pot' && data.roundState !== 'finished')) {
            const blinds = [1, 5, 20, 100, 500, 1000, 2000, 5000, 10000, 2000, 5000, 10000, 20000, 50000, 100000, 250000, 500000, 1000000];
            for (let i = 1; i <= 5; i++) {
                $(`#${player} .stack-${i}`).html('');
            }

            //find the highest chip you can use
            function findHighestChip(chips) {
                let topBlind = blinds[0];
                let z = 0;
                while (topBlind < chips) {
                    if (z >= blinds.length - 1) {
                        if (topBlind * 2 > chips) {
                            break;
                        }
                        topBlind *= 2;
                    } else {
                        if (blinds[z + 1] > chips) {
                            break;
                        }
                        topBlind = blinds[z + 1];
                    }
                    z++;
                }
                return topBlind;
            }

            let stack = chipcount;
            const chipsArr = [];
            while (stack > 0) {
                const highestChip = findHighestChip(stack);
                let pileCount = 0;
                for (stack; stack >= highestChip; stack = stack - highestChip) {
                    pileCount++;
                }
                chipsArr.push([highestChip, pileCount]);
            }

            let chipsonpile = 0;
            let pile = 1;
            for (let j = 0; j < chipsArr.length; j++) {
                let piles = chipsArr[j][1];
                if (j < 3) {
                    $('#' + player + ' .stack-' + (pile)).append(new Array(++piles).join('<div class="chip" style="background-color: #' + assignColor(chipsArr[j][0]) + ';"></div>'));
                    pile++;
                } else {
                    const maxChipsToAdd = 5 - chipsonpile;
                    const chipOverflow = Math.max(0, chipsArr[j][1] - maxChipsToAdd);
                    let chipsToAdd = chipsArr[j][1] - chipOverflow;
                    chipsonpile += chipsToAdd;
                    chipsArr[j][1] -= chipsToAdd;
                    $('#' + player + ' .stack-' + pile).append(new Array(++chipsToAdd).join('<div class="chip" style="background-color: #' + assignColor(chipsArr[j][0]) + ';"></div>'));
                    if (chipsonpile === 5) {
                        chipsonpile = 0;
                        if (chipsArr[j][1] !== 0) {
                            j--;
                        }
                        pile++;
                    }
                }
            }

            function assignColor(chipNomination) {
                const colors = ['BBCFFF', '176AFC', '00CF75', 'FFCC00', 'FFA329', 'F492F4'];
                //const colors = ['B13BE2', 'F492F4', 'FF5500', 'FFA329', 'FFCC00', '00CF75', '3AEEFC', '2F06FC'];
                const c = jQuery.inArray(chipNomination, blinds);
                if (c < blinds.length) {
                    return colors[c % colors.length];
                }
                return 'ffffff';
            }
        }
    }
}

function setStreet(data) {
    if (data.roundState === 'active') {
        switch (data.cards.split(' ').length) {
            case 1:
                street = 'preflop';
                break;
            case 3:
                street = 'flop';
                break;
            case 4:
                street = 'turn';
                break;
            case 5:
                street = 'river';
                break;
            default:
                street = 'preflop';
        }
    } else if (data.roundState === 'finished') {
        // if a finished round follows a finished round, the new round goes straight into showdown
        if (prevRoundState === 'finished' || !prevData) {
            street = 'preflopShow';
        } else {
            switch (prevData.cards.split(' ').length) {
                case 1:
                    street = 'preflopShow';
                    break;
                case 3:
                    street = 'flopShow';
                    break;
                case 4:
                    street = 'turnShow';
                    break;
                case 5:
                    street = 'riverShow';
                    break;
                default:
                    street = 'preflopShow';
            }
        }
    }

    if (data.state === 'finished') {
        street = 'done';
    }
}

function initializeVars(data) {
    roundTurn++;
    prevStreet = street;
    if (roundTurn === 1) {
        refreshCards();
        //uncheck autoactions if stayed from the last game
        $('#autocheck').prop('checked', false);
        $('#autofold').prop('checked', false);
    }

    $('#game-id').html('<b>Game ID: </b>' + data.uuid);

    if (data.roundState === 'active') {
        $('.showCards').removeClass('showCards');
        winningAnimationInProgress = false;
    }

    if (typeof finishedData !== 'undefined' && data.roundState === finishedData.roundState && data.round === finishedData.round && finishedData.roundState !== 'active') {
        showCardsInProgress = true;
    }

    if (reconnected === true) {
        cardsSettings = localStorage.getItem('suits');
        if (!cardsSettings) {
            cardsSettings = '';
        }
        if (cardsSettings === '4c/') {
            $('#foursuits:checkbox').prop('checked', true);
        } else {
            $('#foursuits:checkbox').prop('checked', false);
        }

        if (localStorage.getItem('sound')) {
            soundOn = true;
            $('#sound:checkbox').prop('checked', true);
        }
    }

    if ((data.roundState === 'finished' || typeof finishedData === 'undefined') && data.state === 'active') {
        if (typeof finishedData === 'undefined' || data.players.length !== finishedData.players.length) {
            finishedPrev = data;
        } else {
            finishedPrev = finishedData;
        }

        finishedData = data;

        roundTurn = 0;
    }

    cardChanged = !prevData || prevData.cards !== data.cards;
    if (cardChanged) {
        play('cards');
    }
}

function showRebuyControls(data) {
    const $player1 = $('#player1');
    const $rebuyBtn = $('#rebuy-btn');
    const $rebuyMsg = $('#rebuyMsg');
    $player1.removeClass('rebuyed');

    if (data.state === 'active') {
        //if he busts, show rebuys
        if (
            data.lateRegistrationPossible &&
            data.config.maxRebuys > data.user.rebuyCount &&
            data.roundState !== 'finished' &&
            data.user.chips === 0 &&
            !data.user.rebuyNextRound
        ) {
            $rebuyBtn.removeClass('disabled').show()
        }

        //the round that he had rebuy
        if (data.user.rebuyNextRound === true) {
            if (data.user.rebuyCount === 0) {
                $rebuyMsg.html('Successfully joined. You will play in the next possible hand.');
            } else {
                $rebuyMsg.html('Rebuy added. You will play in the next possible hand.');
            }
            $rebuyMsg.show();
            $player1.addClass('rebuyed');
            $rebuyBtn
                .removeClass('disabled')
                .hide();
        }

        //after he rebought
        if (data.user.chips > 0) {
            $rebuyMsg.hide();
            $player1.removeClass('rebuyed');
            $rebuyBtn.hide();
        }

        //hide it after the late reg is over
        if (data.lateRegistrationPossible === false) {
            $rebuyBtn.hide();
            $rebuyMsg.hide();
        }
    }
}

function everyoneElseFoldedOrBusted(data) {
    const foldedOrBusted = data.players.filter(player => player.action === 'fold' || player.chips === 0);
    return foldedOrBusted.length === data.players.length;
}

function showResults(data) {
    $('.finished').removeClass('finished folded');
    $('.players .cards').addClass('disabled');
    $('.dealt-cards').addClass('disabled');
    $('.dealer').hide();

    const pls = data.players;
    pls.push(data.user);

    let $player;
    for (let i = 0; i < pls.length; i++) {
        $player = $('#player' + getPlayerPosition(data, pls[i].index));
        if (pls[i].finalRank === 1) {
            $('#player' + getPlayerPosition(data, pls[i].index) + ' .player-chips').html('Winner!');
            $player
                .addClass('winner')
                .append('<img src="img/winner.svg">');
        }

        if (pls[i].finalRank === 2) {
            $('#player' + getPlayerPosition(data, pls[i].index) + ' .player-chips').html('Runner up!');
            $player
                .addClass('runnerup')
                .append('<img src="img/runnerup.svg">');
        }

        if (pls[i].finalRank === 3) {
            $('#player' + getPlayerPosition(data, pls[i].index) + ' .player-chips').html('Potato King!');
            $player
                .addClass('potato')
                .append('<img src="img/potato.svg">');
        }
    }

}

//add a rebuy to player
function addRebuy() {
    rebuy();
    $('#player1').addClass('rebuyed');
    $('#rebuyMsg').show();
    rebuyRound = prevData ? prevData.round : data.round;
}

function playSound(data) {
    if (soundOn) {
        const isSafari = /^((?!chrome|android).)*safari/i.test(navigator.userAgent);
        if (snd != null && isSafari && messageShown === false) {
            const promise = document.querySelector('audio').play();
            if (promise !== undefined) {
                promise.catch(error => {
                    $('.allow-audio').show();
                    messageShown = true;
                }).then(() => {
                    // Auto-play started
                });
            }
        }

        lastAction = getLastAction(data);
        if (reconnected === false) {
            if (prevData.round === data.round) {
                if (lastAction === 'call' || lastAction === 'raise') {
                    play('chips');
                }
                if (lastAction === 'check') {
                    play('check');
                }
            }
        }
    }
}

function getLastAction(data) {
    if (reconnected === false && data.state === 'active') {
        if (data.user.action !== prevData.user.action && data.user.action !== 'none') {
            return data.user.action;
        }
        for (let i = 0; i < data.players.length; i++) {
            if (typeof prevData.players[i] !== 'undefined') {
                if (data.players[i].action !== prevData.players[i].action && data.players[i].action !== 'none') {
                    return data.players[i].action;
                }
            }
        }
        if (cardChanged) {
            if ((prevData.targetBet === prevData.previousStreetTargetBet || data.targetBet === 2 * data.smallBlind)) {
                return 'check';
            } else {
                return 'call';
            }
        } else {
            return 'none';
        }
    } else {
        return 'none';
    }
}

const shouldRefreshView = (data) => {
    if (!prevData || data.state === 'created') {
        return true
    }
    return !isConnectionChangeData(data);
}

// checks if the socket data only inform about a player (dis)connecting
function isConnectionChangeData(data) {
    if (!prevData) {
        return false;
    }

    const sanitizedPrevData = Object.assign({}, prevData);
    sanitizedPrevData.players.forEach(player => delete player.connected);
    delete sanitizedPrevData.time;

    const sanitizedData = Object.assign({}, data)
    sanitizedData.players.forEach(player => delete player.connected);
    delete sanitizedData.time;

    return JSON.stringify(sanitizedPrevData) === JSON.stringify(sanitizedData);
}

//HELPERS

//sort array and remove duplicate values
function sortUnique(arr) {
    if (arr.length === 0) return arr;
    arr = arr.sort(function (a, b) {
        return a * 1 - b * 1;
    });
    const ret = [arr[0]];
    for (let i = 1; i < arr.length; i++) { //Start loop at 1: arr[0] can never be a duplicate
        if (arr[i - 1] !== arr[i]) {
            ret.push(arr[i]);
        }
    }
    return ret;
}

function wait(ms) {
    const d = new Date();
    let d2 = null;
    do {
        d2 = new Date();
    }
    while (d2 - d < ms);
}

//check if the page is minimized or user swicthed tab
(function () {
    let hidden = 'hidden';

    // Standards:
    if (hidden in document)
        document.addEventListener('visibilitychange', onchange);
    else if ((hidden = 'mozHidden') in document)
        document.addEventListener('mozvisibilitychange', onchange);
    else if ((hidden = 'webkitHidden') in document)
        document.addEventListener('webkitvisibilitychange', onchange);
    else if ((hidden = 'msHidden') in document)
        document.addEventListener('msvisibilitychange', onchange);
    // IE 9 and lower:
    else if ('onfocusin' in document)
        document.onfocusin = document.onfocusout = onchange;
    // All others:
    else
        window.onpageshow = window.onpagehide
            = window.onfocus = window.onblur = onchange;

    function onchange(evt) {
        const v = 'visible', h = 'hidden',
            evtMap = {
                focus: v, focusin: v, pageshow: v, blur: h, focusout: h, pagehide: h,
            };

        evt = evt || window.event;
        if (evt.type in evtMap) {

        } else {
            if (this[hidden]) {
                switchedTab = true;
                refreshCards();
            } else {
                switchedTab = false;
            }
        }
    }

    // set the initial state (but only if browser supports the Page Visibility API)
    if (document[hidden] !== undefined)
        onchange({type: document[hidden] ? 'blur' : 'focus'});
})();
