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
   }
  createGame($('#userid-create').val(), gameConfig.startingChips, gameConfig.startingBlinds, gameConfig.blindIncreaseTime, gameConfig.playerMoveTime, gameConfig.rebuyTime);

  $("#settings").hide();
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
                        if(parseInt($(".raise-input").val()) > $('input[type="range"]').attr("min")) {
                        console.log("test");
                            $(".raise-input").val(value);
                        }
                   $("#raise").attr("onclick", "gameRaise("+ (value - $("#player1 .bet").html()) +")");
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

//TODO add input validation
