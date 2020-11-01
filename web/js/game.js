var timerOn = -1;
var beepCounter = 0;

var joinInputValidated = [false, false];
var createInputValidated = [false, true, true, true, true, true, true];


$( "#joinGame button").click(function() {
    // Validation happens only on input, so we have to validate here in case there has been no input so far
    joinInputValidated[0] = nameValidation('#userid-join');
    joinInputValidated[1] = gameIdValidation('#gameid');

    if(joinInputValidated.every(v => v)) {
        $("#loader .wrapper .text").html("Feeding you to the sharks…");
        $("#loader").show();
        loader.play();
        connectToGame($('#userid-join').val(), $('#gameid').val());
    } else {
        $("#join-err").html("Some of the fields do not have correct value.").show();
    }
});

$( "#createGame button" ).click(function() {
    // Validation happens only on input, so we have to validate here in case there has been no input so far
    createInputValidated[0] = nameValidation('#userid-create');

    if(createInputValidated.every(v => v)) {
        $("#loader .wrapper .text").html("Cleaning shark tank…");
        $("#loader").show();
        loader.play();
        var gameConfig = {
            startingChips: Math.max(1, $("#startingChips").val()),
            startingBlinds: Math.max(1, $("#startingBlinds").val()),
            blindIncreaseTime: Math.max(1, $("#blindIncreaseTime").val() * 60),
            playerMoveTime: Math.max(10, $("#playerMoveTime").val()),
            rebuyTime: Math.max(0, $("#lateReg").val() * 60),
            maxRebuys: Math.max(0, $("#rebuy").val())
        };
        createGame($('#userid-create').val(), gameConfig);
    } else {
        $("#create-err").html("Some of the fields do not have correct value.").show();
     }
});

$( ".allow-audio").click(function() {
    snd.play();
    $( ".allow-audio").hide();
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

$("#settings h2").hover (
        function () {
            if($(this).parent().children(".toggler").css('display') == 'none') { 
                $(this).parent().css("background-color", "#1D4871");
            }
        },
        function () {
            $(this).parent().css("background-color", "#0F2E4B");
        }
);

$('.raise-slider').on('wheel', (event) => {
    const $rangeSlider = $('#range-slider');
    const roundedVal = Math.round(parseInt($rangeSlider.val()) / currentSmallBlind) * currentSmallBlind;
    const nextVal = event.originalEvent.wheelDelta > 0
        ? roundedVal + currentSmallBlind
        : roundedVal - currentSmallBlind;
    $rangeSlider.val(nextVal).trigger('input');
});

$("#joinGame h2").click (function () {
    if($(window).width() <= 812) {
        $("#joinGame .toggler").slideToggle();
    } else {
        $("#joinGame .toggler").slideToggle();
        $("#createGame .toggler").slideToggle();
    }

    $(this).parent().css("background-color", "#0F2E4B");
});

$("#createGame h2").click (function () {
    if($(window).width() <= 812) {
        $("#createGame .toggler").slideToggle();
    } else {
        $("#createGame .toggler").slideToggle();
        $("#joinGame .toggler").slideToggle();
    }

    $(this).parent().css("background-color", "#0F2E4B");
});

//collapse Join and Create game on small devices
$(document).ready(function(){
    if($(window).width() <= 812) {
        $("#joinGame .toggler").hide();
        $("#createGame .toggler").hide();
    }
});

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

function playerCountdown(start, playerPosition, limit) {
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

        if((lastAction !== "none" || roundTurn === 1) && playerPosition === 1) {
             if(x === 10) {
                 play("turn");
                 beepCounter++;
             }

             if(x === parseInt(limit*18.75)) {
                 play("warning");
                 beepCounter++;
             }
        }

        $("#player" + playerPosition + " .player-timer-running").css( "width", prc + "%" );
        if(prc < 25) {
            $("#player" + playerPosition + " .player-timer-running").css( "background-color", "#FF5500" );
        } else if(prc < 50) {
            $("#player" + playerPosition + " .player-timer-running").css( "background-color", "#FFCC00" );
        } else {
            $("#player" + playerPosition + " .player-timer-running").css( "background-color", "#2F06FC" );
        }

        if (++x === limit*25 || $("#player" + playerPosition).hasClass("none") === false || $("#player" + playerPosition).hasClass("onMove") === false || prc < 1) {
            window.clearInterval(intervalID);
            timerOn = 0;
        }
    }, 40);

}

$(document).ready(function () {
    const gameUuid = localStorage.getItem('game_uuid');
    const playerUuid = localStorage.getItem('player_uuid');
    const playerNick = localStorage.getItem('nick');

    // restore game
    if (gameUuid && playerUuid && playerNick) {
        $("#loader .wrapper .text").html("Reconnecting…");
        $("#loader").show();
        reconnected = true;
        console.log("reconnecting to an existing game");
        connectToGame(playerNick, gameUuid);
    }

    if (playerNick) {
        $('#userid-create').val(playerNick);
        $('#userid-join').val(playerNick);
        joinInputValidated[0] = nameValidation('#userid-join', 1);
        createInputValidated[0] = nameValidation('#userid-create', 1);
    }
});


//Copy code to clipboard
$("#copyButton").click(function() {
    copyToClipboard(document.getElementById("code"));
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
    if($(window).width() >= 1024) {
        document.getElementById("main").style.marginRight = "300px";
    }
  }
  
  function closeNav() {
    
    document.getElementById("sidenav").style.marginRight = "-300px";
    if($(window).width() >= 1024) {
        document.getElementById("main").style.marginRight = "0";
    }
  }

  $( window ).resize(function() {
    if($(window).width() < 1024) {
        document.getElementById("main").style.marginRight = "0";
    }
  });

$('#foursuits:checkbox').change(function() {
   if ($(this).is(':checked')) {
       cardsSettings = "4c/";
       changeSuits(cardsSettings);
       localStorage.setItem('suits', '4c/');
   } else {
       cardsSettings = "";
       changeSuits(cardsSettings);
       localStorage.removeItem('suits');
   }
});

$('#sound:checkbox').change(function() {
   if ($(this).is(':checked')) {
       soundOn = true;
       localStorage.setItem('sound', true);
   } else {
       soundOn = false;
       localStorage.removeItem('sound');
   }
});

function changeSuits(suit) {
     if(typeof $("#player1 .card-1 img").html() !== "undefined") {
         var card1 = $("#player1 .card-1 img").attr("src").split("/");
         var card2 = $("#player1 .card-2 img").attr("src").split("/");
         var c1 = "img/cards/" + suit + card1[card1.length - 1];
         var c2 = "img/cards/" + suit + card2[card2.length - 1];
         $("#player1 .card-1 img").attr("src", c1);
         $("#player1 .card-2 img").attr("src", c2);

         for(i = 1; i <= 5; i++) {
            var card = $(".dealt-cards-" + i + " img").attr("src");
            if(typeof card !== "undefined") {
                card = card.split("/");
                var c = "img/cards/" + suit + card[card.length - 1];
                $(".dealt-cards-" + i + " img").attr("src", c);
             }
         }
     }

}

var snd;

//sounds
function play(src) {
    var elem = document.querySelector('audio');
    if(elem != null) {
        elem.parentNode.removeChild(elem);
    }
    snd = document.createElement("audio");
    if (src === "check") {
        src = "sounds/check.mp3";
    }
    if (src === "chips") {
        src = "sounds/chips.mp3";
    }
    if (src === "warning") {
        src = "sounds/warning.mp3";
    }
    if (src === "turn") {
        src = "sounds/onturn.mp3";
    }
    if (src === "cards") {
        src = "sounds/card.mp3";
    }


    snd.src = src;
    snd.setAttribute("preload", "auto");
    snd.setAttribute("controls", "none");
    snd.style.display = "none";
    document.body.appendChild(snd);
      if(soundOn === true) {
        snd.play();
      }
  }

//realtime input validation

$(document).ready(function(){
    $('#userid-join').on('input',function(){
        joinInputValidated[0] = nameValidation('#userid-join', 1);
        createInputValidated[0] = nameValidation('#userid-create', 1);
    });
    $('#userid-create').on('input',function(){
        joinInputValidated[0] = nameValidation('#userid-join', 2);
        createInputValidated[0] = nameValidation('#userid-create', 2);
    });

    $('#gameid').on('input', () => {
        joinInputValidated[1] = gameIdValidation('#gameid');
    });

    $('#startingChips').on('input',function(){
        createInputValidated[1] = numberValidation('#startingChips', 100);
    });
    $('#rebuy').on('input',function(){
        createInputValidated[2] = numberValidation('#rebuy', 0);
    });
    $('#lateReg').on('input',function(){
        createInputValidated[3] = numberValidation('#lateReg', 0);
    });
    $('#blindIncreaseTime').on('input',function(){
        createInputValidated[4] = numberValidation('#blindIncreaseTime', 1);
    });
    $('#playerMoveTime').on('input',function(){
        createInputValidated[5] = numberValidation('#playerMoveTime', 5);
    });
    $('#startingBlinds').on('input',function(){
        createInputValidated[6] = numberValidation('#startingBlinds', 1);
    });
});

function nameValidation(obj, elem) {
    var regexname= /[^a-zA-ZÀÁÂÃÄÅàáâãäåÒÓÔÕÖØòóôõöøÈÉÊËèéêëÇçÌÍÎÏìíîïÙÚÛÜŮùúûüůÿÑñčČřŘšŠěĚďĎľĽňŇťŤžŽ0-9.:%!?@#^$&*(),+|\\\/_=\- ]/;
    if(elem === 1) {
        $("#userid-create").val($(obj).val());
    } else {
        $("#userid-join").val($(obj).val());
    }
    if($(obj).val().length < 1) {
         $(obj + ' ~ .errmsginput').show();
         $(obj + ' ~ .errmsginput').html("The name has to be at least one character long.");
         $(obj).addClass("invalid");
         return false;
    } else if ($(obj).val().match(regexname)) {
          // there is a mismatch, hence show the error message
          $(obj + ' ~ .errmsginput').show();
          $(obj).addClass("invalid");
          if(obj.val().length > 9) {
             $(obj + ' ~ .errmsginput').html("The name is too long (10 chars max).");
             return false;
          } else {
             $(obj + ' ~ .errmsginput').html("The name contains illegal characters.");
             return false;
          }
    } else {
         // else, do not display message
         $('#join-id-err').hide();
         $('#join-id-err').html("");
         $('#create-id-err').hide();
         $('#create-id-err').html("");
         $("#userid-join").removeClass("invalid");
         $("#userid-create").removeClass("invalid");
         $("#create-err").hide();
         $("#join-err").hide();
         return true;
    }
}

function numberValidation(obj, min) {
    if($(obj).val() < min) {
         $(obj + ' ~ .errmsginput').show();
         $(obj + ' ~ .errmsginput').html("Minimum value is " + min + ".");
         $(obj).addClass("invalid");
         return false;
    } else {
         // else, do not display message
         $(obj + ' ~ .errmsginput').hide();
         $(obj + ' ~ .errmsginput').html("");
         $(obj).removeClass("invalid");
         $("#create-err").hide();
         return true;
    }
}

function gameIdValidation(selector) {
    const $obj = $(selector);
    if ($obj.val().length === 0) {
        $(selector + ' ~ .errmsginput').text('The game ID cannot be empty.').show();
        $obj.addClass('invalid');
        return false;
    } else {
        $(selector + ' ~ .errmsginput').text('').hide();
        $obj.removeClass('invalid');
        $('#join-err').hide();
        return true;
    }
}
