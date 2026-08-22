#!/usr/bin/env bash
# ------------------------------------------------------------------
# One-command local Paper 1.21.4 test server for the Clans plugin.
#
# Usage:  bash setup-server.sh
# It downloads Paper 1.21.4 (latest build) + Vault, drops the plugin
# into plugins/, writes eula.txt/server.properties and starts the
# server in the current directory.
#
# Requires: Java 21+, curl (or wget), internet access to
#   fill.papermc.io, api.papermc.io and github.com
# ------------------------------------------------------------------
set -euo pipefail

MC_VERSION="1.21.4"
SERVER_FOLDER="$(cd "$(dirname "$0")" && pwd)"
PLUGIN_JAR="${SERVER_FOLDER}/../release/Clans-1.0.0.jar"

echo "==> Clans plugin local server setup (Paper ${MC_VERSION})"

if ! command -v java >/dev/null 2>&1; then
  echo "ERROR: Java 21 or newer is required but was not found." >&2
  exit 1
fi
JAVA_MAJOR="$(java -version 2>&1 | sed -n '1s/.*version "\([0-9]*\).*/\1/p')"
if [ "${JAVA_MAJOR:-0}" -lt 21 ]; then
  echo "ERROR: Java 21+ required, found $(java -version 2>&1 | head -1)" >&2
  exit 1
fi

if [ ! -f "${PLUGIN_JAR}" ]; then
  echo "ERROR: ${PLUGIN_JAR} not found. Build it first (mvn package) or use the prebuilt jar in release/." >&2
  exit 1
fi

mkdir -p "${SERVER_FOLDER}/plugins"

# ---- Paper 1.21.4 -------------------------------------------------
if [ ! -f "${SERVER_FOLDER}/paper.jar" ]; then
  echo "==> Downloading Paper ${MC_VERSION}..."
  if command -v curl >/dev/null 2>&1; then
    BUILD_URL="https://fill.papermc.io/v3/projects/paper/versions/${MC_VERSION}/builds/latest"
    BUILD_INFO="$(curl -fsSL "${BUILD_URL}")"
    BUILD="$(echo "${BUILD_INFO}" | sed -n 's/.*"build":\([0-9]*\).*/\1/p' | head -1)"
    if [ -z "${BUILD}" ]; then
      BUILD="$(curl -fsSL "https://api.papermc.io/v2/projects/paper/versions/${MC_VERSION}/builds" | sed -n 's/.*"build":\([0-9]*\).*/\1/p' | tail -1)"
    fi
    echo "==> Paper ${MC_VERSION} build ${BUILD}"
    curl -fL -o "${SERVER_FOLDER}/paper.jar" \
      "https://api.papermc.io/v2/projects/paper/versions/${MC_VERSION}/builds/${BUILD}/downloads/paper-${MC_VERSION}-${BUILD}.jar"
  elif command -v wget >/dev/null 2>&1; then
    echo "Please install curl to download Paper automatically (or place paper.jar yourself)." >&2
    exit 1
  fi
fi

# ---- Vault (economy for paid colours + taxes) ----------------------
if [ ! -f "${SERVER_FOLDER}/plugins/Vault.jar" ]; then
  echo "==> Downloading Vault..."
  (curl -fL -o "${SERVER_FOLDER}/plugins/Vault.jar" \
     "https://github.com/MilkBowl/Vault/releases/latest/download/Vault.jar") || true
fi

# ---- Plugin ---------------------------------------------------------
cp "${PLUGIN_JAR}" "${SERVER_FOLDER}/plugins/Clans-1.0.0.jar"

# ---- EULA / properties ---------------------------------------------
if [ ! -f "${SERVER_FOLDER}/eula.txt" ]; then
  echo "eula=true" > "${SERVER_FOLDER}/eula.txt"
fi
if [ ! -f "${SERVER_FOLDER}/server.properties" ]; then
  cat > "${SERVER_FOLDER}/server.properties" <<'CFG'
online-mode=false
motd=Clans plugin test server
level-type=flat
view-distance=6
simulation-distance=6
max-players=20
enable-rcon=true
rcon.port=25575
rcon.password=clans123
enable-command-block=false
CFG
fi

cd "${SERVER_FOLDER}"
echo "==> Starting Paper ${MC_VERSION} (Ctrl+C to stop)..."
exec java -Xms1G -Xmx2G -jar paper.jar nogui
