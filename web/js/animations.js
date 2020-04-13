/* dealt cards */
var flop = anime.timeline({
    autoplay: false,
    duration: 1000
});

var turn = anime.timeline({
    autoplay: false,
    duration: 1000,
    delay: 350
});

var river = anime.timeline({
    autoplay: false,
    duration: 1000,
    delay: 600
});

var turnAlone = anime.timeline({
    autoplay: false,
    duration: 1000
});

var riverAlone = anime.timeline({
    autoplay: false,
    duration: 1000
});

var riverTurn = anime.timeline({
    autoplay: false,
    duration: 1000
});

var riverShow = anime.timeline({
    autoplay: false,
    duration: 1000
});

var turnAndRiver = anime.timeline({
    autoplay: false,
    duration: 4000
});

var flopInstant = anime.timeline({
    autoplay: false,
    duration: 1
});

var turnInstant = anime.timeline({
    autoplay: false,
    duration: 1
});

var riverInstant = anime.timeline({
    autoplay: false,
    duration: 1
});


flop
    .add({
        targets: '.dealt-cards-3',
        scaleX: [-1,-1],
        translateX: 154,
        rotateY: [0, 90],
        duration: 0
    }, 700)
    .add({
        targets: '.dealt-cards-3',
        opacity: [0,1],
        duration: 0
    })
    .add({
        targets: '.dealt-cards-3',
        rotateY: [90, 180],
        easing: 'easeOutQuad',
        duration: 300
    })
    .add({
        targets: '.dealt-cards-3',
        translateX: 0,
        easing: 'easeOutQuad',
        duration: 500
    })
    .add({
        targets: '.dealt-cards-1',
        opacity: [0, 1],
        duration: 1
    }, '-=500')
    .add({
        targets: '.dealt-cards-2',
        opacity: [0, 1],
        duration: 1
    }, '-=350');

flopInstant
    .add({
        targets: '.dealt-cards-3',
        translateX: 0,
        rotateY: 180,
        opacity: [0,1],
        duration: 1
    })
    .add({
        targets: '.dealt-cards-1',
        opacity: [0, 1],
        duration: 1
    })
    .add({
        targets: '.dealt-cards-2',
        opacity: [0, 1],
        duration: 1
    });

turn
    .add({
        targets: '.dealt-cards-4',
        scaleX: [-1,-1],
        rotateY: [0, 90],
        opacity: [0,1],
        duration: 0
    })
    .add({
        targets: '.dealt-cards-4',
        rotateY: [90, 180],
        duration: 1000
    }, 1550);

turnInstant
    .add({
        targets: '.dealt-cards-4',
        scaleX: [-1,-1],
        rotateY: [0, 180],
        opacity: [0,1],
        duration: 1
    });

river
    .add({
        targets: '.dealt-cards-5',
        scaleX: [-1,-1],
        rotateY: [0, 90],
        opacity: [0,1],
        duration: 0
    })
    .add({
        targets: '.dealt-cards-5',
        rotateY: [90, 180],
        duration: 1000
    }, 2250);

riverInstant
    .add({
        targets: '.dealt-cards-5',
        scaleX: [-1,-1],
        rotateY: [0, 180],
        opacity: [0,1],
        duration: 1
    });

turnAlone
    .add({
        targets: '.dealt-cards-4',
        scaleX: [-1,-1],
        rotateY: [0, 90],
        opacity: [0,1],
        duration: 1
    })
    .add({
        targets: '.dealt-cards-4',
        rotateY: [90, 180],
        duration: 1000
    }, 100);

riverAlone
    .add({
        targets: '.dealt-cards-5',
        scaleX: [-1,-1],
        rotateY: [0, 90],
        opacity: [0,1],
        duration: 1
    })
    .add({
        targets: '.dealt-cards-5',
        rotateY: [90, 180],
        duration: 1000
    }, 100);


riverShow
    .add({
        targets: '.dealt-cards-5',
        scaleX: [-1,-1],
        rotateY: [0, 90],
        opacity: [0,1],
        duration: 1
    })
    .add({
        targets: '.dealt-cards-5',
        rotateY: [90, 180],
        duration: 1000
    }, 700);


turnAndRiver
    .add({
        targets: '.dealt-cards-4',
        scaleX: [-1,-1],
        opacity: [0,1],
        rotateY: [0, 90],
        duration: 1
    })
    .add({
        targets: '.dealt-cards-4',
        rotateY: [90, 180],
        duration: 1000
    }, 700)
    .add({
        targets: '.dealt-cards-5',
        scaleX: [-1,-1],
        opacity: [0,1],
        rotateY: [0, 90],
        duration: 1
    })
    .add({
        targets: '.dealt-cards-5',
        rotateY: [90, 180],
        duration: 1000
    });

function animationAll() {
    flop.play();
    turn.play();
    river.play();
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