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
    duration: 1000,
    delay: 150
});

flop
    .add({
        targets: '.dealt-cards-3',
        scaleX: [-1,-1],
        translateX: 154,
        rotateY: [0, 90],
        duration: 0
    })
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

turn
    .add({
        targets: '.dealt-cards-4',
        scaleX: [-1,-1],
        rotateY: [0, 90],
        duration: 0
    })
    .add({
        targets: '.dealt-cards-4',
        opacity: [0,1],
        duration: 0
    })
    .add({
        targets: '.dealt-cards-4',
        rotateY: [90, 180],
        duration: 1000
    });

river
    .add({
        targets: '.dealt-cards-5',
        scaleX: [-1,-1],
        rotateY: [0, 90],
        duration: 0
    })
    .add({
        targets: '.dealt-cards-5',
        opacity: [0,1],
        duration: 0
    })
    .add({
        targets: '.dealt-cards-5',
        rotateY: [90, 180],
        duration: 1000
    });

turnAlone
    .add({
        targets: '.dealt-cards-4',
        scaleX: [-1,-1],
        rotateY: [0, 90],
        duration: 0
    })
    .add({
        targets: '.dealt-cards-4',
        opacity: [0,1],
        duration: 0
    })
    .add({
        targets: '.dealt-cards-4',
        rotateY: [90, 180],
        duration: 1000
    });

riverAlone
    .add({
        targets: '.dealt-cards-5',
        scaleX: [-1,-1],
        rotateY: [0, 90],
        duration: 0
    })
    .add({
        targets: '.dealt-cards-5',
        opacity: [0,1],
        duration: 0
    })
    .add({
        targets: '.dealt-cards-5',
        rotateY: [90, 180],
        duration: 1000
    });

    riverTurn
    .add({
        targets: '.dealt-cards-5',
        scaleX: [-1,-1],
        rotateY: [0, 90],
        duration: 0
    })
    .add({
        targets: '.dealt-cards-5',
        opacity: [0,1],
        duration: 0
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

function animationTurnAndRiver() {
    turnAlone.play();
    riverTurn.play();
}