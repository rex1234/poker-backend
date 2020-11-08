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
cp -r web/img dist
cp -r web/js dist
cp -r web/lib dist
cp -r web/sounds dist
cp -r web/styles dist
cp web/game.html dist

log_progress "Minifying JavaScript"
uglifyjs dist/js/game.js dist/js/animations.js dist/js/sockets.js -o dist/js/pokrio.js --mangle toplevel
perl -i -p0e 's/<!-- BUNDLE START -->.*?<!-- BUNDLE END -->/<script src="js/pokrio.js"></script>/s'

log_progress "Restarting ${SERVICE} service"
systemctl restart ${SERVICE}.service

echo
echo "All done!"
