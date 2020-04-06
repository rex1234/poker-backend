var socket = io.connect('http://127.0.0.1:9092');

// inbound events

socket.on('gameState', function (data) {
    Cookies.set('player_uuid', data.user.uuid, { expires: 1 });
    console.log(data);
    printPlayers(data);
    print(data);


    // TODO: redraw game board
});

socket.on('error', function (data) {
    print(data);
    console.log(data);

    // TODO: show dialog with error message
});

socket.on('gameDisbanded', function () {
    Cookies.set('player_uuid', null);
    print({msg: "game ended"});

    // TODO: show some end message, redirect back to the connection screen
});

// outbound events

function createGame(nickname) {
    socket.emit("connectGame", {
        name: nickname,
        playerUUID: Cookies.get('player_uuid'),
        gameConfig: {
            startingChips: 25000,
            startingBlinds: 20,
            blindIncreaseTime: 720,
            playerMoveTime: 12,
        }
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

function sendAction(action, numericValue, textValue) {
    socket.emit("action", {
        action: action,
        numericValue: numericValue,
        textValue: textValue
    });
}

function changeName(name) {
    sendAction("changeName", null, name)
}

function gameCall() {
    sendAction("call", null, null)
}

function gameCheck() {
    sendAction("check", null, null)
}

function gameFold() {
    sendAction("fold", null, null)
}

function gameRaise(amount) {
    sendAction("raise", amount, null)
}

function rebuy() {
    sendAction("rebuy", null, null)
}

function showCards() {
    sendAction("showCards", null, null)
}

// Admin actions

function startGame() {
    sendAction("startGame", null, null)
}

function kick(playerIndex) {
    sendAction("kick", playerIndex, null)
}

function pause() {
    sendAction("pause", 1, null)
}

function unpause() {
    sendAction("pause", 0, null)
}


function print(data) {
    //document.getElementById("content").innerHTML = JSON.stringify(data)
}

function printPlayers(data) {
    var pot = data.user.currentBet;
    var players = [[1, data.user.currentBet === data.smallBlind]];

    $("#player1 .player-name").html(data.user.name);
    $("#player1 .player-chips").html(data.user.chips-data.user.currentBet);
    $("#player1 .bet").html(data.user.currentBet);
    if(data.user.cards.length > 0) {
        var cards = data.user.cards.split(" ");
        $("#player1 .card-1").html(cards[0]);
        $("#player1 .card-2").html(cards[1]);
    }
    if(data.user.onMove) {
        $("#player1").addClass("active");
    } else {
         $("#player1").removeClass("active");
    }

    var positions = [1,0,0,0,0,0,0,0,0];

    for(i = 0; i < data.players.length; i++) {
        var position;
        if(data.user.index < data.players[i].index) {
            position = data.players[i].index - data.user.index + 1;
         } else {
            position = data.players[i].index - data.user.index + 10;
         }
         positions[position-1] = 1;

         players.push([position, data.players[i].currentBet === data.smallBlind]);
         if(data.players[i].onMove) {
                $("#player"+ position +" .player-name").addClass("active");
         } else {
                $("#player"+ position +" .player-name").removeClass("active");
         }
        $("#player"+ position +" .player-name").html(data.players[i].name);
        $("#player"+ position +" .player-chips").html(data.players[i].chips-data.players[i].currentBet);
        $("#player"+ position +" .bet").html(data.players[i].currentBet);
        if(data.players[i].cards != null) {
            if(data.players[i].cards.length > 0) {
                var cards = data.players[i].cards.split(" ");
                $("#player"+ position +" .card-1").html(cards[0]);
                $("#player"+ position +" .card-2").html(cards[1]);
            }
        }

        pot += data.players[i].currentBet;
    }

    //show controls
    if(data.user.onMove === true) {
        //show fold if cannot check
        var currentBet = checkCurrentBet(data);

        if(currentBet > data.user.currentBet) {
            $("#fold").removeClass("disabled");
        }

        //show check if can
        if(currentBet === data.user.currentBet) {
             $("#check").removeClass("disabled");
        }

        //show call if can TODO
        if(currentBet > data.user.currentBet) {
             $("#call").removeClass("disabled");
            $("#call").html("Call<br>"+ (currentBet - data.user.currentBet));
        }

        //show raise if can TODO
        if(currentBet < data.user.chips) {
             $("#raise").removeClass("disabled");
             var minRaise = getMinRaiseValue(data);
             $("#raise").attr("onclick", "gameRaise("+ (minRaise - data.user.currentBet) +")");
             $("#raise").html("Raise to<br>"+minRaise);
             $(".raise-slider").removeClass("disabled");

             $(".raise-input").attr({
                "min": minRaise,
                "max": data.user.chips,
                "value": minRaise
             });

             var attributes = {
                 min: minRaise,
                 max: data.user.chips,
                 step: minRaise
               };
             $('input[type="range"]').attr(attributes);
             $('input[type="range"]').rangeslider('update', true);

        }

    } else {
            $("#fold").addClass("disabled");
            $("#call").addClass("disabled");
            $("#check").addClass("disabled");
            $("#raise").addClass("disabled");
            $(".raise-slider").addClass("disabled");
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
        if(i+1 >= players.length && players[i][1] === true) {
              dealer = players[0][0];
         } else if(players[i][1] === true) {
              dealer = players[i+1][0];
         }
        if(players[i][1] === true) {
            $("#player"+ players[i][0] +" .dealer").removeClass("is-dealer");
        }
    }
    $("#player"+ dealer +" .dealer").addClass("is-dealer");


     $(".dealt-cards").html(data.cards);
     $(".pot").html(pot);
}

//checks current bet on a street
function checkCurrentBet(data) {
    var result = 0;
    for(i = 0; i < data.players.length; i++) {
        result = Math.max(result, data.players[i].currentBet);
    }
    return result;
}

//calculates min-raise
function getMinRaiseValue(data) {
    var arr = [data.user.currentBet];
    for(i = 0; i < data.players.length; i++) {
        arr.push(data.players[i].currentBet);
    }
    arr.sort();
    return arr[arr.length-1] - arr[arr.length-2];
}

//returns 0 if preflop
//returns bet on the previous street per player still in the game
function previousStreetBet(data) {
    var result = 0;
    for(i = 0; i < data.players.length; i++) {
        data.players[i].
        result = Math.max(result, data.players[i].currentBet);
    }
}

