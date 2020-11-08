#!/bin/sh

set -e

log_progress() {
  echo
  echo "============================================================"
  echo "$1"
  echo "============================================================"
}

JAR_NAME="pokrio.jar"
RUN_SCRIPT_NAME="run.sh"

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
cp ./build/libs/pokrio-1.0.jar ${JAR_NAME}

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
perl -i -p0e 's/<!-- BUNDLE START -->.*?<!-- BUNDLE END -->/<script src="js\/pokrio.js"><\/script>/s' dist/game.html

log_progress "Updating run script"
rm -f ${RUN_SCRIPT_NAME}

cat > ${RUN_SCRIPT_NAME} << EOF
java -jar ${JAR_NAME}
EOF

chmod +x ${RUN_SCRIPT_NAME}

log_progress "Restarting ${SERVICE} service"
systemctl restart ${SERVICE}.service

echo
echo "All done!"
