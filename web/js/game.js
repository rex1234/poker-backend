

$( "#joinGame button" ).click(function() {
  connectToGame($('#userid').val(), $('#gameid').val());
  $("#controls").hide();
  $(".game-container").show();
  $("#start").hide();
});

$( "#createGame button" ).click(function() {
    var gameConfig = {
        startingChips: $("#startingChips").val(),
        startingBlinds: $("#startingBlinds").val(),
        blindIncreaseTime: $("#blindIncreaseTime").val(),
        playerMoveTime: $("#playerMoveTime").val(),
        rebuyTime: 720
   }
  createGame($('#userid').val(), gameConfig);

  $("#controls").hide();
  $(".game-container").show();
  $("#start").show();
});

$( "#start" ).click(function() {
    startGame();
  $("#start").hide();
});

//Slider + input functionality

$('input[type="range"]').rangeslider({
  polyfill : false,
  onInit : function() {
    $("#raise").html( "Raise to<br>" + this.$element.val() );
    $(".raise-input").val(this.$element.val());
  },
   onSlide : function( position, value) {
                   $("#raise").html("Raise to<br>" + value);
                   $(".raise-input").val(value);
                   $("#raise").attr("onclick", "gameRaise("+ (value - $("#player1 .bet").html()) +")");
               }
 });