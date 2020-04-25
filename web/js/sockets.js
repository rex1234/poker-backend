var socket = io.connect(location.protocol + '//' + window.location.hostname + ':9092');

//reconnection variable
var reconnected = false;

//data
var finishedData;
var prevData;
var roundTurn = 0;
var roundAfterReconnect = 0;
var cardChanged = false;
var street = "preflop";
var prevStreet = "none";
var prevRoundState = "none";

var timerBlinds = -1;
var timerRebuys = -1;
var showCardsDelay = 0;
var hasRebuyed = false;
var rebuyRound = -1;
var cardsSettings = Cookies.get("suits");


var showCardsInProgress = false;
// inbound events

socket.on('gameState', function (data) {
    Cookies.set('player_uuid', data.user.uuid, { expires: 1 });
    Cookies.set('game_uuid', data.uuid, { expires: 1 });
    Cookies.set('nick', data.user.name, { expires: 100 });
    initializeVars(data);
    //user is in game
    $("#loader").hide();
    loader.pause();
    $("#settings").hide();
    $(".left-container").hide();
    $("#main-screen").hide();
    $(".game-container").show();
    $(".pregame").show();

    if(data.state === "created") {
        $(".all-text").html("Invite other players by sending them this code:<div id='code'>" + data.uuid + "</div><button id='copyButton' onclick='copyToClipboard(document.getElementById(\"code\"))'>Copy code</button>");
        //user is admin
        if(data.user.admin) {
            $(".admin-text").html("You will be able to start the game when there are 2 or more players.");
            if(typeof data.players[0] !== "undefined") {
                $(".admin-text").html("");
                $("#start").show();
            }
        } else {
            $(".admin-text").html("Waiting for admin to start the game.");
        }
    }

    if(data.state === "active" || data.state === "paused") {
        $(".pregame").hide();
        $(".game-info").show();

        //show blinds and othe info
        $(".blinds-state .current").html(data.smallBlind + " / " + data.smallBlind*2);
        if($(window).width() > 1023) {
            $(".blinds-state .next").html(getNextSmallBlind(data.smallBlind) + " / " + getNextSmallBlind(data.smallBlind)*2);
        }
        blindsTimer(data.nextBlinds, data.state);
        lateRegTimer(data.config.rebuyTime, data.gameStart, data.state);
        updateLeaderboard(data);
        assignTags(data);
    }

    if(data.user.admin) {
        if(data.state === "active") {
            if(data.roundState === "finished") {
                $("#pause").removeClass("disabled");
                $("#unpause").addClass("disabled");
            } else {
                $("#pause").addClass("disabled");
            }
        }
        if(data.state === "paused") {
            refreshCards();
            $(".showCards").removeClass("showCards");
            $("#unpause").removeClass("disabled");
            $("#pause").addClass("disabled");
        }
    }

    if(data.state === "finished") {
        $(".game-info").hide();
        $("#pot").hide();
        $(".all-text").html("The game has ended, so fuck off.");
        $(".admin-text").hide();
        $("#start").hide();
    }

    if(data.roundState === "finished") {
        roundAfterReconnect++;
    }

    //if user reconnects, set rebuy round to current round (so user don't show the rebuy button again)
    if(reconnected) {
        rebuyRound = data.round;
    }

    console.log(data);

    printPlayers(data);
    print(data);

    showCardsInProgress = false;
    prevRoundState = data.roundState;
    if(data.roundState !== "finished") {
        prevData = data;
    }


});

socket.on('error', function (data) {
    print(data);
    console.log(data);

    //hide loader if err
    $("#loader").hide();

    if(data.code == 20) { // invalid game UUID
        Cookies.remove('player_uuid');
        Cookies.remove('game_uuid');
    }

    // TODO: show dialog with error message
});

socket.on('gameDisbanded', function () {
    Cookies.set('player_uuid', null);
    print({msg: "game ended"});

    // TODO: show some end message, redirect back to the connection screen
});

socket.on('gameDisbanded', function () {
    Cookies.set('player_uuid', null);
    print({msg: "game ended"});

    // TODO: show some end message, redirect back to the connection screen
});

socket.on('chat', function (data) {
    console.log(data);

    var message = data.message;
    var flash = data.flash; // if the message is a react or regular message

    if(flash) {
        var playerIndex = data.index;
        alert("[" + data.time + "]" + data.name + ": " + message);
    } else {
        // TODO: add message to the chat window
    }
});


// outbound events

function createGame(nickname, gameConfig) {
    socket.emit("connectGame", {
        name: nickname,
        playerUUID: Cookies.get('player_uuid'),
        gameConfig: gameConfig
    });
}

function connectToGame(nickname, gameUuid) {
    socket.emit("connectGame", {
        name: nickname,
        gameUUID: gameUuid,
        playerUUID: Cookies.get('player_uuid')
    });
}

function requestGameState() {
    socket.emit("gameRequest");
}

// player actions

function sendAction(action, numericValue = null, textValue = null) {
    socket.emit("action", {
        action: action,
        numericValue: numericValue,
        textValue: textValue
    });
}

function changeName(name) {
    sendAction("changeName", null, name);
}

function leave() {
    sendAction("leave");

    Cookies.remove('game_uuid');
    Cookies.remove('player_uuid');
    Cookies.remove('io');

    $("#settings").show();
    $(".left-container").show();
    $("#main-screen").show();
    $(".game-container").hide();
    $(".pregame").hide();
}

function gameCall() {
    $("#autocheck").prop("checked", false);
    sendAction("call");
}

function gameCheck() {
    $("#autocheck").prop("checked", false);
    sendAction("check");
}

function gameFold() {
    $("#autofold").prop("checked", false);
    sendAction("fold");
}

function gameRaise(amount) {
    sendAction("raise", amount, null);
}

function rebuy() {
    sendAction("rebuy");
}

function showCards() {
    sendAction("showCards");
}

// Admin actions

function startGame() {
    sendAction("startGame");
}

function kick(playerIndex) {
    sendAction("kick", playerIndex, null);
}

function pause() {
    sendAction("pause", 1, null);
}

function unpause() {
    sendAction("pause", 0, null);
}

// chat

function sendReaction(reaction) {
    socket.emit("chat", {
        message: reaction,
        flash: true
    });
}

function sendChatMessage(msg) {
    socket.emit("chat", {
        message: msg,
        flash: false
    });
}

// MISC

function print(data) {
    //document.getElementById("content").innerHTML = JSON.stringify(data)
}

//TODO refactor, split into multiple functions
function printPlayers(data) {
    giveCSSClasses(data, 1, -1);

    var pot = data.user.currentBet;
    var players = [[1, data.user.dealer]];

    $("#player1 .player-name").html(data.user.name);
    //hacky way to determine what to show when all in and last to act – otherwise you can see who won in advance
    if(data.roundState === "finished" && data.state === "active") {
        if(data.user.onMove) {
            $("#player1 .player-chips").html(Math.max(0, prevData.user.chips - data.targetBet));
        } else {
            $("#player1 .player-chips").html(prevData.user.chips-prevData.user.currentBet);
        }
    } else {
        $("#player1 .player-chips").html(data.user.chips - data.user.currentBet);
    }

    showRebuyControls(data);
    showRebuyAndAddonsStats(data);
    updateLastPlayedHand(data);

    betDesc = data.user.currentBet - data.previousTargetBet;

    assignChipsImg(betDesc, "player1", data);

    if(data.user.cards.length > 0) {
        cards = data.user.cards.split(" ");
        $("#player1 .card-1").html('<img src="img/cards/' + cardsSettings + cards[0] +'.svg"/>');
        $("#player1 .card-2").html('<img src="img/cards/' + cardsSettings + cards[1] +'.svg"/>');
    }

    var positions = [1,0,0,0,0,0,0,0,0];

    if(data.state === "active") {
          if(prevData.user.finalRank !== 0) {
              positions[0] = 0;
          } else if(data.roundState !== "finished" && data.user.finalRank !== 0) {
              positions[0] = 0;
          } else {
          positions[0] = 1;
        }
    }
    if(showCardsInProgress === false) {
        dealCards(data);
    }

    //show cards button
     if((data.roundState === "finished" && data.user.action === "fold") || (everyoneFolded(data) && data.state === "active")) {
        setTimeout( function(){ $("#additional").removeClass("disabled"); }, showCardsDelay );
        $("#additional").html("Show cards");
        $("#additional").attr("onclick","showCards(); $('#player1').addClass('showCards');");
        $('#additional').delay(showCardsDelay+2000).hide(0);
        $('#additional').show();
     } else {
        $("#additional").addClass("disabled");
    }
    showCardsDelay = 0;

    //timer functionality
    if(data.user.onMove) {
        playerCountdown(data.user.moveStart, 1, data.config.playerMoveTime, data.cards);
    }

    for(i = 0; i < data.players.length; i++) {
        var position;
        if(data.user.index < data.players[i].index) {
            position = data.players[i].index - data.user.index + 1;
         } else {
            position = data.players[i].index - data.user.index + 10;
         }

        if(data.state === "active") {
            if(prevData.players[i].finalRank !== 0) {
                positions[position-1] = 0;
            } else if(data.roundState !== "finished" && data.players[i].finalRank !== 0) {
                positions[position-1] = 0;
            } else {
               positions[position-1] = 1;
            }
        }

         players.push([position, data.players[i].dealer]);

        giveCSSClasses(data, position, i);
        $("#player"+ position +" .player-name").html(data.players[i].name);
        //hacky way to determine what to show when all in and last to act – otherwise you can see who won in advance
        if(data.roundState === "finished" && data.state === "active") {

            if(data.players[i].onMove) {
                $("#player"+ position +" .player-chips").html(Math.max(0, prevData.players[i].chips - data.targetBet));
            } else {
                $("#player"+ position +" .player-chips").html(prevData.players[i].chips-prevData.players[i].currentBet);
            }

            //showCard fuctionality
            if(typeof data.players[i].cards !== "undefined") {
                $("#player"+ position).removeClass("fold");
                $("#player"+ position).removeClass("fold");
            }

        } else {
            $("#player"+ position +" .player-chips").html(data.players[i].chips-data.players[i].currentBet);
        }
        var betDesc = data.players[i].currentBet - data.previousTargetBet;
        assignChipsImg(betDesc, "player" + position, data);

        //showdown
        if(typeof data.players[i].cards !== "undefined" && data.roundState === "finished" ) {
            if(data.players[i].cards.length > 0) {
                var cards = data.players[i].cards.split(" ");
                $("#player"+ position +" .card-1").html('<img src="img/cards/'+ cardsSettings + cards[0] +'.svg"/>');
                $("#player"+ position +" .card-2").html('<img src="img/cards/' + cardsSettings + cards[1] +'.svg"/>');
                $("#player"+ position).addClass("showdown");
            }
        } else {
            $("#player"+ position +" .card-1").html("");
            $("#player"+ position +" .card-2").html("");
            $("#player"+ position).removeClass("showdown");
        }

        if(data.players[i].onMove) {
            playerCountdown(data.players[i].moveStart, position, data.config.playerMoveTime, data.cards);
        }

        pot += data.players[i].currentBet;
    }

    //just a ugly hack to get the slider's handle to the left
    $("#range-slider")[0].value = 0;

    //show or hide auto actions
    var autoaction = autoControls(data);

    //show controls
    if(data.user.onMove === true && data.roundState !== "finished" && autoaction === false) {
        showControls(data);
    } else {
        hideControls();
    }

    //hide inactive users
    for(i = 0; i < 9; i++) {
        if(positions[i] !== 1) {
            $("#player"+ (i+1)).addClass("folded");
        } else {
            $("#player"+ (i+1)).removeClass("folded");
        }
    }

    //Determine, who is the Dealer
    players.sort();
    var dealer;
        for(i = 0; i < players.length; i++) {
            if(players[i][1] === true) {
                    dealer = players[i][0];
            }
            $("#player"+ players[i][0] +" .dealer").removeClass("is-dealer");
        }
    $("#player"+ dealer +" .dealer").addClass("is-dealer");

     //at the end of the round, only if the showdown was at the river (no earlier allin runout)
     if(data.roundState === "finished" && typeof data.bestCards !== "undefined" && $(".dealt-cards-5").css('opacity') === "1" && data.state !== "paused") {
        highlightCards(finishedData);
     }

    //hide timers in showdown
    if(data.roundState === "finished") {
        $('.player-timer-running').hide();
    } else {
        $('.player-timer-running').show();
    }


    //display pot
    if(data.roundState === "finished") {
        pot = lastWinSum(data);
    }

    if(pot > 0) {
        assignChipsImg(pot, "pot", data);
    }

}


function showControls(data) {
    if(data.user.currentBet === data.user.chips) {
        gameCall();
    } else {
        //show fold if cannot check
        var currentBet = checkHighestBet(data);

        if(data.targetBet > data.user.currentBet) {
            $("#fold").removeClass("disabled");
        }

        if(data.targetBet === data.user.currentBet) {
            $("#check").removeClass("disabled");
        }


        //show call or check if can
        if(data.targetBet > data.user.currentBet) {
            $("#call").removeClass("disabled");
            $("#call").html("Call<br>"+ Math.max(Math.min(data.user.chips  - (data.user.currentBet - data.previousTargetBet), data.targetBet - data.user.currentBet)));
        }

        //show raise if can
        if(data.targetBet < data.user.chips) {
            $("#raise").removeClass("disabled");

            var chipsBehind = data.user.chips - data.user.currentBet;
            var chipsInPot = data.user.currentBet - data.previousTargetBet;
            var minRaiseFromCurr = getMinRaiseValue(data) - chipsInPot;
            var maxRaiseFromCurr = chipsBehind;
            var raiseBy = Math.min(minRaiseFromCurr, maxRaiseFromCurr);
            var raiseTo = chipsInPot + raiseBy;

            var buttonDesc;

            if(data.targetBet === data.previousTargetBet) {
                buttonDesc = "Bet<br>";
            } else {
                buttonDesc = "Raise to<br>";
            }

             //adjust the raise in the button
             $("#raise").attr("onclick", "gameRaise("+ raiseBy +")");
             $("#raise").html(buttonDesc + raiseTo);

             //if there are not enough chips to raise more, don't show the slider and input
             if(chipsBehind > minRaiseFromCurr) {
                $(".raise-slider").removeClass("disabled");
             }

             //don't show raise if everyone is all in and you cover them
             if(isEveryoneElseAllin(data)) {
                $(".raise-slider").addClass("disabled");
                $("#raise").addClass("disabled");
             }

             //affect slider and input accordingly
             $(".raise-input").attr({
                "min": 1,
                "max": maxRaiseFromCurr,
                "value": raiseTo
             });

             var changingInput = false;
             $(".raise-input").val(raiseTo);
             $(".raise-input").on('keyup', function(e) {
                   //min value is min raise, max is max raise
                   var value = Math.min(maxRaiseFromCurr + chipsInPot, Math.max(minRaiseFromCurr, $(".raise-input").val()));
                   $("#raise").attr("onclick", "gameRaise("+ (value - chipsInPot) +")");

                   //treat empty input as 0
                   $("#raise").html(buttonDesc + value);
                   if (value == '') {
                        value = 0;
                   }
                   changingInput = true;

                   //affect slider
                   $("#range-slider")[0].value = value;
             });

             $("#range-slider").attr({
                "min": raiseTo,
                "max": maxRaiseFromCurr + chipsInPot,
                "value": raiseTo
             });

             $("#range-slider")[0].value = raiseTo;

             $("#range-slider")[0].oninput = function() {
                var value = Math.min(maxRaiseFromCurr + chipsInPot, Math.max(minRaiseFromCurr, this.value));
                //Round to 10s, but exclude max value
                var roundedVal = parseInt(value/10)*10;
                if(roundedVal + 9 >= maxRaiseFromCurr + chipsInPot) {
                    roundedVal = maxRaiseFromCurr + chipsInPot;
                }
                $("#raise").attr("onclick", "gameRaise("+ (roundedVal - chipsInPot) +")");
                $("#raise").html(buttonDesc + roundedVal);
                $(".raise-input").val(roundedVal);
             };

             //change bet sizes buttons
             //if preflop, show 2.5 / 3 / 3.5 Buttons, else 33 / 50 / 66 prc
              $(".betsizes.first").removeClass("disabled");
              $(".betsizes.second").removeClass("disabled");
              $(".betsizes.third").removeClass("disabled");

             if(street === "preflop") {
                $(".betsizes.first").html("2.5BB");
                $(".betsizes.first").attr("onclick", "raiseChange(" + Math.min(5*data.smallBlind, maxRaiseFromCurr + chipsInPot) + ")");
                $(".betsizes.second").html("3BB");
                $(".betsizes.second").attr("onclick", "raiseChange(" + Math.min(6*data.smallBlind, maxRaiseFromCurr + chipsInPot) + ")");
                $(".betsizes.third").html("3.5BB");
                $(".betsizes.third").attr("onclick", "raiseChange(" + Math.min(7*data.smallBlind, maxRaiseFromCurr + chipsInPot) + ")");
                $(".betsizes.last").attr("onclick", "raiseChange(" + (maxRaiseFromCurr + chipsInPot) + ")");
                //hide the buttons if preflop action

                if(data.pot > 3*data.smallBlind) {
                    $(".betsizes.first").addClass("disabled");
                    $(".betsizes.second").addClass("disabled");
                    $(".betsizes.third").addClass("disabled");
                }

             } else {
                 if((parseInt(data.pot/3)) > 2*data.smallBlind) {
                     $(".betsizes.first").html("33%");
                     $(".betsizes.first").attr("onclick", "raiseChange(" + Math.min(parseInt(data.pot/3), maxRaiseFromCurr + chipsInPot) + ")");
                 } else {
                     $(".betsizes.first").addClass("disabled");
                 }
                 $(".betsizes.second").html("50%");
                 $(".betsizes.second").attr("onclick", "raiseChange(" + Math.min(parseInt(data.pot/2), maxRaiseFromCurr + chipsInPot) + ")");
                 $(".betsizes.third").html("66%");
                 $(".betsizes.third").attr("onclick", "raiseChange(" + Math.min(parseInt(2*data.pot/3), maxRaiseFromCurr + chipsInPot) + ")");
                 $(".betsizes.last").attr("onclick", "raiseChange(" + (maxRaiseFromCurr + chipsInPot) + ")");
             }

        }
     }
}

function hideControls() {
    $("#fold").addClass("disabled");
    $("#call").addClass("disabled");
    $("#check").addClass("disabled");
    $("#raise").addClass("disabled");
    $(".raise-slider").addClass("disabled");
}

function autoControls(data) {
    var autoaction = false;

    if($("#autocheck").prop("checked") && data.user.currentBet === checkHighestBet(data)) {
        autoaction = true;
        if(data.user.onMove === true) {
            wait(500);
            gameCheck();
        }
    }

    if($("#autofold").prop("checked")) {
         autoaction = true;
        if(data.user.onMove === true) {
            wait(500);
            gameFold();
        }
     }
     $(".autocheck").addClass("disabled");
     $(".autofold").addClass("disabled");

    if(data.user.onMove === true && data.roundState !== "finished" && autoaction === false) {

    } else {

         //show autofold button when out of turn and cannot check
         if(data.roundState !== "finished" && (data.user.currentBet < checkHighestBet(data)) && data.user.action === "none" && data.user.chips > 0) {
             $(".autofold").removeClass("disabled");
         }

         //show autocheck button when out of turn and can check
         if(data.roundState !== "finished" && (data.user.currentBet >= checkHighestBet(data)) && data.user.action === "none" && data.state === "active") {
             $(".autocheck").removeClass("disabled");
         }
    }

    return autoaction;
}

//checks highest bet on a street
function checkHighestBet(data) {
    var result = data.user.currentBet;
    for(i = 0; i < data.players.length; i++) {
        result = Math.max(result, data.players[i].currentBet);
    }
    return result;
}

//calculates min-raise
function getMinRaiseValue(data) {
    var arr = [data.user.currentBet - data.previousTargetBet];
    for(i = 0; i < data.players.length; i++) {
        arr.push(data.players[i].currentBet - data.previousTargetBet);
    }
    arr = sortUnique(arr);

    //TODO skipping blinds

   if(arr.length <= 1) {
        return Math.max(data.smallBlind*2, arr[0]*2);
    }

    //PREFLOP: everyone called BB
    if(data.user.currentBet === data.smallBlind*2 && arr.length === 1) {
        return data.smallBlind*2;
    }

    //POST FLOP + PREFLOP: basis raise *2 previous jump
    if(arr.length >= 2) {
        return Math.max(4*data.smallBlind, arr[arr.length-1] + (arr[arr.length-1] - arr[arr.length-2]));
    }

}

//returns true if everyone is folded or allin
function isEveryoneElseAllin(data) {
    var rtn = true;
    for(i = 0; i < data.players.length; i++) {
        if(data.players[i].action !== "fold") {
            rtn = rtn && (data.players[i].currentBet === data.players[i].chips);
        }
    }
    return rtn;
}

//show cards
function dealCards(data) {
   var cards = data.cards.split(" ");
    cards.reverse();

    function addFlop() {
        $(".dealt-cards-1").html('<img src="img/cards/' + cardsSettings + cards[0] +'.svg"/>');
        $(".dealt-cards-2").html('<img src="img/cards/' + cardsSettings + cards[1] +'.svg"/>');
        $(".dealt-cards-3").html('<img src="img/cards/' + cardsSettings + cards[2] +'.svg"/>');
    }

    function addTurn() {
        $(".dealt-cards-4").html('<img src="img/cards/' + cardsSettings + cards[3] +'.svg"/>');
    }

    function addRiver() {
        $(".dealt-cards-5").html('<img src="img/cards/' + cardsSettings + cards[4] +'.svg"/>');
    }

     //delete cards from previous game
     if(cards[0] === "") {
         $(".dealt-cards-1").html("");
         $(".dealt-cards-2").html("");
         $(".dealt-cards-3").html("");
         $(".dealt-cards-4").html("");
         $(".dealt-cards-5").html("");
         $(".dealt-cards-5").css('opacity', 0);
         $(".dealt-cards-4").css('opacity', 0);
         $(".dealt-cards-3").css('opacity', 0);
         $(".dealt-cards-2").css('opacity', 0);
         $(".dealt-cards-1").css('opacity', 0);
     }

    if(reconnected) {
        if(street === "flop") {
            addFlop();
            animationFlopInstant();
        }
        if(street === "turn") {
            addFlop();
            addTurn();
            animationFlopInstant();
            animationTurnInstant();
        }
        if(street === "river") {
            addFlop();
            addTurn();
            addRiver();
            animationFlopInstant();
            animationTurnInstant();
            animationRiverInstant();
        }
        $(".dealt-cards-1").css('opacity', 0);
        reconnected = false;
    } else {
        //animate allins streets = preflop allin

        if(street === "preflopShow" && data.cards.length === 14) {
            showCardsDelay = 4000;
            $(".dealt-cards-5").css('opacity', 0);
            $(".dealt-cards-4").css('opacity', 0);
            addFlop();
            addTurn();
            addRiver();
            animationAll();
        }

        if(street === "flopShow" && data.cards.length === 14) {
            showCardsDelay = 2500;
            $(".dealt-cards-4").css('opacity', 0);
            $(".dealt-cards-5").css('opacity', 0);
            addTurn();
            addRiver();
            animationTurnAndRiver();
        }

        if(street === "turnShow" && data.cards.length === 14) {
            showCardsDelay = 1500;
            addRiver();
            animationRiverShowdown();
        }

        if(street === "flop" && cardChanged) {
            addFlop();
            animationFlop();
        }

        if(street === "turn" && cardChanged) {
             addTurn();
             animationTurn();
        }

        if(street === "river" && cardChanged) {
             addRiver();
             animationRiver();
        }
    }
}

//changes input and slider to a value
function raiseChange(value) {
    $('.raise-input').val(value);
    $("#range-slider")[0].value = value;
    $(".raise-input").keyup();
}

//get css classes for certain player
function giveCSSClasses(data, position, i) {
    $("#player" + position).removeClass("created active paused finished ractive rfinished none call raise check fold onMove");

    var onm = false;
    if(position === 1) {
        onm = data.user.onMove;
        $("#player1").addClass(data.state + " r" + data.roundState + " " + data.user.action);
    } else {
        onm = data.players[i].onMove;
        $("#player" + position).addClass(data.state + " r" + data.roundState + " " + data.players[i].action);
    }

    if(onm === true) {
        $("#player" + position).addClass("onMove");
    }
}

//highlight winning cards
function highlightCards(data) {
    //turn off animations for other players when user reconnects
    if(isReconnect(data)) {
        $(".card-1").addClass('notransition');
        $(".card-2").addClass('notransition');
        $(".dealt-cards div").addClass('notransition');
        $(".card-1")[0].offsetHeight; // Trigger a reflow, flushing the CSS changes
        $(".card-2")[0].offsetHeight;
        $(".dealt-cards div")[0].offsetHeight;
    } else {
        $(".card-1").removeClass('notransition');
        $(".card-2").removeClass('notransition');
        $(".dealt-cards div").removeClass('notransition');
    }
        var winners = [];
        var arrPos = [];

        if(data.user.winner === true) {
            winners.push(data.user.index);
            arrPos.push(-1);
        }

        //TODO Replace finalRank by handRank when implemented

        //determine who won
        for(i = 0; i < data.players.length; i++) {
            if(data.players[i].winner === true) {
                winners.push(data.players[i].index);
                arrPos.push(i);
            }
        }

        $(".card-1").addClass("notPlaying");
        $(".card-2").addClass("notPlaying");

        //hide cards in players hands
        for(i = 0; i < winners.length; i++) {
            var position = getPlayerPosition(data, winners[i]);
            var cardsPl;
            var bestPl;

            if(position === 1) {
                cardsPl = data.user.cards.split(" ");
                bestPl = data.user.bestCards.split(" ");
            } else {
                cardsPl = data.players[arrPos[i]].cards.split(" ");
                bestPl = data.players[arrPos[i]].bestCards.split(" ");
            }

            for(k = 0; k < cardsPl.length; k++) {
                var contains = (bestPl[0] === cardsPl[k]) || (bestPl[1] === cardsPl[k]);
                if(contains) {
                    $("#player" + position + " .cards .card-" + (k+1)).removeClass("notPlaying");
                }
            }
        }

        var cards = data.cards.split(" ");
        var bestCards = data.bestCards.split(" ");
        var pos = bestCards.length;
        //partly hide cards that does not won
        for(i = 5; i > 0; i--) {
            if(cards[i-1] !== bestCards[pos-1]) {
                $(".dealt-cards-" + (6-i)).addClass("notPlaying");
            } else if(pos === 0) {
                $(".dealt-cards-" + (6-i)).addClass("notPlaying");
            } else {
                pos--;
            }
        }
}

//resets all css on the cards
function refreshCards() {
    $(".notPlaying").removeClass("notPlaying");
}

//returns true if noone played yet
function noOnePlayed(data) {
    if(data.user.action !== "none" && data.user.action !== "fold") {
        return false;
    }
    for(i = 0; i < data.players.length; i++) {
        if(data.players[i].action !== "none"  && data.players[i].action !== "fold") {
            return false;
        }
    }
    return true;
}

function getPlayerPosition(data, index) {
    var position;
    if(data.user.index === index) {
        return 1;
    }
    if(data.user.index < index) {
        position = index - data.user.index + 1;
    } else {
        position = index - data.user.index + 10;
    }
    return position;
}

//returns next level's blinds
function getNextSmallBlind(blinds) {
    var arr = [10, 20, 30, 50, 75, 100, 150, 250, 400, 600, 800, 1000];
    for(i = 0; i < arr.length - 1; i++) {
        if(blinds === arr[i]) {
            return arr[i+1];
        }
    }
    return blinds*2;
}

function blindsTimer(nextBlinds, state) {
    var intervalID = setInterval(function () {
        if(timerBlinds !== intervalID) {
            if(timerBlinds !== -1) {
                window.clearInterval(timerBlinds);
            }
            timerBlinds = intervalID;
        }

        if(state !== "paused") {
            var remaining = nextBlinds - Date.now();
            var minutes = parseInt(remaining/1000/60);
            var seconds = parseInt(remaining/1000 - minutes*60);
            if(minutes < 10) {
                minutes = "0" + minutes;
            }
            if(seconds < 10) {
                seconds = "0" + seconds;
             }
            $(".level-time span").html(minutes + ":" + seconds);
            if (remaining <= 0) {
                window.clearInterval(intervalID);
                timerBlinds = -1;
            }
        }
    }, 1000);
}

function updateLeaderboard(data) {
    if(data.roundState !== "finished") {
        var pls = [];
        var bustedPls = [];
        for(i = 0; i < prevData.players.length; i++) {
            if(data.players[i].chips > 0) {
                pls.push([data.players[i].chips, data.players[i].name, data.players[i].rebuyCount]);
            } else {
                bustedPls.push([data.players[i].finalRank, data.players[i].name, data.players[i].rebuyCount]);
            }
        }
        if(data.user.chips > 0) {
            pls.push([data.user.chips, data.user.name, data.user.rebuyCount]);
        } else {
            bustedPls.push([data.user.finalRank, data.user.name, data.user.rebuyCount]);
        }
        pls.sort();
        bustedPls.sort();
        $("#leaderboard .inside table").html("");
        for(i = pls.length - 1; i >= 0; i--) {
            var middle = "";
             if(pls[i][2] > 0) {
                middle = "<div class='leaderboard-rebuys'>" + pls[i][2] + "</div>"
             }
            $("#leaderboard .inside table").append("<tr><td>" + (pls.length - i) + "</td><td>" + pls[i][1] + middle + "</td><td>" + pls[i][0] + "</td></tr>");
        }
        for(i = bustedPls.length - 1; i >= 0; i--) {
            var middle = "";
             if(bustedPls[i][2] > 0) {
                middle = "<div class='leaderboard-rebuys'>" + bustedPls[i][2] + "</div>"
             }
            $("#leaderboard .inside table").append("<tr><td>" + (pls.length + bustedPls.length - i) + "</td><td>" + bustedPls[i][1] + middle + "</td><td>Busted!</td></tr>");
        }
    }
}

function updateLastPlayedHand(data) {
    if(data.roundState !== "finished" && roundAfterReconnect !== 0) {
        $("#last-hand-h").html("LAST HAND (" + prevData.round + ")");

        //dealt cards
        var cards = finishedData.cards.split(" ");
        var cardsStr = "";
        if(cards.length >= 3) {
            for(i = cards.length - 1; i >= 0; i--) {
                cardsStr += "<img src='img/cards/"+ cardsSettings + cards[i] + ".svg' width='30' height='47'>";
            }
        }
        for(i = cards.length; i < 5; i++) {
            cardsStr += "<img src='img/cards/unknown.svg' width='30' height='47'>";
        }
        if(cards.length === 1) {
            cardsStr += "<img src='img/cards/unknown.svg' width='30' height='47'>";
        }
        $(".lh-dealt").html(cardsStr);

        //get players who were in the pot and sort them by winnings
        var pls = [];
        if(finishedData.user.action !== "fold") {
            pls.push([finishedData.user.lastWin, finishedData.user.cards, finishedData.user.hand, finishedData.user.name]);
        }
        for(i = 0; i < finishedData.players.length; i++) {
            if(finishedData.players[i].action !== "fold") {
                pls.push([finishedData.players[i].lastWin, finishedData.players[i].cards, finishedData.players[i].hand, finishedData.players[i].name]);
            }
        }
        pls.sort();
        totalWinnings = 0;
        var plsStr = "";
        for(i = pls.length - 1; i >= 0; i--) {
            plsStr += "<div class='inside'>";

            var winStr = " ";
            if(pls[i][0] > 0) {
                winStr += "wins " + pls[i][0];
                totalWinnings += pls[i][0];
            } else {
                winStr += "loses"
            }

            if(typeof pls[i][1] === "undefined" && typeof pls[i][2] === "undefined") {
                //player made everyone folded
                plsStr += "<div class='lh-messageplayer'><b>"+ pls[i][3] + winStr +" </b><br>w/o showdown</div><div class='lh-cardsplayer'><img src='img/cards/unknown.svg' width='30' height='47'><img src='img/cards/unknown.svg' width='30' height='47'></div>";

            } else if (typeof pls[i][2] === "undefined") {
                //user made everyone folded or player showed cards after everyone folded
                plsStr += "<div class='lh-messageplayer'><b>"+ pls[i][3] + winStr +" </b><br>w/o showdown</div><div class='lh-cardsplayer'><img src='img/cards/" + cardsSettings + pls[i][1].split(" ")[0] + ".svg' width='30' height='47'><img src='img/cards/" + cardsSettings + pls[i][1].split(" ")[1] + ".svg' width='30' height='47'></div>";

            } else {
                plsStr += "<div class='lh-messageplayer'><b>"+ pls[i][3] + winStr +" </b><br>with " + pls[i][2] + "</div><div class='lh-cardsplayer'><img src='img/cards/" + cardsSettings + pls[i][1].split(" ")[0] + ".svg' width='30' height='47'><img src='img/cards/" + cardsSettings + pls[i][1].split(" ")[1] + ".svg' width='30' height='47'></div>";
            }

            plsStr += "</div>";

        }
        $(".last-hand-report").html(plsStr);

        $(".lh-message").html("<b>Cards:</b><br>Pot " + totalWinnings);
    }
}

function lateRegTimer(rebuyTime, gameStart, state) {
    var intervalID = setInterval(function () {
        if(timerRebuys !== intervalID) {
            if(timerRebuys !== -1) {
                window.clearInterval(timerRebuys);
            }
            timerRebuys = intervalID;
        }

        if(state !== "paused") {
             var lateReg = rebuyTime*1000 + gameStart;
             var remaining = lateReg - Date.now();
             var hours = parseInt(remaining/1000/60/60);
             var minutes = parseInt(remaining/1000/60);
             var seconds = parseInt(remaining/1000 - minutes*60);

             var txt = "Rebuy, Late reg. end: ";
             var endedtxt = "Rebuy, Late reg. period ended.";
             if($(window).width() < 1024) {
              txt = "Rebuy: "
              endedtxt = "Rebuys ended."
             }

             if(minutes < 10) {
                 minutes = "0" + minutes;
             }
             if(seconds < 10) {
                 seconds = "0" + seconds;
              }
              if(hours < 1) {
                 $(".rebuys-late-addon").html(txt + minutes + ":" + seconds);
             } else {
                 $(".rebuys-late-addon").html(txt + hours + "." + (minutes - hours*60) + ":" + seconds);
             }

             if (remaining <= 0) {
                  $(".rebuys-late-addon").html(endedtxt);
                 window.clearInterval(intervalID);
                 timerRebuys = -1;
             }
         }
    }, 1000);
}

function showRebuyAndAddonsStats(data) {
    if(data.user.rebuyCount > 0) {
        $("#player1 .player-rebuys").removeClass("disabled");
        $("#player1 .player-rebuys").html(data.user.rebuyCount);
    }
    for(i = 0; i < data.players.length; i++) {
        if(data.players[i].rebuyCount > 0) {
                $("#player" + getPlayerPosition(data, data.players[i].index) + " .player-rebuys").removeClass("disabled");
                $("#player" + getPlayerPosition(data, data.players[i].index) + " .player-rebuys").html(data.players[i].rebuyCount);
        }
    }
}

function assignTags(data) {
    if (isReconnect(data) === false) {
        var players = [...data.players];
        players.push(data.user);
        var prevPlayers = [...prevData.players];
        prevPlayers.push(prevData.user);
        for(i = 0; i < players.length; i++) {
            var prevAction = prevPlayers[i].action;
            var action = players[i].action;
            var position = getPlayerPosition(data, players[i].index);
            $("#player" + position + " .player-tag").removeClass("check call raise");
            if(players[i].chips !== players[i].currentBet) {
                $("#player" + position + " .player-tag").removeClass("allin");
                $("#player" + position + " .player-tag").hide();
            }

            var checkLast = prevPlayers[i].currentBet === prevData.targetBet;
            var checkShowdown = data.roundState === "finished" && players[i].onMove && action === "check";
            var callLast = prevPlayers[i].currentBet < prevData.targetBet;
            var callShowdown = data.roundState === "finished" && players[i].onMove && action === "call";

            //check
            if((action === "check" && prevAction !== action) || (action === "none" && prevPlayers[i].onMove && checkLast) || checkShowdown) {
                $("#player" + position + " .player-tag").addClass("check");
                $("#player" + position + " .player-tag").show();
                $("#player" + position + " .player-tag").show().delay(600).queue(function(n) {
                  $(this).fadeOut(); n();
                });
                $("#player" + position + " .player-tag").html("Check");
            }

            //call
            if((action === "call" && prevAction !== action) || (action === "none" && prevPlayers[i].onMove && callLast) || callShowdown) {
                  $("#player" + position + " .player-tag").addClass("call");
                  $("#player" + position + " .player-tag").show();
                  $("#player" + position + " .player-tag").show().delay(600).queue(function(n) {
                    $(this).fadeOut(); n();
                  });
                  $("#player" + position + " .player-tag").html("Call");
            }

            //bet or raise TODO add animation for allin
           if((action === "raise" && prevAction !== action)) {
               if(players[i].chips === players[i].currentBet && players[i].finalRank === 0) {
                     $("#player" + position + " .player-tag").addClass("allin");
                     $("#player" + position + " .player-tag").show();
                     $("#player" + position + " .player-tag").html("allin");
               } else {
                    $("#player" + position + " .player-tag").addClass("raise");
                    $("#player" + position + " .player-tag").show();
                    $("#player" + position + " .player-tag").show().delay(600).queue(function(n) {
                      $(this).fadeOut(); n();
                    });
                    if(prevData.previousTargetBet === prevData.targetBet) {
                        $("#player" + position + " .player-tag").html("Bet");
                    } else {
                        $("#player" + position + " .player-tag").html("Raise");
                    }
               }
           }

           if(data.roundState === "finished") {
                if($("#player" + position + " .player-tag").hasClass("allin")) {
                    $("#player" + position + " .player-tag").removeClass("allin");
                    $("#player" + position + " .player-tag").hide();
                }
           }
            //allin
        }
    }
}

function assignChipsImg(chipcount, player, data) {
    //clear chips from before
    if(player === "pot") {
          $("#" + player + " .amount").html("Pot: "+ chipcount);
          if(player === "pot" && prevData.round !== data.round) {
                $("#" + player + " .stack-1").html(""); $("#" + player + " .stack-2").html(""); $("#" + player + " .stack-3").html(""); $("#" + player + " .stack-4").html(""); $("#" + player + " .stack-5").html("");
          }
    } else {
        if(chipcount <= 0) {
            $("#" + player + " .bet .amount").html("");
        } else {
            $("#" + player + " .bet .amount").html(chipcount);
        }
          $("#" + player + " .stack-1").html(""); $("#" + player + " .stack-2").html(""); $("#" + player + " .stack-3").html(""); $("#" + player + " .stack-4").html(""); $("#" + player + " .stack-5").html("");
    }

    if(chipcount > 0) {

        //for pot, change only when street changed
        if(player !== "pot" || (player === "pot" && street != prevStreet && street != "preflop")) {
             var blinds = [1, 5, 20, 100, 500, 1000, 2000, 5000, 10000, 2000, 5000, 10000, 20000, 50000, 100000, 250000, 500000, 1000000];
             $("#" + player + " .stack-1").html(""); $("#" + player + " .stack-2").html(""); $("#" + player + " .stack-3").html(""); $("#" + player + " .stack-4").html(""); $("#" + player + " .stack-5").html("");

             //find the highest chip you can use
             function findHighestChip(chips) {
                 var topBlind = blinds[0];
                 var z = 0;
                 while (topBlind < chips) {
                     if (z >= blinds.length-1) {
                         if(topBlind*2 > chips) {
                             break;
                         }
                         topBlind *= 2;
                     } else {
                         if(blinds[z+1] > chips) {
                             break;
                         }
                         topBlind = blinds[z+1];
                     }
                     z++;
                 }
                 return topBlind;
             }

             var stack = chipcount;
             var chipsArr = [];
             while(stack > 0) {
                 var highestChip = findHighestChip(stack);
                 var pileCount = 0;
                 for(stack; stack >= highestChip; stack = stack-highestChip) {
                     pileCount++;
                 }
                 chipsArr.push([highestChip, pileCount]);
             }

             var chipsonpile = 0;
             var pile = 1;
             for(j = 0; j < chipsArr.length; j++) {
                 var piles = chipsArr[j][1];
                 if(j < 3) {
                     $("#" + player + " .stack-" + (pile)).append(new Array(++piles).join('<div class="chip" style="background-color: #'+assignColor(chipsArr[j][0])+ ';"></div>'));
                     pile++;
                 } else {
                     var maxChipsToAdd = 5 - chipsonpile;
                     var chipOverflow = Math.max(0, chipsArr[j][1] - maxChipsToAdd);
                     var chipsToAdd = chipsArr[j][1] - chipOverflow;
                     chipsonpile += chipsToAdd;
                     chipsArr[j][1] -= chipsToAdd;
                     $("#" + player + " .stack-" + pile).append(new Array(++chipsToAdd).join('<div class="chip" style="background-color: #'+assignColor(chipsArr[j][0])+ ';"></div>'));
                     if(chipsonpile === 5) {
                         chipsonpile = 0;
                         if(chipsArr[j][1] !== 0) {
                             j--;
                         }
                         pile++;
                     }
                 }
             }

             function assignColor(chipNomination) {
                 var colors = ["BBCFFF", "176AFC", "00CF75", "FFCC00", "FFA329", "F492F4" ];
                 //var colors = ["B13BE2", "F492F4", "FF5500", "FFA329", "FFCC00", "00CF75", "3AEEFC", "2F06FC"];
                 var c = jQuery.inArray(chipNomination, blinds);
                 if(c < blinds.length) {
                     return colors[c % colors.length];
                 }
                 return "ffffff";
             }
        }
    }
}

function lastOnMove(data) {
    return 0;
}

function isAllInShowdown(data) {
    var peopleAllIn = 0;
    var peopleActive = 0;
    if(prevData.user.action !== "fold") {
        peopleActive++;
        if(data.user.chips === data.user.currentBet) {
            peopleAllIn++;
        }
    }
    for(i =0; i < data.players[i]; i++) {
        if(prevData.players[i].action !== "fold") {
            peopleActive++;
            if(data.players[i].chips === data.players[i].currentBet) {
                peopleAllIn++;
            }
        }
    }
    if(peopleActive - peopleAllIn <= 1) {
        return true;
    } else {
        return false;
    }
}

function initializeVars(data) {
    roundTurn++;
    prevStreet = street;
    if(roundTurn === 1) {
        refreshCards();
    }

    if(data.roundState === "active") {
        $(".showCards").removeClass("showCards");
    }

    if(typeof finishedData !== "undefined" && data.roundState === finishedData.roundState && data.round === finishedData.round && finishedData.roundState !== "active") {
        showCardsInProgress = true;
    }

    if(reconnected === true) {
        if(typeof cardsSettings === "undefined") {
            cardsSettings = "";
        }
        console.log(cardsSettings);
        if(cardsSettings === "4c/") {
            $("#foursuits:checkbox").prop("checked", true);
        } else {
            $("#foursuits:checkbox").prop("checked", false);
        }
    }

    if((data.roundState === "finished" || typeof finishedData === "undefined") && data.state === "active") {
        finishedData = data;
        roundTurn = 0;
    }

    if(typeof prevData === "undefined") {
        prevData = data;
    }

    cardChanged = (prevData.cards !== data.cards);
    if (data.roundState === "active") {
        var x;
            x = data.cards.split(" ").length;
         switch(x) {
           case 1:
             street = "preflop";
             break;
           case 3:
             street = "flop";
             break;
           case 4:
             street = "turn";
             break;
           case 5:
             street = "river";
             break;
         }
    } else if (data.roundState === "finished"){
        var x;
            x = prevData.cards.split(" ").length;
         switch(x) {
           case 1:
             street = "preflopShow";
             break;
           case 3:
             street = "flopShow";
             break;
           case 4:
             street = "turnShow";
             break;
           case 5:
             street = "riverShow";
             break;
           default:
             street = "preflop";
         }
    }
    if(data.state === "finished") {
        street = "done";
    }

}
function showRebuyControls(data) {
    $("#player1").removeClass("rebuyed");

    if(data.state === "active") {
    //if he busts, show rebuys
        if(data.roundState !== "finished" && data.user.chips === 0 && data.lateRegistrationEnabled === true && data.user.rebuyNextRound === false) {
            $("#rebuys").removeClass("disabled");
            $("#rebuys").addClass("rebuyed");
            $("#rebuys").show();
            $("#rebuys").html("Rebuy");
            $("#rebuys").attr("onclick","addRebuy();");
        }

        //the round that he had rebuy
        if(data.user.rebuyNextRound === true) {
            $("#rebuyMsg").show();
            $("#player1").addClass("rebuyed");
            $("#rebuys").removeClass("disabled");
            $("#rebuys").hide();
        }

        //after he rebought
        if(data.user.chips > 0) {
            $("#rebuyMsg").hide();
            $("#player1").removeClass("rebuyed");
            $("#rebuys").hide();
        }
    }
}

function everyoneFolded(data) {
    for(i = 0; i < data.players.length; i++) {
        if(data.players[i].action !== "fold") {
            return false;
        }
    }

    return true;
}

//add a rebuy to player
function addRebuy() {
    rebuy();
    $("#player1").addClass("rebuyed");
    $("#rebuyMsg").show();
    rebuyRound = prevData.round;
}

//checks if the socket change is only in reconnection var
function isReconnect(data) {
    if(data.state === "created") {
        return false;
    }
    if (prevStreet !== street) {
        return false;
    }
    if (data.roundState !== prevRoundState) {
        return false;
    }
    if(data.user.onMove !== prevData.user.onMove) {
        return false;
    }
    for(i = 0; i < data.players.length; i++) {
        if(data.players[i].onMove !== prevData.players[i].onMove) {
            return false;
        }
    }
    return true;
}

//sums all last wins
function lastWinSum(data) {
    var lastWin = data.user.lastWin;
    for(i = 0; i < data.players.length; i++){
        lastWin += data.players[i].lastWin;
    }
    return lastWin;
}

//HELPERS

//sort array and remove duplicate values
function sortUnique(arr) {
  if (arr.length === 0) return arr;
  arr = arr.sort(function (a, b) { return a*1 - b*1; });
  var ret = [arr[0]];
  for (var i = 1; i < arr.length; i++) { //Start loop at 1: arr[0] can never be a duplicate
    if (arr[i-1] !== arr[i]) {
      ret.push(arr[i]);
    }
  }
  return ret;
}

function wait(ms) {
    var d = new Date();
    var d2 = null;
    do { d2 = new Date(); }
    while(d2-d < ms);
}
