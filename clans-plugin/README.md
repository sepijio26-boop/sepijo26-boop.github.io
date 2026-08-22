# Clans — Paper 1.21.4 Clan Plugin

A light-weight, configurable clan plugin for **Paper 1.21.4**. All messages
are editable in `messages.yml` and all rule settings in `config.yml`.

**Features**

- `/clan create <name> <color>` — create a clan and become its owner.
- Clan tags (**2–4 chars**, English letters auto-uppercased, digits and `#`
  allowed, no duplicates): the tag is shown in **chat**, in the **tab list**
  and its colour is applied to the **name tag**.
- **10 tag colours** with configurable Vault prices; **red, blue, yellow are free**.
- Hypixel-style messages: blue/green with pink highlights and a single
  `&9Clan:` tag.
- Owner commands: `disband`, `kick`, `invite`, `pvp`, `tax`, `public`,
  `private`, `promote`, `demote`, `color`, `deposit/withdraw/vault`.
- Public clans: `/clan join <clan>`. Private clans: invite-only.
- Invites are **clickable chat messages** (also `/clan accept` / `/clan decline`).
- Kicked players from a **public** clan can't rejoin for **7 days** unless
  re-invited.
- Clan PvP: `pvp off` blocks all clan damage; `pvp on` (default) allows
  member-vs-member fights while the owner and OGs are always protected.
- Hourly **tax** (default `salary` mode): the clan **vault pays** the set
  amount to every non-OG member every hour and the OGs additionally split
  20% of each payment; the owner is notified of each cycle (even if they
  were offline). A `collect` mode (members pay into the vault) is available
  in config.yml.
- Vault economy (optional; paid colours + taxes need it).

---

## Install

```bash
# 1. Build (or use the prebuilt jar in release/)
mvn package

# 2. Copy the jar
cp target/Clans-1.0.0.jar <server>/plugins/

# 3. (optional) Vault for paid colours/taxes
#    https://www.spigotmc.org/resources/vault.34315/

# 4. Start the server
java -Xms1G -Xmx2G -jar paper-1.21.4-*.jar nogui
```

A full local test setup is in [`server/`](server/README.md)
(`bash server/setup-server.sh` gives you a running Paper 1.21.4 server in
seconds).

---

## Commands

| Command | Who | What |
|---|---|---|
| `/clan create <name> <color>` | player | create a clan (1 free tag colour can be picked; paid colours are charged via Vault) |
| `/clan colors` | anyone | list colours and prices |
| `/clan join <clan>` | player | join a **public** clan |
| `/clan accept [clan]` | player | accept a pending invite (or click the chat button) |
| `/clan decline [clan]` | player | decline a pending invite |
| `/clan leave` | member | leave your clan (owner must disband) |
| `/clan disband` | owner | delete the clan |
| `/clan kick <player>` | owner | remove a member (public clans: 7-day rejoin ban unless re-invited) |
| `/clan invite <player>` | owner | send a clickable invite |
| `/clan pvp <on\|off>` | owner | toggle clan PvP (default **on**) |
| `/clan tax <amount\|off>` | owner | hourly tax per member (default off) |
| `/clan public` / `/clan private` | owner | toggle join mode (default **public**) |
| `/clan promote <player> <admin\|og>` | owner | rank up a member |
| `/clan demote <player>` | owner | back to member |
| `/clan color <color>` | owner | change the tag colour (paid colours are charged) |
| `/clan deposit <amount>` | member | put economy money into the clan vault |
| `/clan withdraw <amount>` | owner | take economy money from the clan vault |
| `/clan vault` | member | vault balance + tax info |
| `/clan info [clan]` | anyone | clan details and member list |
| `/clan list` | anyone | all clans |
| `/clan help` | anyone | usage summary |
| `/clan reload` | admin (`clans.admin`) | reload config.yml + messages.yml |

---

## Clan names

> 2–4 characters. English letters are handled case-insensitively and always
> shown uppercased (`sepi` → `SEPI`). Digits and `#` are allowed. No spaces
> or other symbols. Names are unique.

Valid: `SEPI`, `13#`, `S3P#`, `فا` (any letters) · Invalid: `A`, `ABCDE`, `A B`, `A$B`.

## Colours (`config.yml`)

| Colour | Cost |
|---|---|
| `red` | free |
| `blue` | free |
| `yellow` | free |
| `green` | 20,000 |
| `gold` | 25,000 |
| `aqua` | 30,000 |
| `purple` | 35,000 |
| `pink` | 40,000 |
| `white` | 50,000 |
| `gray` | 75,000 |

Every entry has `cost`, `legacy` (chat code like `&c`) and `chat`
(Bukkit `ChatColor` name used for the name-tag colour).

## Tag display

- **Chat** — `chat.format`, default `"{name} {tag}: {message}"`
  (tag after the name, e.g. `SEPIO &c[SEPI]: hello`).
- **Tab list** — `tab.format`, default `"{name} {tag}"`.
- **Name tag** — the player's name is *only* coloured with the clan colour
  (no tag text), implemented with scoreboard teams, no extra plugin needed.

Placeholders: `{name}`, `{tag}`, `{message}` (chat only).
To put the tag **before** the name, change the formats to
`"{tag} {name}: {message}"` and `"{tag} {name}"`.

## PvP rules

| Clan setting | Owner/OG as victim | Normal member as victim |
|---|---|---|
| `pvp on` (default) | **protected** | can be damaged by other clan members |
| `pvp off` | protected | protected |

The owner and OGs can always damage other members; members can never damage
the owner or OGs (melee and projectiles are handled).

## Taxes (`config.yml` → `tax`)

- `mode` — `salary` (default) or `collect`.
- `interval-minutes` — how often taxes are processed (default 60).
- `og-percent` — the OG cut (default 20%).
- `exempt-owner` — the owner is never part of the cycle (default true).
- Per clan the owner sets `/clan tax <amount>` or `/clan tax off`.

With the default **salary** mode, every interval the plugin takes `<amount>`
*from the clan vault* and deposits it into the account of every non-OG
member; the OGs additionally split <og-percent> of each payment (both come
out of the vault, so fund it with `/clan deposit`). With **collect** mode
every non-OG member pays `<amount>` into the vault and the OGs split
<og-percent> of the collected amount. In both cases the owner gets a chat
notification for every cycle, even after being offline.

## Invites & kicks

- Invites expire after `invite.expiry-minutes` (default 10).
- Kick: the player is fully removed. If the clan is **public** they can't
  `/clan join` for `kick-cooldown-days` (default 7) — **unless the owner
  invites them again**, in which case the invite always works.

## Configuration files

- `plugins/Clans/config.yml` — colours, defaults, invite/kick/tax settings,
  chat/tab/name-tag display.
- `plugins/Clans/messages.yml` — every plugin message (English by default):
  `prefix: "&9Clan: &b"` plus `error.*`, `clan.*`, `usage.*`, `help.lines`, etc.
- `plugins/Clans/data/clans.yml` / `players.yml` — saved data (safe to delete
  to wipe clans).

## Permissions

| Node | Default | Description |
|---|---|---|
| `clans.use` | true | use `/clan` |
| `clans.admin` | op | `/clan reload` |

## Build

```bash
mvn clean package          # needs Java 21 + Maven
# -> target/Clans-1.0.0.jar
```

The checked-in `release/Clans-1.0.0.jar` is a prebuilt jar compiled against
the real Paper 1.21.4 API surface (the sandbox used a wasm OpenJDK 23 javac
plus API stubs and patched the class files to Java 17 bytecode, which Paper's
Java 21 runtime loads normally).

## Tests

`clans-plugin/sandbox/build.mjs` compiles the plugin and runs **89 headless
logic tests** (name validation, colour registry, create/join/invite/accept/
decline, kick cooldown + re-invite, promote/demote, PvP rule matrix, tax
distribution and vault deposits/withdrawals) inside a Java 23 wasm VM:

```bash
# inside the sandbox where the toolchain is provisioned:
node sandbox/build.mjs     # jars + tests in one step
```

> Note: this sandbox has no internet access to PaperMC/Mojang, so the Paper
> server itself could not be booted here. `bash server/setup-server.sh` on
> any machine with internet gives you the exact local Paper 1.21.4 server
> with the plugin pre-installed — see `server/README.md` for expected output.

---

## راهنمای سریع (فارسی)

1. `bash server/setup-server.sh` → سرور Paper 1.21.4 با پلاگین بالا میاد.
2. داخل بازی: `/clan create SEPI red` → کلن با تگ قرمز `[SEPI]` ساخته میشه.
   توی چت و تب بعد از اسمت میاد؛ روی اسم هم فقط رنگش اعمال میشه.
3. رنگ‌ها: `red`, `blue`, `yellow` رایگان؛ بقیه با پول (Vault) خریده میشن:
   `/clan colors`
4. صاحب کلن: `/clan invite <player>` (دعوت قابل کلیک)، `/clan kick`,
   `/clan pvp on|off`، `/clan tax 200`، `/clan public|private`،
   `/clan promote <player> admin|og`، `/clan disband`.
5. عضوی که از کلن **پابلیک** کیک شده ۷ روز نمی‌تونه `/clan join` بزنه مگر
   اینکه دوباره دعوت بشه.
6. تکس هر ساعت از همه‌ی اعضا (بجز OG) گرفته میشه؛ ۸۰٪ خزانه‌ی کلن، ۲۰٪ به OGها.
   اونر هم پیام جمع‌آوری رو می‌گیره.
7. همه‌ی متن‌ها در `plugins/Clans/messages.yml` و تنظیمات در
   `plugins/Clans/config.yml` قابل تغییرند.
