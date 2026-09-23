# MultiverseProgramming

Programmable computers with **Lua** inside your Minecraft server, inspired by ComputerCraft.

Place a computer in the world, insert a **floppy disk**, and validate & run your Lua code with one click — the output appears straight in chat.

Built for **Purpur / Paper 1.21.11** with Java 21.

> **Wiki**: [English](wiki/en/Home.md) · [Español](wiki/es/Home.md)
> Recipes, commands, configuration and Lua API are documented there.

## Features

- **Computer block** (default: lectern) — right‑click to open the computer GUI.
- **Advanced Computer** (default: enchanting table) — runs looping programs without the short timeout and keeps the disk inside its inventory.
- **Floppy disks** — an in-game book & quill that holds your Lua program, page by page.
- **Validate & Run button** — checks your code for errors and, if valid, executes the program right on the computer.
- **Sandboxed Lua** (Luaj) — `io`, `os`, `luajava`, `package`, `dofile` and `loadfile` are disabled, and every program has a configurable execution timeout.
- **Chat output** — everything the program prints with `print()` is sent to the player's chat.

## Requirements

- Purpur or Paper server **1.21.11** (any 1.21.x should work).
- Java **21**.
- Nothing else — Lua (Luaj) is bundled inside the plugin jar.

## Quick start

1. Drop `MultiverseProgramming.jar` into your server's `plugins/` folder and restart.
2. Craft a **Floppy Disk** (see [Recipes](wiki/en/Recipes.md)) — a fresh disk comes with a template.
3. Place a **lectern** and right‑click it — the computer GUI opens.
4. Insert the disk into **slot 0** and click the **green "✔ Validate Code" button**.
   - If the code has an error, the GUI closes and the error appears in chat.
   - If it is valid, the program runs immediately.
5. The program's `print()` output appears in your chat (`(no output)` if it printed nothing).

### Example program

Write this in the disk pages:

```lua
local nombre = "Steve"
for i = 1, 3 do
  print("Hola " .. nombre .. "! (" .. i .. ")")
end
```

## Building from source

```powershell
mvn -q -DskipTests package
```

The artifact is generated at `target/MultiverseProgramming-<version>.jar` (Luaj bundled via shade), where `<version>` matches the pom's `<version>`.

## Documentation

- **English wiki**: [wiki/en/Home.md](wiki/en/Home.md)
- **Spanish wiki**: [wiki/es/Home.md](wiki/es/Home.md)
- Recipes: [wiki/en/Recipes.md](wiki/en/Recipes.md) · [wiki/es/Recipes.md](wiki/es/Recipes.md)
- Configuration, commands and permissions: [wiki/en/Configuration.md](wiki/en/Configuration.md) · [wiki/es/Configuration.md](wiki/es/Configuration.md)
- Lua reference: [wiki/en/Lua-Scripting.md](wiki/en/Lua-Scripting.md) · [wiki/es/Lua-Scripting.md](wiki/es/Lua-Scripting.md)

## License

This project is licensed under the **GNU General Public License v3.0**. See [LICENSE](LICENSE).
