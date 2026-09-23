# MultiverseProgramming

Programmable computers with **Lua** inside your Minecraft server, inspired by ComputerCraft.

Place a computer in the world, insert a **floppy disk**, validate your Lua code with one click, and run it straight from the chat.

Built for **Purpur / Paper 1.21.11** with Java 21.

> **Wiki**: [English](wiki/en/Home.md) · [Español](wiki/es/Home.md)

## Features

- **Computer block** (default: lectern) — right‑click to open the computer GUI.
- **Floppy disks** — an in-game book & quill that holds your Lua program, page by page.
- **Validate button** — checks your code for errors and reports them in chat.
- **Run from chat** — `/pc run` executes the program on the disk you are holding.
- **Sandboxed Lua** (Luaj) — `io`, `os`, `luajava`, `package`, `dofile` and `loadfile` are disabled, and every program has a configurable execution timeout.
- **Chat output** — everything the program prints with `print()` is sent to the player's chat.

## Requirements

- Purpur or Paper server **1.21.11** (any 1.21.x should work).
- Java **21**.
- Nothing else — Lua (Luaj) is bundled inside the plugin jar.

## Quick start

1. Drop `MultiverseProgramming.jar` into your server's `plugins/` folder and restart.
2. `/pc disco` (alias of `/pc disk`) — receive a floppy disk. A fresh disk comes with a template.
3. Place a **lectern** and right‑click it — the computer GUI opens.
4. Insert the disk into **slot 0** and click the **green "Validar código" button**.
   - If the code has an error, the GUI closes and the error appears in chat.
   - If it is correct, the game tells you the code is valid and to run it.
5. Hold the disk in your hand and run **`/pc run`**.
   - The program's `print()` output appears in your chat.

### Example program

Write this in the disk pages:

```lua
local nombre = "Steve"
for i = 1, 3 do
  print("Hola " .. nombre .. "! (" .. i .. ")")
end
```

## Commands

| Command | Description |
|---|---|
| `/pc disco` | Gives you a (new) floppy disk |
| `/pc run` | Executes the program on the disk in your hand |
| `/pc help` | Shows the command list |
| `/pc` (no argument) | Same as `/pc help` |

Aliases: `computador`, `computadora`, `disco`.

## Permissions

| Permission | Default | Description |
|---|---|---|
| `multiverseprogramming.use` | `true` | Allows using computers and disks |

## Configuration (`config.yml`)

```yaml
# Block that acts as a computer (right-click opens the GUI).
computer-block: LECTERN

# Maximum execution time of a program, in milliseconds.
execution-timeout-ms: 3000
```

- `computer-block` can be any [Material](https://jd.papermc.io/paper/1.21/org/bukkit/Material.html) name, e.g. `BARREL` or `DISPENSER`.
- Programs that take longer than `execution-timeout-ms` are interrupted with a warning in chat.

## Lua sandbox notes

- Only the safe standard libraries are available (`table`, `string`, `math`, `coroutine`, `bit32`).
- `io`, `os`, `luajava`, `package`, `dofile` and `loadfile` are **removed**.
- Every `print()` call is captured and sent to the executing player's chat.
- Runtime errors and infinite loops are caught and reported instead of crashing the server.

## Building from source

```powershell
mvn -q -DskipTests package
```

The artifact is generated at `target/MultiverseProgramming.jar` (Luaj bundled via shade).

## Documentation

- **English wiki**: [wiki/en/Home.md](wiki/en/Home.md)
- **Spanish wiki**: [wiki/es/Home.md](wiki/es/Home.md)

## License

This project is licensed under the **GNU General Public License v3.0**. See [LICENSE](LICENSE).