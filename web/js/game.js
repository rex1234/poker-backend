var timerOn = -1;

$( "#joinGame button").click(function() {
    $("#loader .wrapper .text").html("Feeding you to the sharks…");
    $("#loader").show();
    connectToGame($('#userid-join').val(), $('#gameid').val());
});

$( "#createGame button" ).click(function() {
    $("#loader .wrapper .text").html("Cleaning shark tank…");
    $("#loader").show();
    var gameConfig = {
        startingChips: $("#startingChips").val(),
        startingBlinds: $("#startingBlinds").val(),
        blindIncreaseTime: $("#blindIncreaseTime").val(),
        playerMoveTime: $("#playerMoveTime").val(),
        rebuyTime: $("#lateReg").val() * 60
    };
    createGame($('#userid-create').val(), gameConfig);
});

$( "#start" ).click(function() {
    startGame();
});


//raise on enter
$(document).bind('keypress', function(e) {
    if(e.keyCode==13){
        $('#raise').trigger('click');
    }
});

//Settings
$("#settings h2")
    .click (function () {
        $("#joinGame .toggler").slideToggle();
        $("#createGame .toggler").slideToggle();
        $(this).parent().css("background-color", "#0F2E4B");
    }).hover (
        function () {
            if($(this).parent().children(".toggler").css('display') == 'none') { 
                $(this).parent().css("background-color", "#1D4871");
            }
        },
        function () {
            $(this).parent().css("background-color", "#0F2E4B");
        }
);

$('#userid-join').change(function() {
      $('#userid-create').val($(this).val());
});

$('#userid-create').change(function() {
      $('#userid-join').val($(this).val());
});


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
    $(".advanced-inputs").slideToggle();
});

function playerCountdown(start, playerPosition, limit, cards) {
    var x = 0;
    var intervalID = setInterval(function () {

        if(timerOn !== intervalID) {
            if(timerOn !== -1) {
                window.clearInterval(timerOn);
            }
            timerOn = intervalID;
        }

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

        if (++x === limit*25 || $("#player" + playerPosition).hasClass("none") === false || $("#player" + playerPosition).hasClass("onMove") === false || cardsCheck || prc < 1) {
            window.clearInterval(intervalID);
            timerOn = 0;
        }
    }, 40);

}

$(document).ready(function () {

        // restore game
        if (Cookies.get('game_uuid') && Cookies.get('player_uuid')) {
            $("#loader .wrapper .text").html("Reconnecting…");
            $("#loader").show();
            animationLoader();
            reconnected = true;
            console.log("reconnecting to an existing game");
            connectToGame(Cookies.get("nick"), Cookies.get("game_uuid"));
        }

        if(Cookies.get('player_uuid')) {
            $('#userid-create').val(Cookies.get("nick"));
            $('#userid-join').val(Cookies.get("nick"));
        }
    }
);


//Copy code to clipboard
$("#copyButton").click(function() {
    copyToClipboard(document.getElementById("code"));
    console.log("sss");
});

function copyToClipboard(elem) {
	  // create hidden text element, if it doesn't already exist
    var targetId = "_hiddenCopyText_";
    var isInput = elem.tagName === "INPUT" || elem.tagName === "TEXTAREA";
    var origSelectionStart, origSelectionEnd;
    if (isInput) {
        // can just use the original source element for the selection and copy
        target = elem;
        origSelectionStart = elem.selectionStart;
        origSelectionEnd = elem.selectionEnd;
    } else {
        // must use a temporary form element for the selection and copy
        target = document.getElementById(targetId);
        if (!target) {
            var target = document.createElement("textarea");
            target.style.position = "absolute";
            target.style.left = "-9999px";
            target.style.top = "0";
            target.id = targetId;
            document.body.appendChild(target);
        }
        target.textContent = elem.textContent;
    }
    // select the content
    var currentFocus = document.activeElement;
    target.focus();
    target.setSelectionRange(0, target.value.length);

    // copy the selection
    var succeed;
    try {
    	  succeed = document.execCommand("copy");
    } catch(e) {
        succeed = false;
    }
    // restore original focus
    if (currentFocus && typeof currentFocus.focus === "function") {
        currentFocus.focus();
    }

    if (isInput) {
        // restore prior selection
        elem.setSelectionRange(origSelectionStart, origSelectionEnd);
    } else {
        // clear temporary content
        target.textContent = "";
    }
    $("#copyButton").html("Copied!");
    return succeed;
}

// sidenav

function openNav() {
    document.getElementById("sidenav").style.marginRight = "0px";
    if($(window).width() > 1204) {
        document.getElementById("main").style.marginRight = "300px";
    }
  }
  
  function closeNav() {
    
    document.getElementById("sidenav").style.marginRight = "-300px";
    if($(window).width() > 1204) {
        document.getElementById("main").style.marginRight = "0";
    }
  }



//TODO add input validation
