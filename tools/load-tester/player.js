const addPlayer = (name, socketServerUrl, gameUuid, gameConfig, logger, onMoveCallback, gameCreatedCallback) => {
  let gameCreatedCallbackCalled = false;

  const socket = require('socket.io-client')(socketServerUrl);

  socket.on('connect', () => {
    socket.emit('connectGame', {
      name,
      gameUUID: gameUuid,
      gameConfig,
    });
  });

  socket.on('gameState', data => {
    if (gameCreatedCallback && !gameCreatedCallbackCalled) {
      gameCreatedCallback(data.uuid);
      gameCreatedCallbackCalled = true;
    }
    if (onMoveCallback && data.user.onMove) {
      logger('On move');
      onMoveCallback(name);
    }
  });

  socket.on('error', error => {
    logger(`Server error: ${JSON.stringify(error)}`);
  })

  const startGame = () => {
    logger('Starting the game');
    socket.emit('action', { action: 'startGame' })
  };

  const doGameAction = (action) => {
    logger(`Performing game action: ${action}`);
    socket.emit('action', { action })
  };

  const leave = () => {
    logger('Leaving');
    socket.emit('leave');
  }

  return ({
    startGame,
    doGameAction,
    leave,
  });
};

module.exports = {
  addPlayer: addPlayer,
};
