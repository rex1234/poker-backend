let timerOn = -1;
let beepCounter = 0;
let timerTick = 0;

const joinInputValidated = [false, false];
const createInputValidated = [false, true, true, true, true, true, true];


$('#joinGame button').on('click', function () {
    // Validation happens only on input, so we have to validate here in case there has been no input so far
    joinInputValidated[0] = nameValidation('#userid-join');
    joinInputValidated[1] = gameIdValidation('#gameid');

    if (joinInputValidated.every(v => v)) {
        $('#loader .wrapper .text').html('Feeding you to the sharks…');
        $('#loader').show();
        loader.play();
        connectToGame($('#userid-join').val(), $('#gameid').val());
    } else {
        $('#join-err').html('Some of the fields do not have correct value.').show();
    }
});

$('#createGame button').on('click', function () {
    // Validation happens only on input, so we have to validate here in case there has been no input so far
    createInputValidated[0] = nameValidation('#userid-create');

    if (createInputValidated.every(v => v)) {
        $('#loader .wrapper .text').html('Cleaning shark tank…');
        $('#loader').show();
        loader.play();
        const gameConfig = {
            startingChips: Math.max(100, $('#startingChips').val()),
            startingBlinds: Math.max(1, $('#startingBlinds').val()),
            blindIncreaseTime: Math.max(60, $('#blindIncreaseTime').val() * 60),
            playerMoveTime: Math.max(5, $('#playerMoveTime').val()),
            rebuyTime: Math.max(0, $('#lateReg').val() * 60),
            maxRebuys: Math.max(0, $('#rebuy').val()),
        };
        localStorage.setItem('gameConfig', JSON.stringify(gameConfig));
        createGame($('#userid-create').val(), gameConfig);
    } else {
        $('#create-err').html('Some of the fields do not have correct value.').show();
    }
});

$('.allow-audio').on('click', function () {
    snd.play();
    $('.allow-audio').hide();
});

$('#start').on('click', function () {
    startGame();
});

$('#fold').on('click', gameFold);
$('#call').on('click', gameCall);
$('#check').on('click', gameCheck);
$('#additional').on('click', () => {
    showCards();
    $('#player1').addClass('showCards');
});
$('#rebuy-btn').on('click', addRebuy);
$('#sad-react').on('click', () => sendReaction('sad'));

$('.openbtn').on('click', openNav);
$('.closebtn').on('click', closeNav);
$('#pause').on('click', pause);
$('#unpause').on('click', unpause);
$('#leave').on('click', leave);

//raise on enter
$(document).bind('keypress', function (e) {
    if (e.keyCode === 13) {
        $('#raise').trigger('click');
    }
});

//Settings

$('#settings h2').hover(
    function () {
        if ($(this).parent().children('.toggler').css('display') === 'none') {
            $(this).parent().css('background-color', '#1D4871');
        }
    },
    function () {
        $(this).parent().css('background-color', '#0F2E4B');
    },
);

$('.raise-slider').on('wheel', (event) => {
    event.preventDefault();
    const $rangeSlider = $('#range-slider');
    const roundedVal = Math.round(parseInt($rangeSlider.val()) / currentSmallBlind) * currentSmallBlind;
    const nextVal = event.originalEvent.deltaY < 0
        ? roundedVal + currentSmallBlind
        : roundedVal - currentSmallBlind;
    $rangeSlider.val(nextVal).trigger('input');
});

$('#joinGame h2').on('click', function () {
    if ($(window).width() <= 812) {
        $('#joinGame .toggler').slideToggle();
    } else {
        $('#joinGame .toggler').slideToggle();
        $('#createGame .toggler').slideToggle();
    }

    $(this).parent().css('background-color', '#0F2E4B');
});

$('#createGame h2').on('click', function () {
    if ($(window).width() <= 812) {
        $('#createGame .toggler').slideToggle();
    } else {
        $('#createGame .toggler').slideToggle();
        $('#joinGame .toggler').slideToggle();
    }

    $(this).parent().css('background-color', '#0F2E4B');
});

//collapse Join and Create game on small devices
$(document).ready(function () {
    if ($(window).width() <= 812) {
        $('#joinGame .toggler').hide();
        $('#createGame .toggler').hide();
    }
});

$('#userid-join').change(function () {
    $('#userid-create').val($(this).val());
    createInputValidated[0] = nameValidation('#userid-create', 1);
});

$('#userid-create').change(function () {
    $('#userid-join').val($(this).val());
    joinInputValidated[0] = nameValidation('#userid-join', 2);
});


//coloring inputs

$('.input-main input').focus(function () {
    $(this).parent().addClass('clicked');
});

$('.input-main input').blur(function () {
    $(this).parent().removeClass('clicked');
});

$('.input-main').hover(
    function () {
        $(this).find('.label').addClass('clicked-label');
    },
    function () {
        $(this).find('.label').removeClass('clicked-label');
    },
);

//advanced settings
$('.advanced-settings').on('click', function () {
    $('.advanced-settings').hide();
    $('.advanced-inputs').slideToggle();
});

function playerCountdown(start, playerPosition, limit, serverTime) {
    let x = 0;
    const execEveryMs = 40;
    timerTick = 0;
    const intervalID = setInterval(function () {
        timerTick++;
        if (timerOn !== intervalID) {
            if (timerOn !== -1) {
                window.clearInterval(timerOn);
            }
            timerOn = intervalID;
        }

        const remainingTimeSocket = (limit * 1000) - (serverTime - start);
        const remainingTime = remainingTimeSocket - timerTick * execEveryMs;

        const prc = 100 * (remainingTime / (limit * 1000));

        if ((lastAction !== 'none' || roundTurn === 1) && playerPosition === 1) {
            if (x === 10) {
                play('turn');
                beepCounter++;
            }

            if (x === parseInt(limit * 18.75)) {
                play('warning');
                beepCounter++;
            }
        }

        const $playerTime = $('#player' + playerPosition + ' .player-timer-running');
        $playerTime.css('width', prc + '%');
        if (prc < 25) {
            $playerTime.css('background-color', '#FF5500');
        } else if (prc < 50) {
            $playerTime.css('background-color', '#FFCC00');
        } else {
            $playerTime.css('background-color', '#2F06FC');
        }

        const $player = $('#player' + playerPosition);
        if (++x === limit * 25 || $player.hasClass('onMove') === false || prc < 1) {
            window.clearInterval(intervalID);
            timerOn = 0;
        }
    }, execEveryMs);

}

$(document).ready(function () {
    const gameUuid = localStorage.getItem('game_uuid');
    let playerUuid = localStorage.getItem('player_uuid');
    const playerNick = localStorage.getItem('nick');
    const gameConfig = localStorage.getItem('gameConfig');
    const gameStarted = localStorage.getItem('gameStarted');

    if (!playerUuid || playerUuid.length !== 32) {
        playerUuid = randomString(32)
        localStorage.setItem("player_uuid", playerUuid);
    }

    // restore game
    if (gameStarted === "true") {
        $('#loader .wrapper .text').html('Reconnecting…');
        $('#loader').show();
        reconnected = true;
        console.log('reconnecting to an existing game');
        connectToGame(playerNick);
    }

    if (playerNick) {
        $('#userid-create').val(playerNick);
        $('#userid-join').val(playerNick);
        joinInputValidated[0] = nameValidation('#userid-join', 1);
        createInputValidated[0] = nameValidation('#userid-create', 1);
    }

    if (gameConfig) {
        try {
            const parsedConfig = JSON.parse(gameConfig);
            $('#startingChips').val(parsedConfig.startingChips);
            $('#startingBlinds').val(parsedConfig.startingBlinds);
            $('#blindIncreaseTime').val(parsedConfig.blindIncreaseTime / 60);
            $('#playerMoveTime').val(parsedConfig.playerMoveTime);
            $('#lateReg').val(parsedConfig.rebuyTime / 60);
            $('#rebuy').val(parsedConfig.maxRebuys);
        } catch (e) {
            console.error(e);
            console.error('Could not parse stored game config, default values will be used');
        }
    }
});


//Copy code to clipboard
$('#copyButton').off('click').on('click', () => {
    copyToClipboard(document.getElementById('code'));
});

function copyToClipboard(elem) {
    let target;
    // create hidden text element, if it doesn't already exist
    const targetId = '_hiddenCopyText_';
    const isInput = elem.tagName === 'INPUT' || elem.tagName === 'TEXTAREA';
    let origSelectionStart, origSelectionEnd;
    if (isInput) {
        // can just use the original source element for the selection and copy
        target = elem;
        origSelectionStart = elem.selectionStart;
        origSelectionEnd = elem.selectionEnd;
    } else {
        // must use a temporary form element for the selection and copy
        target = document.getElementById(targetId);
        if (!target) {
            target = document.createElement('textarea');
            target.style.position = 'absolute';
            target.style.left = '-9999px';
            target.style.top = '0';
            target.id = targetId;
            document.body.appendChild(target);
        }
        target.textContent = elem.textContent;
    }
    // select the content
    const currentFocus = document.activeElement;
    target.focus();
    target.setSelectionRange(0, target.value.length);

    // copy the selection
    let succeed;
    try {
        succeed = document.execCommand('copy');
    } catch (e) {
        succeed = false;
    }
    // restore original focus
    if (currentFocus && typeof currentFocus.focus === 'function') {
        currentFocus.focus();
    }

    if (isInput) {
        // restore prior selection
        elem.setSelectionRange(origSelectionStart, origSelectionEnd);
    } else {
        // clear temporary content
        target.textContent = '';
    }
    $('#copyButton').html('Copied!');
    return succeed;
}

// sidenav

function openNav() {
    document.getElementById('sidenav').style.marginRight = '0px';
    if ($(window).width() >= 1024) {
        document.getElementById('main').style.marginRight = '300px';
    }
}

function closeNav() {
    document.getElementById('sidenav').style.marginRight = '-300px';
    if ($(window).width() >= 1024) {
        document.getElementById('main').style.marginRight = '0';
    }
}

$(window).resize(function () {
    if ($(window).width() < 1024) {
        document.getElementById('main').style.marginRight = '0';
    }
});

$('#foursuits:checkbox').change(function () {
    if ($(this).is(':checked')) {
        cardsSettings = '4c/';
        changeSuits(cardsSettings);
        localStorage.setItem('suits', '4c/');
    } else {
        cardsSettings = '';
        changeSuits(cardsSettings);
        localStorage.removeItem('suits');
    }
});

$('#sound:checkbox').change(function () {
    if ($(this).is(':checked')) {
        soundOn = true;
        localStorage.setItem('sound', true);
    } else {
        soundOn = false;
        localStorage.removeItem('sound');
    }
});

function changeSuits(suit) {
    let $card1img = $('#player1 .card-1 img');
    let $card2img = $('#player1 .card-2 img');
    if (typeof $card1img.html() !== 'undefined') {
        const card1 = $card1img.attr('src').split('/');
        const card2 = $card2img.attr('src').split('/');
        const c1 = 'img/cards/' + suit + card1[card1.length - 1];
        const c2 = 'img/cards/' + suit + card2[card2.length - 1];
        $card1img.attr('src', c1);
        $card2img.attr('src', c2);

        for (let i = 1; i <= 5; i++) {
            let card = $('.dealt-cards-' + i + ' img').attr('src');
            if (typeof card !== 'undefined') {
                card = card.split('/');
                const c = 'img/cards/' + suit + card[card.length - 1];
                $('.dealt-cards-' + i + ' img').attr('src', c);
            }
        }
    }

}

let snd;

//sounds
function play(src) {
    const elem = document.querySelector('audio');
    if (elem != null) {
        elem.parentNode.removeChild(elem);
    }
    snd = document.createElement('audio');
    if (src === 'check') {
        src = 'sounds/check.mp3';
    }
    if (src === 'chips') {
        src = 'sounds/chips.mp3';
    }
    if (src === 'warning') {
        src = 'sounds/warning.mp3';
    }
    if (src === 'turn') {
        src = 'sounds/onturn.mp3';
    }
    if (src === 'cards') {
        src = 'sounds/card.mp3';
    }


    snd.src = src;
    snd.setAttribute('preload', 'auto');
    snd.setAttribute('controls', 'none');
    snd.style.display = 'none';
    document.body.appendChild(snd);
    if (soundOn === true) {
        snd.play();
    }
}

//realtime input validation

$(document).ready(function () {
    $('#userid-join').on('input', function () {
        joinInputValidated[0] = nameValidation('#userid-join', 1);
        createInputValidated[0] = nameValidation('#userid-create', 1);
    });
    $('#userid-create').on('input', function () {
        joinInputValidated[0] = nameValidation('#userid-join', 2);
        createInputValidated[0] = nameValidation('#userid-create', 2);
    });

    $('#gameid').on('input', () => {
        joinInputValidated[1] = gameIdValidation('#gameid');
    });

    $('#startingChips').on('input', function () {
        createInputValidated[1] = numberValidation('#startingChips', 100, 1000000);
    });
    $('#rebuy').on('input', function () {
        createInputValidated[2] = numberValidation('#rebuy', 0, 100);
    });
    $('#lateReg').on('input', function () {
        createInputValidated[3] = numberValidation('#lateReg', 0, 24 * 60);
    });
    $('#blindIncreaseTime').on('input', function () {
        createInputValidated[4] = numberValidation('#blindIncreaseTime', 1, 24 * 60);
    });
    $('#playerMoveTime').on('input', function () {
        createInputValidated[5] = numberValidation('#playerMoveTime', 5, 60 * 60);
    });
    $('#startingBlinds').on('input', function () {
        createInputValidated[6] = numberValidation('#startingBlinds', 1, 1000000);
    });
});

function nameValidation(obj, elem) {
    //regex explained: https://stackoverflow.com/a/39134560
    const regexname = /[^.:%!?@#^$&*(),+|'_=\-0-9a-zA-Z\u00C0-\u024F\u1E00-\u1EFF ]/;

    if (elem === 1) {
        $('#userid-create').val($(obj).val());
    } else {
        $('#userid-join').val($(obj).val());
    }

    const markInvalid = (errMsg) => {
        $(obj + ' ~ .errmsginput').show();
        $(obj + ' ~ .errmsginput').html(errMsg);
        $(obj).addClass('invalid');
    };

    if ($(obj).val().length < 1) {
        markInvalid('The name has to be at least one character long.');
        return false;
    } else if ($(obj).val().length > 10) {
        markInvalid('The name is too long (10 chars max).');
        return false;
    } else if ($(obj).val().match(regexname)) {
        markInvalid('The name contains illegal characters.');
        return false;
    } else {
        // else, do not display message
        $('#join-id-err').html('').hide();
        $('#create-id-err').html('').hide();
        $('#userid-join').removeClass('invalid');
        $('#userid-create').removeClass('invalid');
        $('#create-err').hide();
        $('#join-err').hide();
        return true;
    }
}

function numberValidation(obj, min, max) {
    const val = $(obj).val();
    if (min && val < min) {
        $(obj + ' ~ .errmsginput').show();
        $(obj + ' ~ .errmsginput').html('Minimum value is ' + min + '.');
        $(obj).addClass('invalid');
        return false;
    } else if (max && val > max) {
        $(obj + ' ~ .errmsginput').show();
        $(obj + ' ~ .errmsginput').html('Maximum value is ' + max + '.');
        $(obj).addClass('invalid');
    } else {
        // else, do not display message
        $(obj + ' ~ .errmsginput').hide();
        $(obj + ' ~ .errmsginput').html('');
        $(obj).removeClass('invalid');
        $('#create-err').hide();
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

function randomString(length) {
    // declare all characters
    const characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';

    let result = '';
    const charactersLength = characters.length;
    for (let i = 0; i < length; i++) {
        result += characters.charAt(Math.floor(Math.random() * charactersLength));
    }

    return result;
}
