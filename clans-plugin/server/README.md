# Local Paper 1.21.4 test server

This folder contains everything needed to run the Clans plugin on a local
Paper 1.21.4 server.

## One command setup

```bash
cd server
bash setup-server.sh
```

The script:

1. checks for Java 21+,
2. downloads the latest Paper 1.21.4 build into `server/paper.jar`,
3. downloads Vault into `plugins/` (used only for paid colours and taxes),
4. copies `release/Clans-1.0.0.jar` into `plugins/`,
5. creates `eula.txt` + a test `server.properties` (offline mode, RCON on),
6. starts the server with `nogui`.

## What you should see

```
[12:01:02 INFO]: [Clans] Clans enabled. 10 colours, data folder ./plugins/Clans
[12:01:03 INFO]: Done (3.000s)! For help, type "help"
```

## Testing from the console (RCON)

With the server running:

```bash
python3 rcon.py "clan list"
python3 rcon.py "clan colors"
```

Commands that need a player (create/kick/invite/...) are best tested with two
Minecraft clients joined to `localhost` (`online-mode=false`).

## Manual setup (without the script)

```bash
mkdir -p mc/plugins
mv release/Clans-1.0.0.jar mc/plugins/
# download paper-1.21.4-*.jar and Vault.jar into mc/
echo "eula=true" > mc/eula.txt
java -Xms1G -Xmx2G -jar mc/paper.jar nogui
```

## Requirements

- Paper 1.21.4 (Java 21 runtime)
- Vault (optional) - required only for paid colours and taxes
