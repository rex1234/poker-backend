var thirdCard = $(".dealt-cards-3").css("left");
var loader = anime({
    targets: '#loader .wrapper img',
    easing: 'easeInOutSine',
    keyframes: [
        {translateY: [0,0]},
        {translateY: [0,-20]},
        {translateY: [-20,0]}
      ],
      loop: true,
    delay: anime.stagger(200)
});

$( window ).resize(function() {
    thirdCard = $(".dealt-cards-3").css("left");
});

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

var preflop = anime.timeline({
    autoplay: false,
    duration: 5000
});

preflop
    .add({
        targets: '.dealt-cards-3',
        scaleX: [-1,-1],
        translateX: [0, thirdCard],
        rotateY: [0, 90],
        duration: 1
    }, 700)
    .add({
        targets: '.dealt-cards-3',
        opacity: [0,1],
        duration: 1
    })
    .add({
        targets: '.dealt-cards-3',
        rotateY: [90, 180],
        rotate: 0.001,  
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
    }, '-=350')
    .add({
        targets: '.dealt-cards-4',
        scaleX: [-1,-1],
        opacity: [0,1],
        rotateY: [0, 90],
        duration: 1
    }, '+=500')
    .add({
        targets: '.dealt-cards-4',
        rotateY: [90, 180],
        rotate: 0.001,    
        duration: 1000
    })
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
         rotate: 0.001,  
         duration: 1000,
         complete: function(anim) {
             refreshCards();
             highlightCards(finishedData);
             var winners = getBiggestWinner(finishedData);
             for(i = 0; i < winners.length; i++) {
                 winningAnimation(getPlayerPosition(finishedData, winners[i]));
             }
         }
     });


flop
    .add({
        targets: '.dealt-cards-3',
        scaleX: [-1,-1],
        translateX: [0, thirdCard],
        rotateY: ['0deg', '90deg'],
        duration: 0
    }, 700)
    .add({
        targets: '.dealt-cards-3',
        opacity: [0,1],
        duration: 0
    })
    .add({
        targets: '.dealt-cards-3',
        rotateY: ['90deg', '180deg'],
        rotate: 0.001,  
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

turnInstant
    .add({
        targets: '.dealt-cards-4',
        scaleX: [-1,-1],
        rotateY: [0, 180],
        opacity: [0,1],
        duration: 1
    });

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
        rotate: 0.001,  
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
        rotate: 0.001,  
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
        rotate: 0.001,  
        opacity: [0,1],
        duration: 1
    })
    .add({
        targets: '.dealt-cards-5',
        rotateY: [90, 180],
        duration: 1000,
        complete: function(anim) {
            refreshCards();
            highlightCards(finishedData);
            var winners = getBiggestWinner(finishedData);
            for(i = 0; i < winners.length; i++) {
                winningAnimation(getPlayerPosition(finishedData, winners[i]));
            }
        }
    }, 700);


turnAndRiver
    .add({
        targets: '.dealt-cards-4',
        scaleX: [-1,-1],
        opacity: [0,1],
        rotateY: [0, 90],
        rotate: 0.001,  
        duration: 1
    })
    .add({
        targets: '.dealt-cards-4',
        rotateY: [90, 180],
        rotate: 0.001,  
        duration: 1000
    }, 700)
    .add({
        targets: '.dealt-cards-5',
        scaleX: [-1,-1],
        opacity: [0,1],
        rotateY: [0, 90],
        rotate: 0.001,  
        duration: 1
    })
    .add({
        targets: '.dealt-cards-5',
        rotateY: [90, 180],
        rotate: 0.001,  
        duration: 1000,
        complete: function(anim) {
            refreshCards();
            highlightCards(finishedData);
            var winners = getBiggestWinner(finishedData);
            for(i = 0; i < winners.length; i++) {
                winningAnimation(getPlayerPosition(finishedData, winners[i]));
            }
        }
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


var confettiReach = 100;
var confettiSize = 8;
var confettiYoffset = 0;

if($(window).width() < 1024) {
    confettiReach = 50;
    confettiSize = 5;
    confettiYoffset = 20;
}

$( window ).resize(function() {
    if($(window).width() < 1024) {
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
    var sign = Math.random();
    //if(sign > .5) {
    //    sign = -1;
    //}
    return (Math.floor(Math.random() * (max - min)) + min); //The maximum is exclusive and the minimum is inclusive
  }

  function randomColor() {
    var colors = ['#2F06FC', '#67FFBD', '#FF5500'];
    return colors[Math.floor(Math.random() * colors.length)];
  }

function winningAnimation(position) {
    $("#player" + position).addClass("winning");
    for(i = 0; i < 100; i++) {
        $("#player" + position + " .player-animation").append("<div class='confetti conf" + i +"'></div>");
        $(".conf"+i).css("background-color", randomColor());
        var randSize = getRandomInt(2, confettiSize);
        $(".conf"+i).css("width", randSize);
        $(".conf"+i).css("height", randSize);
        if(i === 0) {
        anime({
            targets: '.conf'+i,
            translateX: getRandomInt(-confettiReach, confettiReach),
            translateY: getRandomInt(-confettiReach - confettiYoffset, confettiReach - confettiYoffset),
            opacity: [1,0],
            easing: "easeOutQuad",
            complete: function(anim) {
                $("#player" + position).removeClass("winning");
                $("#player" + position + " .player-animation").html("");
            },
            duration: 2000
          });
        } else {
            var dur = getRandomInt(1000, 2000);
           anime({
               targets: '.conf'+i,
               translateX: getRandomInt(-confettiReach, confettiReach),
               translateY: getRandomInt(-confettiReach - confettiYoffset, confettiReach - confettiYoffset),

               opacity: [1,0],
               easing: "easeOutQuad",
               duration: dur
             });
             anime({
                targets: '.conf'+i,
                opacity: [1,0],
                easing: "linear",
                duration: dur*1.2
              });
        }
    }
}