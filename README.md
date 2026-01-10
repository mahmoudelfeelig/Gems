# Gems
a Minecraft mod that adds gems with unique powers for each holder.

- Docs index: `docs/README.md`
- Player guide: `gameplay.md`
- Developer mechanics reference (source-of-truth): `checklist.md`
- Server owner guide: `server-ops.md`
- Balance/config reference: `balance.md`
- Balancing: edit `config/gems/balance.json` (generated on first run) and run `/gems reloadBalance` (op level 2+) to apply.

## Server install

- Download the mod jar and place it in your server’s `mods/` folder (with Fabric Loader + Fabric API installed).
- If you’re building from source: `./gradlew build` and use `build/libs/*.jar`.

## In-game config UI

- Install Mod Menu, then open the Gems config screen from Mod Menu.

## Testing

- Unit tests: `./gradlew test`
- End-to-end integration (Fabric GameTest): `./gradlew gametest` (writes `build/reports/gametest.xml`, uses `run/gametest/`)

## Developer setup

- Prereqs: JDK 21, Git, and a recent Gradle wrapper (included).
- Clone and open the project:
  - `git clone <repo-url>`
  - `cd Gems`
- Run in dev:
  - Client: `./gradlew runClient`
  - Server: `./gradlew runServer`
  - GameTest server: `./gradlew gametest`
- Build a release jar: `./gradlew build` (output in `build/libs/`).
