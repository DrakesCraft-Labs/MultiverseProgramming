# MultiverseProgramming

Programmable computers with **Lua** inside your Minecraft server, inspired by ComputerCraft.

Place a computer in the world, insert a **floppy disk**, and validate & run your Lua code with one click — the output appears straight in chat.

Built for **Purpur / Paper 1.21.11** with Java 21.

> **Wiki**: [English](wiki/en/Home.md) · [Español](wiki/es/Home.md)

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
2. Craft a **Floppy Disk** (paper surrounded by an iron ingot) — a fresh disk comes with a template.
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

## Commands

| Command | Description |
|---|---|
| `/pc give <item>` | **Admin**: gives you a custom item (`floppydisk`, `computer`, `advancedcomputer`) |
| `/pc help` | Shows the command list |
| `/pc` (no argument) | Same as `/pc help` |

Aliases: `computador`, `computadora`, `disco`.

## Permissions

| Permission | Default | Description |
|---|---|---|
| `multiverseprogramming.use` | `true` | Allows using computers and disks |
| `multiverseprogramming.admin` | `op` | Allows giving custom items (`/pc give`) |

## Configuration (`config.yml`)

```yaml
# Block that acts as a computer (right-click opens the GUI).
computer-block: LECTERN

# Maximum execution time of a program, in milliseconds.
execution-timeout-ms: 3000

# Block that acts as an advanced computer (loops, no short timeout).
advanced-computer-block: ENCHANTING_TABLE

# Maximum execution time on an advanced computer, in milliseconds. 0 = no limit.
advanced-execution-timeout-ms: 0
```

- The `*-block` options accept any [Material](https://jd.papermc.io/paper/1.21/org/bukkit/Material.html) name, e.g. `BARREL` or `DISPENSER`.
- Programs that take longer than `execution-timeout-ms` on a regular computer are interrupted with a warning in chat.
- On an advanced computer a program runs until it ends (`0` = unlimited) and can be stopped by clicking the button again. Its output is streamed to chat as the program prints.

## Crafting recipes

Everything is obtainable in survival — no commands needed. These are the recipes as seen in the 3×3 crafting table.

### Floppy Disk

```text
+---+---+---+
|   | P |   |
+---+---+---+
| P | I | P |
+---+---+---+
|   | P |   |
+---+---+---+
```

**Legend:** `P` = Paper · `I` = Iron Ingot → **1 Floppy Disk**

### Computer (lectern)

```text
+---+---+---+
| S | S | S |
+---+---+---+
|   | B |   |
+---+---+---+
|   | S |   |
+---+---+---+
```

**Legend:** `S` = Wooden Slab (any wood) · `B` = Bookshelf → **1 Computer**

### Advanced Computer (enchanting table)

Requires the regular **Computer** first (vanilla lectern recipe):

```text
+---+---+---+
|   | C |   |
+---+---+---+
| D | O | D |
+---+---+---+
| O | O | O |
+---+---+---+
```

**Legend:** `C` = Computer (lectern) · `D` = Diamond · `O` = Obsidian → **1 Advanced Computer**

## Lua sandbox notes

- Only the safe standard libraries are available (`table`, `string`, `math`, `coroutine`, `bit32`).
- `io`, `os`, `luajava`, `package`, `dofile` and `loadfile` are **removed**.
- Every `print()` call is captured and sent to the executing player's chat.
- Runtime errors and infinite loops are caught and reported instead of crashing the server.

## Building from source

```powershell
mvn -q -DskipTests package
```

The artifact is generated at `target/MultiverseProgramming-<version>.jar` (Luaj bundled via shade), where `<version>` matches the pom's `<version>`.

## Documentation

- **English wiki**: [wiki/en/Home.md](wiki/en/Home.md)
- **Spanish wiki**: [wiki/es/Home.md](wiki/es/Home.md)

## License

This project is licensed under the **GNU General Public License v3.0**. See [LICENSE](LICENSE).