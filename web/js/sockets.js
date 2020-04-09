var socket = io.connect('http://' + window.location.hostname + ':9092');

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

function createGame(nickname, sc, sb, bi, pm, rt) {
    socket.emit("connectGame", {
        name: nickname,
        playerUUID: Cookies.get('player_uuid'),
        gameConfig: {
            startingChips: sc,
            startingBlinds: sb,
            blindIncreaseTime: bi,
            playerMoveTime: pm,
            rebuyTime: rt
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

//TODO refactor, split into multiple functions
function printPlayers(data) {
    giveCSSClasses(data, 1, -1);

    var pot = data.user.currentBet;
    var players = [[1, data.user.dealer]];

    $("#player1 .player-name").html(data.user.name);
    $("#player1 .player-chips").html(data.user.chips - data.user.currentBet);
    $("#player1 .bet").html(data.user.currentBet - data.previousTargetBet);
    if(data.user.cards.length > 0) {
        var cards = data.user.cards.split(" ");
        $("#player1 .card-1").html('<img src="img/cards/' + cards[0] +'.svg"/>');
        $("#player1 .card-2").html('<img src="img/cards/' +cards[1] +'.svg"/>');
    }

    var positions = [1,0,0,0,0,0,0,0,0];

    //timer functionality
    if(data.user.onMove) {
        playerCountdown(data.user.moveStart, 1, data.cards);
    }

    for(i = 0; i < data.players.length; i++) {
        if(data.state === "created" && data.user.admin) {
            $("#start").show();
        } else {
            $(".pregame").hide();
        }
        var position;
        if(data.user.index < data.players[i].index) {
            position = data.players[i].index - data.user.index + 1;
         } else {
            position = data.players[i].index - data.user.index + 10;
         }
         positions[position-1] = 1;

         players.push([position, data.players[i].dealer]);

        giveCSSClasses(data, position, i);
        $("#player"+ position +" .player-name").html(data.players[i].name);
        $("#player"+ position +" .player-chips").html(data.players[i].chips-data.players[i].currentBet);
        $("#player"+ position +" .bet").html(data.players[i].currentBet - data.previousTargetBet);
        if(data.players[i].cards != null) {
            if(data.players[i].cards.length > 0) {
                var cards = data.players[i].cards.split(" ");
                $("#player"+ position +" .card-1").html(cards[0]);
                $("#player"+ position +" .card-2").html(cards[1]);
            }
        }

        if(data.players[i].onMove) {
            playerCountdown(data.players[i].moveStart, position, data.cards);
        }

        pot += data.players[i].currentBet;
    }

    //show controls
    if(data.user.onMove === true && data.roundState !== "finished") {
        //show fold if cannot check
        var currentBet = checkHighestBet(data);

        if(currentBet > data.user.currentBet) {
            $("#fold").removeClass("disabled");
        }

        //show check if can
        if(currentBet === data.user.currentBet) {
             $("#check").removeClass("disabled");
        }

        //show call if can
        if(currentBet > data.user.currentBet) {
             $("#call").removeClass("disabled");
            $("#call").html("Call<br>"+ (currentBet - data.user.currentBet));
        }

        //show raise if can
        if(currentBet < data.user.chips) {
             $("#raise").removeClass("disabled");
             var raiseTo = getMinRaiseValue(data);
             var raiseBy = raiseTo - (data.user.currentBet - data.previousTargetBet);

             var buttonDesc;

             var check = (data.targetBet === data.previousTargetBet);

             console.log(
                "targetBet: " + data.targetBet +
                ",  previousTargetBet: " + data.previousTargetBet +
                ",  check: " + check);

             if(check) {
                buttonDesc = "Bet<br>";
             } else {
                 buttonDesc = "Raise to<br>";
             }

              $("#raise").attr("onclick", "gameRaise("+ raiseBy +")");
              $("#raise").html(buttonDesc + raiseTo);
              $(".raise-slider").removeClass("disabled");

            //affect slider and input accordingly
             $(".raise-input").attr({
                "min": 1,
                "max": data.user.chips - data.user.currentBet,
                "value": raiseTo
             });

             $(".raise-input").val(raiseTo);

             $(".raise-input").on('keyup', function(e) {
               var $inputRange = $('[data-rangeslider]', e.target.parentNode);
               var value = $('input[type="number"]', e.target.parentNode)[0].value;
               $("#raise").attr("onclick", "gameRaise("+ Math.max(raiseTo , value) - data.user.currentBet +")");
               $("#raise").html(buttonDesc + value);
               if (value == '') {
                    value = 0;
               }
               $inputRange .val(value) .change();
             });

             //Slider + input functionality

             $('input[type="range"]').rangeslider({
               polyfill : false,
               onInit : function() {
                 $("#raise").html( buttonDesc + this.$element.val() );
                 $(".raise-input").val(this.$element.val());
               },
                onSlide : function( position, value) {
                                $("#raise").html(buttonDesc + value);
                                if($(".raise-input").is(":focus") === false) {
                                     $(".raise-input").val(value);
                                }
                                $("#raise").attr("onclick", "gameRaise("+ (value - $("#player1 .bet").html()) +")");
                            },
              });

             $('.raise-slider .slider').hover(function() {
               $(".raise-input").blur();
             });

            var attributes = {
                 min: raiseTo,
                 max: data.user.chips - data.user.currentBet,
                 step: 1
               };
             $('input[type="range"]').attr(attributes);
             $('input[type="range"]').val(raiseTo).change();
             $('input[type="range"]').rangeslider('update', true);

        }

    } else {
            $("#fold").addClass("disabled");
            $("#call").addClass("disabled");
            $("#check").addClass("disabled");
            $("#raise").addClass("disabled");
            $(".raise-slider").addClass("disabled");
    }

    //showdown
    if(data.roundState === "finished") {

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


    var cards = data.cards.split(" ");
    cards.reverse();
    console.log(cards.length);

    //delete cards from previous game
    if(cards[0] === "") {
        $(".dealt-cards-1").html("");
        $(".dealt-cards-2").html("");
        $(".dealt-cards-3").html("");
        $(".dealt-cards-4").html("");
        $(".dealt-cards-5").html("");
    }

     if(cards[2] !== undefined) {
        $(".dealt-cards-1").html('<img src="img/cards/' + cards[0] +'.svg"/>');
        $(".dealt-cards-2").html('<img src="img/cards/' + cards[1] +'.svg"/>');
        $(".dealt-cards-3").html('<img src="img/cards/' + cards[2] +'.svg"/>');
      }

     if(cards[3] !== undefined) { $(".dealt-cards-4").html('<img src="img/cards/' + cards[3] +'.svg"/>'); }
     if(cards[4] !== undefined) { $(".dealt-cards-5").html('<img src="img/cards/' + cards[4] +'.svg"/>'); }
     $(".pot").html(pot);
}

//checks highest bet on a street
function checkHighestBet(data) {
    var result = 0;
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

    console.log(arr);

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

//get css classes for certain player
function giveCSSClasses(data, position, i) {
    $("#player" + position).removeClass("created active paused finished ractive rfinished none call raise check fold onMove");

    var onm = false;
    if(position === 1) {
        onm = data.user.onMove;
        $("#player1").addClass(data.state + " r" + data.RoundState + " " + data.user.action);
    } else {
        onm = data.players[i].onMove;
        $("#player" + position).addClass(data.state + " r" + data.roundState + " " + data.players[i].action);
    }

    if(onm === true) {
        $("#player" + position).addClass("onMove");
    };
}

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
