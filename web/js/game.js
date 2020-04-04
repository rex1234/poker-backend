$( "#joinGame button" ).click(function() {
  connectToGame($('#userid').val(), '12345');
  $("#controls").hide();
});

$( "#createGame button" ).click(function() {
  createGame({
                         startingChips:  $("#startingChips").val(),
                         startingBlinds: $("#startingBlinds").val(),
                         blindIncreaseTime: $("#blindIncreaseTime").val(),
                         playerMoveTime: $("#playerMoveTime").val(),
                 });
  $("#controls").hide();
});