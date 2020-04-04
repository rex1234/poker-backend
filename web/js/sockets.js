var socket = io.connect('http://127.0.0.1:9092');

// inbound events

socket.on('gameState', function (data) {
    Cookies.set('player_uuid', data.user.uuid, { expires: 1 });

    printPlayers(data);
    print(data);
    console.log(data);

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

function startGame() {
    sendAction("startGame", null, null)
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

function showCards() {
    sendAction("showCards", null, null)
}


function print(data) {
    document.getElementById("content").innerHTML = JSON.stringify(data)
}

function printPlayers(data) {
    var pot = data.user.currentBet;
    $(".player1 .name").html(data.user.name);
    if(data.user.onMove) {
        $(".player1 .name").addClass("onMove");
    } else {
        $(".player1 .name").removeClass("onMove");
    }
    $(".player1 .chips").html(data.user.chips);
    $(".player1 .bet").html(data.user.currentBet);
    $(".player1 .cards").html(data.user.cards);
    for(i = 0; i < data.players.length; i++) {
        var position;
        if(data.user.index < data.players[i].index) {
            position = data.players[i].index - data.user.index + 1;
         } else {
            position = data.players[i].index - data.user.index + 10;
         }
         if(data.players[i].onMove) {
                $(".player"+ position +" .name").addClass("onMove");
         } else {
                $(".player"+ position +" .name").removeClass("onMove");
         }
        $(".player"+ position +" .name").html(data.players[i].name);
        $(".player"+ position +" .chips").html(data.players[i].chips);
        $(".player"+ position +" .bet").html(data.players[i].currentBet);
        $(".player"+ position +" .cards").html(data.players[i].cards);
        pot += data.players[i].currentBet;
    }

     $("#dealtCards").html(data.cards);
     $("#pot").html(pot);
}