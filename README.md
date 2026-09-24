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

### 🐢 Programmable Turtle
- **Mobile Robotic Agent**: Moves forward/back/up/down, turns left/right, inspects, digs, places blocks, sucks and drops items.
- **Dedicated GUI & Inventory**: 16 turtle inventory slots, floppy disk drive, fuel status monitor, and control buttons.
- **Anti-Lag Blueprint Builder**: Autonomous building engine that places `.litematic` and `.nbt` structures block-by-block with material checks and tick pacing without causing server lag spikes.

### 🌐 Blueprint Nexus Web Portal
- **Online Visualizer & Dispatcher**: Hosted right here on GitHub Pages at [`https://drakescraft-labs.github.io/MultiverseProgramming/`](https://drakescraft-labs.github.io/MultiverseProgramming/) and also served internally by your server on port `8080`.
- **Drag & Drop Upload**: Upload `.litematic` (Schematica / Litematica bit-unpacked) and vanilla structure `.nbt` files directly from your browser.
- **Layer Slicer & Material Calculator**: Inspect every Y-level cross section in 2D/3D and preview the exact list and count of required Minecraft blocks.
- **Remote Construction Dispatch**: Target any active turtle in your world and dispatch autonomous construction at specified X, Y, Z coordinates.

### 🖥️ Computers & Advanced Computers
- **Standard Computer Block** (default: lectern) — right‑click to open GUI, insert floppy disk, validate and run Lua code with chat output.
- **Advanced Computer** (default: enchanting table) — runs persistent programs and maintains state.
- **Sandboxed Lua (Luaj)** — safe sandboxing with restricted standard library.
- **High-Performance Anti-Lag Engine**: Configurable instruction count caps (`max-instructions-per-run`), execution timeouts, and throttled execution loops to safely run massive programs without freezing or crashing the server.

### 🔌 Advanced Peripherals
- **Display Monitor** (`monitor`): Render multiline text, ASCII banners, and RGB colors on adjacent sign monitors.
- **Auto-Crafter** (`crafter`): Programmatic 3x3 recipe crafting, ingredient validation, and recipe querying.
- **Inventory Transposer** (`transposer`): Automated item routing, precise stack transfer, and container sorting between adjacent chests.
- **Sound Synthesizer** (`synth`): Play custom note block melodies, instruments, tones, and octaves directly via Lua.

---

## Quick Start

1. Drop `MultiverseProgramming-1.0.3.jar` into your server's `plugins/` folder and restart.
2. Craft a **Floppy Disk** (see [Recipes](wiki/en/Recipes.md)) — fresh disks come preloaded with template code.
3. Place a **Turtle** or **Computer** and right‑click to open its interface.
4. Insert your disk into the drive slot and click **"✔ Validate Code"**.
5. Connect your browser to the [Blueprint Nexus Web Portal](https://drakescraft-labs.github.io/MultiverseProgramming/) to upload designs and dispatch construction tasks!

### Example Lua Program (Turtle)

```lua
-- Refuel from slot 1 and dig a 3x3 room
turtle.select(1)
turtle.refuel(10)
print("Fuel level: " .. turtle.getFuelLevel())

for x = 1, 3 do
  for y = 1, 3 do
    turtle.dig()
    turtle.forward()
  end
  turtle.turnRight()
end
```

---

## Building from Source

```powershell
mvn -q clean package
```

The compiled jar with all bundled dependencies (Luaj, shaded) will be in `target/MultiverseProgramming-1.0.3.jar`.

---

## Documentation

- **English Wiki**: [wiki/en/Home.md](wiki/en/Home.md)
- **Spanish Wiki**: [wiki/es/Home.md](wiki/es/Home.md)
- Recipes: [wiki/en/Recipes.md](wiki/en/Recipes.md) · [wiki/es/Recipes.md](wiki/es/Recipes.md)
- Configuration: [wiki/en/Configuration.md](wiki/en/Configuration.md) · [wiki/es/Configuration.md](wiki/es/Configuration.md)
- Lua API Reference: [wiki/en/Lua-Scripting.md](wiki/en/Lua-Scripting.md) · [wiki/es/Lua-Scripting.md](wiki/es/Lua-Scripting.md)

---

## License

This project is licensed under the **GNU General Public License v3.0**. See [LICENSE](LICENSE) for details.
