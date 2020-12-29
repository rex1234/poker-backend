const { DELAY_BETWEEN_PLAYER_ADDITIONS, PLAYERS_PER_GAME } = require('./config');
const { addPlayer } = require('./player');
const { sleep } = require('./utils');

const GAME_CONFIG = {
  startingChips: 1000,
    startingBlinds: 10,
    blindIncreaseTime: 600,
    playerMoveTime: 30,
    rebuyTime: 0,
    maxRebuys: 0,
};

const GAME_ACTIONS = ['call', 'check', 'fold'];

const createGame = async (gameNumber, socketServerUrl) => {
  const gameLogger = msg => console.log(`[Game ${gameNumber}] ${msg}`);
  const createPlayerLogger = (playerName) => (msg) => console.log(`[Game ${gameNumber}] [Player ${playerName}] ${msg}`);

  gameLogger('Creating');

  let playerOnMove;
  const players = [];

  let gameCreatedCallback = async (gameUuid) => {
    for (let i = 1; i < PLAYERS_PER_GAME; i++) {
      await sleep(DELAY_BETWEEN_PLAYER_ADDITIONS);
      players.push(addPlayer(i, randomString(32), false, socketServerUrl, gameUuid, undefined, createPlayerLogger(i), onMoveCallback));
    }
    gameLogger('Created');
  }

  const onMoveCallback = (playerOnMoveName) => {
    playerOnMove = playerOnMoveName;
  }

  players.push(addPlayer(0,  randomString(32), true, socketServerUrl, undefined, GAME_CONFIG, createPlayerLogger(0), onMoveCallback, gameCreatedCallback));

  const start = () => players[0].startGame();

  const tick = () => {
    // if the randomly selected action is illegal (i.e. player cannot check), we don't care, it's just a load test
    players[playerOnMove].doGameAction(GAME_ACTIONS[Math.floor(Math.random() * 3)]);
  }

  const disconnectPlayers = () => players.forEach(player => player.leave());

  return ({
    start,
    tick,
    disconnectPlayers,
  })
};

function randomString(length) {
  // declare all characters
  const characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';

  let result = '';
  const charactersLength = characters.length;
  for (let i = 0; i < length; i++) {
    result += characters.charAt(Math.floor(Math.random() * charactersLength));
  }

  return result;
}

module.exports = {
  createGame: createGame,
};
