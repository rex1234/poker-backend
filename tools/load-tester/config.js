require('dotenv').config();

// if you aren't happy with the defaults, set the variables in .env
module.exports = {
  NUMBER_OF_GAMES: process.env.NUMBER_OF_GAMES || 50,
  PLAYERS_PER_GAME: process.env.PLAYERS_PER_GAME || 9,
  DELAY_BETWEEN_GAME_CREATIONS: process.env.DELAY_BETWEEN_GAME_CREATIONS || 1000, // ms
  DELAY_BETWEEN_PLAYER_ADDITIONS: process.env.DELAY_BETWEEN_PLAYER_ADDITIONS || 100, // ms
  DELAY_BETWEEN_GAME_STARTS: process.env.DELAY_BETWEEN_GAME_STARTS || 500, // ms
  MAX_DELAY_BETWEEN_TICKS: process.env.MAX_DELAY_BETWEEN_TICKS || 100, // ms
};
