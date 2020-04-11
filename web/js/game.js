$( "#joinGame button").click(function() {
    connectToGame($('#userid-join').val(), $('#gameid').val());
    $("#settings").hide();
    $(".game-container").show();
});

$( "#createGame button" ).click(function() {
    var gameConfig = {
        startingChips: $("#startingChips").val(),
        startingBlinds: $("#startingBlinds").val(),
        blindIncreaseTime: $("#blindIncreaseTime").val(),
        playerMoveTime: $("#playerMoveTime").val(),
        rebuyTime: $("#lateReg").val()
    };
    createGame($('#userid-create').val(), gameConfig);

    $("#settings").hide();
    $(".game-container").show();
    $(".pregame").show();
});

$( "#start" ).click(function() {
    startGame();
    $(".pregame").hide();
});


//raise on enter
$(document).bind('keypress', function(e) {
    if(e.keyCode==13){
        $('#raise').trigger('click');
    }
});

//Settings
$("#createGame").hover(
    function() {$("#createGame").removeClass("unfocused");
        $("#joinGame").addClass("unfocused")}
)

$("#joinGame").hover(
    function() {$("#joinGame").removeClass("unfocused");
        $("#createGame").addClass("unfocused")}
)

//Keyboard focus by tab
$('#joinGame').on('keyup', checkFocused)
function checkFocused() {
    $("#joinGame").removeClass("unfocused");
    $("#createGame").addClass("unfocused");
};

$('#createGame').on('keyup', checkFocused2)
function checkFocused2() {
    $("#createGame").removeClass("unfocused");
    $("#joinGame").addClass("unfocused");
};


//coloring inputs

$(".input-main input").focus (function () {
    $(this).parent().addClass("clicked");
});

$(".input-main input").blur (function () {
    $(this).parent().removeClass("clicked");
});

$(".input-main").hover(
    function() {
        $(this).find(".label").addClass("clicked-label");
    },
    function() {
        $(this).find(".label").removeClass("clicked-label")
    }
)

//advanced settings
$( ".advanced-settings" ).click(function() {
    $(".advanced-settings").hide();
    $(".advanced-inputs").show();
});

function playerCountdown(start, playerPosition, limit, cards) {
    var x = 0;
    var intervalID = setInterval(function () {

        var d = new Date();
        var now = d.getTime();
        var prc = 100 - 100*((now - start)/(limit*1000));
        var crd = cards.split(" ");

        var crdflop = $(".dealt-cards-1").html().charAt(20) + $(".dealt-cards-1").html().charAt(21);
        var crdturn = $(".dealt-cards-4").html().charAt(20) + $(".dealt-cards-4").html().charAt(21);
        var crdriver = $(".dealt-cards-5").html().charAt(20) + $(".dealt-cards-5").html().charAt(21);

        $("#player" + playerPosition + " .player-timer-running").css( "width", prc + "%" );
        if(prc < 25) {
            $("#player" + playerPosition + " .player-timer-running").css( "background-color", "#FF5500" );
        } else if(prc < 50) {
            $("#player" + playerPosition + " .player-timer-running").css( "background-color", "#FFCC00" );
        } else {
            $("#player" + playerPosition + " .player-timer-running").css( "background-color", "#2F06FC" );
        }

        var cardsCheck = false;
        if (crd[crd.length-1] !== crdflop) {
            cardsCheck = true;
        }


        if (++x === limit*5 || $("#player" + playerPosition).hasClass("none") === false || $("#player" + playerPosition).hasClass("onMove") === false || cardsCheck || prc < 1) {
            window.clearInterval(intervalID);
        }
    }, 200);
}

$(document).ready(function () {
        // restore game
        if (Cookies.get('game_uuid') && Cookies.get('player_uuid')) {
            console.log("reconnecting to an existing game");
            connectToGame(Cookies.get("nick"), Cookies.get("game_uuid"));
        }
    }
);

//TODO add input validation
