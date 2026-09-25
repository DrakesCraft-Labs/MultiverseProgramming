# Turtles, Quarry Engine & Peripheral Ecosystem (English) 🐢⚙️

This document covers the internal design of the **Programmable Turtle**, the **Quarry Engine lateral attachment module**, the **Peripherals Architecture**, and the **Third-Party Protection & Audit Bridges**.

---

## 1. Turtle Architecture & State Machine (`Turtle.java`)

A Turtle is an autonomous in-game mobile block capable of navigation, block manipulation, and volumetric structure building.

```
       ┌────────────────────────────────────────────────────────┐
       │                       Status.IDLE                      │
       └──────────────┬──────────────────────────┬──────────────┘
                      │ startBuild()             │ startQuarry()
                      ▼                          ▼
       ┌────────────────────────┐      ┌────────────────────────┐
       │     Status.BUILDING    │      │      Status.MINING     │
       └──────────┬─────────────┘      └──────────┬─────────────┘
                  │ pauseBuild()                  │ pauseQuarry() / Storage Full
                  ▼                               ▼
       ┌────────────────────────────────────────────────────────┐
       │                      Status.PAUSED                     │
       └────────────────────────────────────────────────────────┘
```

### Key Components:
- **Spatial Tracking**: Registered in [`TurtleManager`](file:///c:/Users/danie/OneDrive/Documentos/Github/Personal/MultiverseProgramming/src/main/java/com/multiverse/programming/turtle/TurtleManager.java) in dual lookup tables: `Location -> Turtle` and `id -> Turtle`.
- **Directional Facing**: Sanitized to horizontal axes (`NORTH`, `EAST`, `SOUTH`, `WEST`).
- **Inventory System**: 16 dedicated slots exposed to both the GUI and the Lua sandbox (`turtle.select(slot)`, `turtle.getItemDetail()`).
- **Hologram Displays**: Uses Paper 1.21 `TextDisplay` entities with automatic fallback to invisible `ArmorStand` markers for legacy worlds.

---

## 2. Quarry Engine Lateral Upgrade Module

The Quarry Engine turns the turtle into an autonomous, heavy-duty excavator. It demonstrates the engine upgrade pattern:

### A. Lateral Attachment Validation
Before excavation begins, the turtle validates that a Quarry Engine block (`BLAST_FURNACE` by default) is physically adjacent to its **left** or **right** relative to its horizontal facing:
```java
public LateralSide findLateralQuarrySide() {
    Material qMat = getQuarryBlockMaterial();
    Block b = location.getBlock();
    if (b == null) return null;
    
    BlockFace left = getLeftFace(this.facing);
    if (b.getRelative(left).getType() == qMat) return LateralSide.LEFT;
    
    BlockFace right = getRightFace(this.facing);
    if (b.getRelative(right).getType() == qMat) return LateralSide.RIGHT;
    
    return null;
}
```
If missing, excavation is rejected with `"No Quarry Engine attached to the lateral side of the turtle"`.

### B. Dual Chest Deployment & Holograms
At launch, the turtle automatically deploys:
1. **Mined Blocks Storage Chest**: Located directly behind the turtle, tagged with floating hologram `"§e📦 Mined Blocks Storage"`.
2. **Fuel Chest**: Located adjacent to the storage chest, tagged with `"§6⚡ Place fuel here"`.

### C. Full Storage Auto-Pause
To prevent destroying player blocks when storage is exhausted, [`isQuarryStorageFull()`](file:///c:/Users/danie/OneDrive/Documentos/Github/Personal/MultiverseProgramming/src/main/java/com/multiverse/programming/turtle/Turtle.java) checks the chest:
1. If all chest slots are occupied and no items can stack, the turtle pauses immediately (`"Paused: Mined blocks storage chest is full"`).
2. The target block is **not** broken until space is freed.
3. Resuming via `turtle.resumeQuarry()` or the in-game GUI flushes any buffer and continues cleanly.

### D. Physical Lockstep Relocation (`relocateTurtleWithEngine()`)
As the turtle moves and rotates across layers, the attached engine block moves with it:
- During movements (`forward`, `back`, `up`, `down`), both target positions are checked for obstacles.
- During turns (`turnLeft`, `turnRight`), the engine pivots around the turtle to the new lateral coordinate.
- Upon completion, the turtle and engine return to the initial starting coordinates.

### E. +20% Fuel Consumption Rate
The quarry module consumes 20% more fuel (1.20x). To prevent IEEE 754 floating-point rounding errors over millions of operations, fuel is calculated using an integer point accumulator:
```java
quarryFuelPoints += 120; // 120 points per action
int toDeduct = quarryFuelPoints / 100;
quarryFuelPoints %= 100;
this.fuel -= toDeduct;
```

---

## 3. Peripheral Architecture (`PeripheralManager.java`)

Peripherals are modular hardware blocks adjacent to a Computer or Turtle. They implement the [`Peripheral`](file:///c:/Users/danie/OneDrive/Documentos/Github/Personal/MultiverseProgramming/src/main/java/com/multiverse/programming/peripheral/Peripheral.java) interface:

```java
public interface Peripheral {
    String getType();
    Location getLocation();
    LuaTable toLuaTable();
}
```

When a program runs, `PeripheralManager.bindAll(globals, plugin, location, isTurtle)` scans the 6 adjacent block faces (`NORTH`, `SOUTH`, `EAST`, `WEST`, `UP`, `DOWN`) and injects matching Lua tables into the sandbox:

| Peripheral | Block Material | Lua Variable | Core Functions |
| :--- | :--- | :--- | :--- |
| **Monitor** | Any Sign / Hanging Sign | `monitor` | `write(text)`, `setCursor(x,y)`, `clear()`, `renderBanner(ascii)` |
| **Crafter** | `CRAFTER` | `crafter` | `craft()`, `canCraft()`, `getRecipe()`, `inspectGrid()` |
| **Transposer** | `HOPPER` | `transposer` | `transferItem(fromDir, toDir, slot, count)`, `getInventorySize()` |
| **Speaker** | `NOTE_BLOCK` | `speaker` | `playNote(instrument, pitch)`, `playTone(freq)`, `playSound(snd)` |
| **Scanner** | `OBSERVER` | `scanner` | `scanEntities(radius)`, `scanPlayers(radius)`, `scanBlocks(radius, mat)` |
| **Cartographer** | `CARTOGRAPHY_TABLE` | `cartographer` | `getBiome()`, `createMap(scale)`, `renderToMonitor(side, scale)` |
| **Alchemist** | `BREWING_STAND` | `alchemist` | `brew(type, [modifier], [splash])`, `getRecipes()`, `inspectStand()` |
| **Farmer** | `COMPOSTER` | `farmer` | `inspectCrop(dir)`, `harvest(dir)`, `replant(dir)`, `fertilize(dir)` |
| **Quarry** | `BLAST_FURNACE` | `quarry` | `start(w, l, targetY, liquids)`, `stop()`, `getStatus()` |
| **NPC** | `PLAYER_HEAD` / `CARVED_PUMPKIN` | `npc` | `say(msg)`, `prompt(choices, cb)`, `setName(name)`, `getHistory()` |

---

## 4. Protection & Audit Logging Integrations

### Claim Protections (`ProtectionManager.java`)
Before a Turtle places or destroys a block in the world, `ProtectionManager.checkBuildArea()` queries active region protection plugins:
- **WorldGuard**: Checks `Flags.BUILD` and region ownership via `WorldGuardPlugin.inst().createProtectionQuery()`.
- **GriefPrevention**: Checks claim trust via `GriefPrevention.instance.dataStore.getClaimAt()`.
- **Towny**: Verifies town plot build permissions.
- **Lands** & **Residence**: Verifies land flags for the turtle owner's UUID.

If the area is protected, the Turtle immediately halts with an error message and does not alter the world.

### CoreProtect Audit Bridge (`CoreProtectBridge.java`)
All block removals and block placements by Turtles, Computers, and Quarries are logged to CoreProtect:
```java
CoreProtectBridge.logRemoval(owner, turtleId, location, oldMaterial, oldBlockData);
CoreProtectBridge.logPlacement(owner, turtleId, location, newMaterial, newBlockData);
```
Server administrators using `/co inspect` or `/co lookup` can inspect the exact Turtle ID and player owner that modified any block.
