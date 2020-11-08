#!/bin/sh

set -e

log_progress() {
  echo
  echo "============================================================"
  echo "$1"
  echo "============================================================"
}

DIRECTORY=$(pwd)

case $DIRECTORY in
  "/root/projects/poker-backend")
    SERVICE="pokrio"
    ;;
  "/root/projects/poker-backend-dev")
    SERVICE="pokriodev"
    ;;
  *)
    echo "Not in a deployment directory, doing nothing"
    exit 1
    ;;
esac

echo "Updating ${SERVICE} in ${DIRECTORY}"

log_progress "Fetching new sources"
git fetch --all
git checkout master

log_progress "Building the backend"
./gradlew fatJar
cp ./build/libs/pokrio-1.0.jar pokrio.jar

log_progress "Copying the frontend code"
mkdir -p dist
cp web/img dist
cp web/js dist
cp web/lib dist
cp web/sounds dist
cp web/styles dist
cp game.html dist

log_progress "Minifying JavaScript"
echo `uglifyjs dist/js/game.js` > dist/js/game.js
echo `uglifyjs dist/js/animations.js` > dist/js/animations.js
echo `uglifyjs dist/js/sockets.js` > dist/js/sockets.js

log_progress "Restarting ${SERVICE} service"
systemctl restart ${SERVICE}.service

echo
echo "All done!"
