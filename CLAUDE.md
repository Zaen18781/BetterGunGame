# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## Workflow Orchestration

### 1. Plan Node Default
- Enter plan mode for ANY non-trivial task (3+ steps or architectural decisions)
- If something goes sideways, STOP and re-plan immediately — don't keep pushing
- Use plan mode for verification steps, not just building
- Write detailed specs upfront to reduce ambiguity

### 2. Subagent Strategy
- Use subagents liberally to keep main context window clean
- Offload research, exploration, and parallel analysis to subagents
- For complex problems, throw more compute at it via subagents
- One task per subagent for focused execution

### 3. Self-Improvement Loop
- After ANY correction from the user: update `tasks/lessons.md` with the pattern
- Write rules for yourself that prevent the same mistake
- Ruthlessly iterate on these lessons until mistake rate drops
- Review lessons at session start for relevant project

### 4. Verification Before Done
- Never mark a task complete without proving it works
- Diff behavior between main and your changes when relevant
- Ask yourself: "Would a staff engineer approve this?"
- Run tests, check logs, demonstrate correctness

### 5. Demand Elegance (Balanced)
- For non-trivial changes: pause and ask "is there a more elegant way?"
- If a fix feels hacky: "Knowing everything I know now, implement the elegant solution"
- Skip this for simple, obvious fixes — don't over-engineer
- Challenge your own work before presenting it

### 6. Autonomous Bug Fixing
- When given a bug report: just fix it. Don't ask for hand-holding
- Point at logs, errors, failing tests — then resolve them
- Zero context switching required from the user
- Go fix failing CI tests without being told how

---

## Task Management

1. **Plan First**: Write plan to `tasks/todo.md` with checkable items
2. **Verify Plan**: Check in before starting implementation
3. **Track Progress**: Mark items complete as you go
4. **Explain Changes**: High-level summary at each step
5. **Document Results**: Add review section to `tasks/todo.md`
6. **Capture Lessons**: Update `tasks/lessons.md` after corrections

---

## Core Principles

- **Simplicity First**: Make every change as simple as possible. Impact minimal code.
- **No Laziness**: Find root causes. No temporary fixes. Senior developer standards.
- **Minimal Impact**: Changes should only touch what's necessary. Avoid introducing bugs.

---

## Build

```bash
./gradlew build -x test   # build JAR (skips tests)
./gradlew build            # full build with tests
```

Output: `build/libs/BetterCore-*.jar`

---

## Architecture

BetterCore is a PaperMC plugin (Java 21, Minecraft 1.21+) organized around a **module system**.

### Module System

Every feature lives in its own module under `src/main/java/de/ZaenCotti/bettercore/modules/{name}/`.

- Annotate with `@ModuleInfo(id = "...", name = "...", version = "...")`
- Extend `AbstractModule` and implement `enable()` / `disable()`
- Register in `BetterCore.java` via `moduleRegistry.register(new XModule(this))`
- Modules are loaded/enabled automatically by `ModuleRegistry`

### Config & Messages

Each module gets:
- `ModuleConfig` → `modules/{id}/config.yml` (settings)
- `MessageConfig` → `modules/{id}/messages.yml` (player-facing text, supports MiniMessage)

Access via `getConfig()` and `getMessages()` from `AbstractModule`.

### Storage

`ConnectionPool` (HikariCP) supports SQLite (default) and MySQL. Get a connection from `plugin.getConnectionPool().getConnection()`. Always use try-with-resources. Cache DB results with `CacheManager` (Caffeine).

### GUIs

Use **triumph-gui** (`StaticGui`, `PaginatedGui`). Build items with `ItemBuilder`. Call `gui.update()` after `gui.setItem()` when the GUI is already open.

### Commands

Declare in `plugin.yml`. Register in `enable()` via `registerCommand("name", new XCommand(this))`.

### Utilities

| Class | Purpose |
|---|---|
| `TextUtils` | MiniMessage parsing, legacy `&`/`§` codes, PlaceholderAPI, small caps |
| `ItemBuilder` | Fluent item construction |
| `SoundUtils` | XSeries-based cross-version sound playback |
| `ServerScheduler` | Folia-compatible task scheduling (use instead of `Bukkit.getScheduler()`) |
| `TeleportUtils` | Safe teleport with async chunk loading |

### Persistence Pattern

Modules that need per-player YAML persistence (not DB) use a `data.yml` file:
```
modules/{id}/data.yml
```
Load with `YamlConfiguration.loadConfiguration(dataFile)`, save with `config.save(dataFile)`.

### Chat / Nametag

- **ChatModule**: formats chat using MiniMessage. Player name color injected as `Placeholder.component("player", coloredName)` via the `<player>` tag in `config.yml` format string.
- **NametagModule**: manages PacketEvents-based nametags. Re-applies `hideVanillaNametag()` every update cycle to counteract LuckPerms scoreboard team changes.
- **NametagGUI** reads colors 1:1 from **ChatColorGUI** (via `getNormalEntries()` / `getCustomEntries()`). Change colors in chat config → automatically reflected in nametag GUI.
- `ChatColorGUI.ColorEntry` and its entry lists are `public` for cross-module access.

### Key Files

- `BetterCore.java` — plugin entry point, module registration
- `ModuleRegistry.java` — loads/enables/disables modules
- `AbstractModule.java` — base class all modules extend
- `utils/TextUtils.java` — primary text/color utility
- `modules/chat/ChatModule.java` — chat formatting + color persistence
- `modules/nametag/NametagModule.java` — nametag display + persistence
- `modules/chat/DeathMessageManager.java` — custom death messages with item detection

### MiniMessage

All player-facing strings use MiniMessage tags (`<red>`, `<gradient:#ff0000:#0000ff>`, etc.). Legacy `&` codes are supported in config via `TextUtils.translateLegacyCodes()`.
