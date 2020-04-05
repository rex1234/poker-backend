

$( "#joinGame button" ).click(function() {
  connectToGame($('#userid').val(), '12345');
  $("#controls").hide();
  $(".game-container").show();
  $("#start").hide();
});

$( "#createGame button" ).click(function() {
  createGame(
        $('#userid').val()
  );
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
  onSlide : function( position, value ) {
      $("#raise").html("Raise to<br>" + value );
      $(".raise-input").val(value);
      $("#raise").attr("onclick", "gameRaise("+ value +")");
  }
});

$(".raise-input").on('keyup', function(e) {
  var $inputRange = $('[data-rangeslider]', e.target.parentNode);
  var value = $('input[type="number"]', e.target.parentNode)[0].value;
  $("#raise").attr("onclick", "gameRaise("+ value +")");
  $inputRange .val(value) .change();
});