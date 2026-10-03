#!/usr/bin/env bash
#
# Arranca un servidor Paper real con el jar del plugin y comprueba que carga, se habilita,
# levanta el portal web, responde a comandos de consola y se apaga limpio, sin errores de enlace
# con la API de esa version.
#
#   .github/scripts/server-smoke-test.sh <version-de-minecraft> <ruta-al-jar>
#
# El MISMO jar (compilado una vez contra 1.21.11 con Java 21) se prueba en todas las versiones:
# un metodo que falte o haya cambiado en una API mas nueva solo aparece al enlazar las clases en
# ese servidor, nunca al compilar.
#
set -euo pipefail

VERSION="$1"
PLUGIN_JAR="$(realpath "$2")"
WORK="server-$VERSION"
START_TIMEOUT="${START_TIMEOUT:-600}"
SETTLE_SECONDS="${SETTLE_SECONDS:-20}"
WEB_PORT="${WEB_PORT:-8080}"

rm -rf "$WORK"
mkdir -p "$WORK/plugins"
cd "$WORK"

echo "::group::Descargar Paper $VERSION"
BUILDS="$(curl -fsSL "https://fill.papermc.io/v3/projects/paper/versions/$VERSION/builds")"
URL="$(echo "$BUILDS" | jq -r '([.[] | select(.channel == "STABLE")] + .)[0].downloads."server:default".url')"
SHA="$(echo "$BUILDS" | jq -r '([.[] | select(.channel == "STABLE")] + .)[0].downloads."server:default".checksums.sha256')"
if [ -z "$URL" ] || [ "$URL" = "null" ]; then
    echo "No hay build de Paper para $VERSION" >&2
    exit 1
fi
echo "Build de Paper: $URL"
curl -fsSL -o paper.jar "$URL"
echo "$SHA  paper.jar" | sha256sum -c -
echo "::endgroup::"

cp "$PLUGIN_JAR" plugins/
echo "eula=true" > eula.txt
cat > server.properties <<'PROPERTIES'
online-mode=false
spawn-protection=0
max-players=2
view-distance=4
simulation-distance=4
PROPERTIES

echo "::group::Arrancar el servidor"
mkfifo console
java -Xms1G -Xmx3G -jar paper.jar --nogui < console > server.log 2>&1 &
SERVER_PID=$!
# Mantener abierta la entrada de la consola para que el servidor no la vea terminar.
exec 3> console

started=false
for _ in $(seq 1 "$START_TIMEOUT"); do
    if grep -q "Done (" server.log; then
        started=true
        break
    fi
    if ! kill -0 "$SERVER_PID" 2>/dev/null; then
        break
    fi
    sleep 1
done
echo "::endgroup::"

if [ "$started" != true ]; then
    echo "El servidor no termino de arrancar" >&2
    tail -n 200 server.log >&2
    kill "$SERVER_PID" 2>/dev/null || true
    exit 1
fi

# Dejar correr lo programado para despues del arranque (recetas, tareas de tortugas).
sleep "$SETTLE_SECONDS"

# El portal web corre en su propio hilo dentro del servidor: si responde, el plugin esta vivo.
failed=false
if curl -fsS --max-time 10 "http://127.0.0.1:$WEB_PORT/api/blueprints" > web-blueprints.json; then
    echo "Portal web OK: $(head -c 200 web-blueprints.json)"
else
    echo "::error::Paper $VERSION: el portal web no respondio en el puerto $WEB_PORT"
    failed=true
fi

echo "mvprog web" >&3
echo "mvprog reload" >&3
sleep 5
echo "stop" >&3

for _ in $(seq 1 120); do
    if ! kill -0 "$SERVER_PID" 2>/dev/null; then
        break
    fi
    sleep 1
done
if kill -0 "$SERVER_PID" 2>/dev/null; then
    echo "El servidor no se apago a tiempo" >&2
    kill -9 "$SERVER_PID" 2>/dev/null || true
fi
exec 3>&-

echo "::group::Log del servidor"
cat server.log
echo "::endgroup::"

require() {
    if ! grep -qE "$1" server.log; then
        echo "::error::Paper $VERSION: falta en el log: $2"
        failed=true
    fi
}
forbid() {
    if grep -nE "$1" server.log; then
        echo "::error::Paper $VERSION: $2"
        failed=true
    fi
}

require "Enabling MultiverseProgramming" "el plugin no se habilito"
require "MultiverseProgramming enabled \(" "onEnable no llego a su final"
require "\[WebPortal\] Web server successfully running" "el portal web no arranco"
require "Configuration reloaded" "/mvprog reload no se ejecuto"
require "Disabling MultiverseProgramming" "el plugin no se deshabilito al apagar"
forbid "Could not load 'plugins/MultiverseProgramming|Error occurred while enabling MultiverseProgramming" \
       "el servidor rechazo cargar o habilitar el plugin"
forbid "NoSuchMethodError|NoSuchFieldError|NoClassDefFoundError|AbstractMethodError|IncompatibleClassChangeError" \
       "una clase o miembro del plugin no enlaza con esta API"
forbid "^[[:space:]]*at com\.multiverse\.programming\." "el codigo del plugin lanzo una excepcion"

if [ "$failed" = true ]; then
    exit 1
fi
echo "Paper $VERSION: el plugin cargo, se habilito, sirvio el portal web, recargo y se apago limpio."
