const {
  DELAY_BETWEEN_GAME_CREATIONS,
  DELAY_BETWEEN_GAME_STARTS,
  MAX_DELAY_BETWEEN_TICKS,
  NUMBER_OF_GAMES
} = require('./config');
const { createGame } = require('./game');
const { sleep } = require('./utils');

const main = async () => {
  const server = process.argv[2];
  if (!server) {
    console.log('Example usage: node index.js ws://www.callingstation.net:9093');
    process.exit(-1);
  }

  const games = [];

  process.on('SIGINT', () => {
    games.forEach(game => game.disconnectPlayers());
    process.exit(0);
  });

  console.log(`Starting load testing of ${server}`);

  await sleep(3000); // to give the user some time to cancel

  for (let i = 1; i <= NUMBER_OF_GAMES; i++) {
    games.push(await createGame(i, server));
    await sleep(DELAY_BETWEEN_GAME_CREATIONS);
  }

  await sleep(5000); // extra time to make sure all the games have been created

  for (game of games) {
    game.start();
    await sleep(DELAY_BETWEEN_GAME_STARTS);
  }

  while (true) {
    // randomly sleep for 1 to MAX_DELAY_BETWEEN_TICKS ms
    await sleep(Math.floor(Math.random() * MAX_DELAY_BETWEEN_TICKS) + 1);
    games[Math.floor(Math.random() * NUMBER_OF_GAMES)].tick();
  }
}

main();
