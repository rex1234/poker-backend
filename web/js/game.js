$( "#joinGame button" ).click(function() {
  connectToGame($('#userid').val(), '12345');
  $("#controls").hide();
  $(".game").show();
  $("#start").hide();
});

$( "#createGame button" ).click(function() {
  createGame(
        $('#userid').val()
  );
  $("#controls").hide();
  $(".game").show();
  $("#start").show();
});

$( "#start" ).click(function() {
    startGame();
  $("#start").hide();
});