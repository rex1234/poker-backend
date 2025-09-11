let thirdCard = $('.dealt-cards-3').css('left');
const loader = anime({
    targets: '#loader .wrapper img',
    easing: 'easeInOutSine',
    keyframes: [
        {translateY: [0, 0]},
        {translateY: [0, -20]},
        {translateY: [-20, 0]},
    ],
    loop: true,
    delay: anime.stagger(200),
});

$(window).resize(function () {
    thirdCard = $('.dealt-cards-3').css('left');
});

/* dealt cards */
const flop = anime.timeline({
    autoplay: false,
    duration: 1000,
});

const turn = anime.timeline({
    autoplay: false,
    duration: 1000,
    delay: 350,
});

const river = anime.timeline({
    autoplay: false,
    duration: 1000,
    delay: 600,
});

const turnAlone = anime.timeline({
    autoplay: false,
    duration: 1000,
});

const riverAlone = anime.timeline({
    autoplay: false,
    duration: 1000,
});

const riverShow = anime.timeline({
    autoplay: false,
    duration: 1000,
});

const turnAndRiver = anime.timeline({
    autoplay: false,
    duration: 4000,
});

const flopInstant = anime.timeline({
    autoplay: false,
    duration: 1,
});

const turnInstant = anime.timeline({
    autoplay: false,
    duration: 1,
});

const riverInstant = anime.timeline({
    autoplay: false,
    duration: 1,
});

const preflop = anime.timeline({
    autoplay: false,
    duration: 5000,
});

preflop
    .add({
        targets: '.dealt-cards-3',
        scaleX: [-1, -1],
        translateX: [0, thirdCard],
        rotateY: [0, 90],
        duration: 1,
    }, 700)
    .add({
        targets: '.dealt-cards-3',
        opacity: [0, 1],
        duration: 1,
    })
    .add({
        targets: '.dealt-cards-3',
        rotateY: [90, 180],
        rotate: 0.001,
        easing: 'easeOutQuad',
        duration: 300,
    })
    .add({
        targets: '.dealt-cards-3',
        translateX: 0,
        easing: 'easeOutQuad',
        duration: 500,
    })
    .add({
        targets: '.dealt-cards-1',
        opacity: [0, 1],
        duration: 1,
    }, '-=500')
    .add({
        targets: '.dealt-cards-2',
        opacity: [0, 1],
        duration: 1,
    }, '-=350')
    .add({
        targets: '.dealt-cards-4',
        scaleX: [-1, -1],
        opacity: [0, 1],
        rotateY: [0, 90],
        duration: 1,
    }, '+=500')
    .add({
        targets: '.dealt-cards-4',
        rotateY: [90, 180],
        rotate: 0.001,
        duration: 1000,
    })
    .add({
        targets: '.dealt-cards-5',
        scaleX: [-1, -1],
        opacity: [0, 1],
        rotateY: [0, 90],
        duration: 1,
    })
    .add({
        targets: '.dealt-cards-5',
        rotateY: [90, 180],
        rotate: 0.001,
        duration: 1000,
        complete: function () {
            refreshCards();
            highlightCards(finishedData);
            winningAnimationHandler();
        },
    });


flop
    .add({
        targets: '.dealt-cards-3',
        scaleX: [-1, -1],
        translateX: [0, thirdCard],
        rotateY: ['0deg', '90deg'],
        duration: 0,
    }, 700)
    .add({
        targets: '.dealt-cards-3',
        opacity: [0, 1],
        duration: 0,
    })
    .add({
        targets: '.dealt-cards-3',
        rotateY: ['90deg', '180deg'],
        rotate: 0.001,
        easing: 'easeOutQuad',
        duration: 300,
    })
    .add({
        targets: '.dealt-cards-3',
        translateX: 0,
        easing: 'easeOutQuad',
        duration: 500,
    })
    .add({
        targets: '.dealt-cards-1',
        opacity: [0, 1],
        duration: 1,
    }, '-=500')
    .add({
        targets: '.dealt-cards-2',
        opacity: [0, 1],
        duration: 1,
    }, '-=350');

flopInstant
    .add({
        targets: '.dealt-cards-3',
        translateX: 0,
        rotateY: 180,
        opacity: [0, 1],
        duration: 1,
    })
    .add({
        targets: '.dealt-cards-1',
        opacity: [0, 1],
        duration: 1,
    })
    .add({
        targets: '.dealt-cards-2',
        opacity: [0, 1],
        duration: 1,
    });

turnInstant
    .add({
        targets: '.dealt-cards-4',
        scaleX: [-1, -1],
        rotateY: [0, 180],
        opacity: [0, 1],
        duration: 1,
    });

riverInstant
    .add({
        targets: '.dealt-cards-5',
        scaleX: [-1, -1],
        rotateY: [0, 180],
        opacity: [0, 1],
        duration: 1,
    });

turnAlone
    .add({
        targets: '.dealt-cards-4',
        scaleX: [-1, -1],
        rotateY: [0, 90],
        rotate: 0.001,
        opacity: [0, 1],
        duration: 1,
    })
    .add({
        targets: '.dealt-cards-4',
        rotateY: [90, 180],
        duration: 1000,
    }, 100);

riverAlone
    .add({
        targets: '.dealt-cards-5',
        scaleX: [-1, -1],
        rotateY: [0, 90],
        rotate: 0.001,
        opacity: [0, 1],
        duration: 1,
    })
    .add({
        targets: '.dealt-cards-5',
        rotateY: [90, 180],
        duration: 1000,
    }, 100);


riverShow
    .add({
        targets: '.dealt-cards-5',
        scaleX: [-1, -1],
        rotateY: [0, 90],
        rotate: 0.001,
        opacity: [0, 1],
        duration: 1,
    })
    .add({
        targets: '.dealt-cards-5',
        rotateY: [90, 180],
        duration: 1000,
        complete: function () {
            refreshCards();
            highlightCards(finishedData);
            winningAnimationHandler();
        },
    }, 700);


turnAndRiver
    .add({
        targets: '.dealt-cards-4',
        scaleX: [-1, -1],
        opacity: [0, 1],
        rotateY: [0, 90],
        rotate: 0.001,
        duration: 1,
    })
    .add({
        targets: '.dealt-cards-4',
        rotateY: [90, 180],
        rotate: 0.001,
        duration: 1000,
    }, 700)
    .add({
        targets: '.dealt-cards-5',
        scaleX: [-1, -1],
        opacity: [0, 1],
        rotateY: [0, 90],
        rotate: 0.001,
        duration: 1,
    })
    .add({
        targets: '.dealt-cards-5',
        rotateY: [90, 180],
        rotate: 0.001,
        duration: 1000,
        complete: function () {
            refreshCards();
            highlightCards(finishedData);
            winningAnimationHandler();
        },
    });

function animationAll() {
    preflop.play();
    //turn.play();
    //river.play();
}

function animationFlop() {
    flop.play();
}

function animationTurn() {
    turnAlone.play();
}

function animationRiver() {
    riverAlone.play();
}

function animationRiverShowdown() {
    riverShow.play();
}

function animationTurnAndRiver() {
    turnAndRiver.play();
}

function animationFlopInstant() {
    flopInstant.play();
}

function animationTurnInstant() {
    turnInstant.play();
}

function animationRiverInstant() {
    riverInstant.play();
}


let confettiReach = 100;
let confettiSize = 8;
let confettiYoffset = 0;

if ($(window).width() < 1024) {
    confettiReach = 50;
    confettiSize = 5;
    confettiYoffset = 20;
}

$(window).resize(function () {
    if ($(window).width() < 1024) {
        confettiReach = 50;
        confettiSize = 5;
        confettiYoffset = 20;
    } else {
        confettiReach = 100;
        confettiSize = 8;
        confettiYoffset = 0;

    }
});


//returns random int with random sign
function getRandomInt(min, max) {
    min = Math.ceil(min);
    max = Math.floor(max);
    // var sign = Math.random();
    // if(sign > .5) {
    //     sign = -1;
    // }
    return (Math.floor(Math.random() * (max - min)) + min); //The maximum is exclusive and the minimum is inclusive
}

function randomColor() {
    const colors = ['#2F06FC', '#67FFBD', '#FF5500'];
    return colors[Math.floor(Math.random() * colors.length)];
}

function winningAnimation(position) {
    $('#player' + position).addClass('winning');
    let $conf;
    for (let i = 0; i < 100; i++) {
        $('#player' + position + ' .player-animation').append(`<div class="confetti conf${i}"></div>`);
        $conf = $('.conf' + i);
        $conf.css('background-color', randomColor());
        const randSize = getRandomInt(2, confettiSize);
        $conf.css('width', randSize);
        $conf.css('height', randSize);
        if (i === 0) {
            anime({
                targets: '.conf' + i,
                translateX: getRandomInt(-confettiReach, confettiReach),
                translateY: getRandomInt(-confettiReach - confettiYoffset, confettiReach - confettiYoffset),
                opacity: [1, 0],
                easing: 'easeOutQuad',
                complete: function () {
                    $('#player' + position).removeClass('winning');
                    $('#player' + position + ' .player-animation').html('');
                },
                duration: 2000,
            });
        } else {
            const dur = getRandomInt(1000, 2000);
            anime({
                targets: '.conf' + i,
                translateX: getRandomInt(-confettiReach, confettiReach),
                translateY: getRandomInt(-confettiReach - confettiYoffset, confettiReach - confettiYoffset),

                opacity: [1, 0],
                easing: 'easeOutQuad',
                duration: dur,
            });
            anime({
                targets: '.conf' + i,
                opacity: [1, 0],
                easing: 'linear',
                duration: dur * 1.2,
            });
        }
    }
}

// triggers animation for all winners
function winningAnimationHandler() {
    if (winningAnimationInProgress === false) {
        const winners = getBiggestWinner(finishedData);
        for (let j = 0; j < winners.length; j++) {
            winningAnimation(getPlayerPosition(finishedData, winners[j]));
            //pause animations after the last player was animated
            if (j === winners.length - 1) {
                winningAnimationInProgress = true;
            }
        }
    }
}