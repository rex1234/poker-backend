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

log_progress "Minifying JavaScript"
echo `uglifyjs web/js/game.js` > web/js/game.js
echo `uglifyjs web/js/animations.js` > web/js/animations.js
echo `uglifyjs web/js/sockets.js` > web/js/sockets.js

log_progress "Restarting ${SERVICE} service"
systemctl restart ${SERVICE}.service

echo
echo "All done!"
