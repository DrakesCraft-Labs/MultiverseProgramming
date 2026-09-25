# MultiverseProgramming

Programmable computers, autonomous turtles, and automation peripherals with **Lua** inside your Minecraft server, inspired by ComputerCraft.

Place a computer or turtle in the world, insert a **floppy disk**, and validate & run your Lua code with one click — or control constructions remotely via the **Blueprint Nexus Web Portal**.

Built for **Purpur / Paper 1.21.11** with Java 21.

[![Live Web Dashboard](https://img.shields.io/badge/Blueprint%20Nexus-Web%20Portal-38bdf8?style=for-the-badge&logo=googlechrome&logoColor=white)](https://drakescraft-labs.github.io/MultiverseProgramming/)
[![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-GPL%203.0-blue?style=for-the-badge)](LICENSE)

> 📖 **Wiki**: [English](wiki/en/Home.md) · [Español](wiki/es/Home.md)  
> 🌐 **Web Portal**: [Launch Blueprint Nexus](https://drakescraft-labs.github.io/MultiverseProgramming/)

---

## Features

### 🐢 Programmable Turtle & Autonomous Builder
- **Mobile Robotic Agent**: Moves forward/back/up/down, turns left/right, inspects, digs, places blocks, sucks and drops items.
- **Anti-Lag Blueprint Builder**: Autonomous building engine that places `.litematic` and `.nbt` structures block-by-block with tick pacing without causing server lag spikes.
- **3D Rotation & Orientation**: Build structures in any orientation (`NORTH`, `EAST`, `SOUTH`, `WEST` or `0`, `90`, `180`, `270` degrees) with automatic blockstate and coordinate rotation.
- **Supply & Fuel Chests with Holograms**: When materials or fuel are required, the turtle spawns adjacent chests with floating English holograms (`"Place construction blocks here"` / `"Place fuel here"`), pulling items automatically.
- **Claim Protection Integration**: Full support for **WorldGuard** and **ProtectionStones** to prevent unauthorized construction in protected plots. Configurable owner-only policy (`protection-stones-require-owner`).
- **Container & Dupe Protection**: Strict checks prevent storing nested containers (shulker boxes in chests) and writable books inside constructed containers.

### 🌐 Blueprint Nexus Web Portal
- **Online Visualizer & Dispatcher**: Hosted at [`https://drakescraft-labs.github.io/MultiverseProgramming/`](https://drakescraft-labs.github.io/MultiverseProgramming/) and internally by your server on port `8080`.
- **Drag & Drop Upload**: Upload `.litematic` and vanilla structure `.nbt` files directly from your browser.
- **Storage Quotas & Pastebin Cloud**: Global/personal storage quota limits per player with admin bypass (`/mvprog getbypass`).
- **Remote Construction Dispatch**: Target any active turtle in your world and dispatch autonomous construction at specified X, Y, Z coordinates and orientation.

### 🖥️ Computers & Advanced Computers
- **Standard Computer Block** (default: lectern) — right-click GUI, insert floppy disk, validate and run Lua code with chat output.
- **Advanced Computer** (default: enchanting table) — persistent state, long-running loops, and a **dynamic Stop Button**:
  - Displays a green emerald (`▶ Run Program`) when idle.
  - Transforms into a glowing redstone block (`⏹ Stop Program`) while running.
  - Allows stopping execution at any time, even after removing the disk.
- **Machine Crafting & Functionality Toggles**: Independent toggles in `config.yml` to enable/disable crafting recipes and runtime functionality for each machine block.

### 🔌 Advanced Peripherals
- **Display Monitor** (`monitor`): Render multiline text, ASCII banners, and RGB colors on adjacent sign monitors.
- **Auto-Crafter** (`crafter`): Programmatic 3x3 recipe crafting, ingredient validation, and recipe querying.
- **Inventory Transposer** (`transposer`): Automated item routing, precise stack transfer, and container sorting between adjacent chests.
- **Sound Synthesizer** (`speaker`): Play custom note block melodies, instruments, tones, and octaves directly via Lua.

---

## Quick Start

1. Drop `MultiverseProgramming-1.0.8.jar` into your server's `plugins/` folder and start the server.
2. Craft a **Floppy Disk** (see [Recipes](wiki/en/Recipes.md)) — fresh disks come preloaded with template code.
3. Place a **Turtle** or **Computer** and right-click to open its interface.
4. Insert your disk into the drive slot and click **"✔ Validate Code"** or **"▶ Run Program"**.
5. Connect your browser to the [Blueprint Nexus Web Portal](https://drakescraft-labs.github.io/MultiverseProgramming/) to upload designs and dispatch construction tasks!

### Example Lua Construction Program

```lua
-- Start construction with 90-degree clockwise rotation (facing EAST)
local ok, err = turtle.build("BP-CASTLE", 100, 64, 200, false, "EAST")
if not ok then
  print("Error: " .. err)
end
```

---

## Commands

All commands use the `/mvprog` prefix (aliases: `/pc`, `/computador`, `/computadora`, `/disco`):

```text
/mvprog help                                                     - Shows help menu
/mvprog web                                                      - Shows Web Dashboard link
/mvprog get <code|url>                                           - Downloads blueprint from cloud nexus
/mvprog quota [player]                                           - Views personal or target player storage quota
/mvprog bp [list|quota|delete]                                   - Manages personal blueprints
/mvprog build <bp> <x> <y> <z> [turtle] [clear] [orientation]   - Orders turtle to construct (Admin)
/mvprog getbypass <code|url>                                     - Downloads blueprint bypassing quota (Admin)
/mvprog bp clean [days]                                          - Purges unpinned blueprints (Admin)
/mvprog give <item>                                              - Gives custom item (Admin)
/mvprog reload                                                   - Reloads configuration and recipes (Admin)
```

---

## Building from Source

```powershell
mvn -q clean package
```

The compiled jar with all bundled dependencies (Luaj, shaded) will be in `target/MultiverseProgramming-1.0.8.jar`.

---

## License

This project is licensed under the **GNU General Public License v3.0**. See [LICENSE](LICENSE) for details.
