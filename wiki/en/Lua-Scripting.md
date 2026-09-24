# Lua Scripting

The plugin embeds **Luaj 3.0** (pure Java Lua runtime), fully isolated and sandboxed for Paper/Purpur 1.21.11.

## Available Libraries

Programs have access to safe standard Lua libraries:

- `string` — `string.format`, `string.sub`, …
- `table` — `table.insert`, `table.concat`, …
- `math` — `math.random`, `math.floor`, …
- `coroutine`
- `bit32`
- `sleep(seconds_or_ms)` — cooperative non-blocking sleep (e.g. `sleep(0.5)` for 500 ms, or `sleep(100)` for 100 ms).
- `peripheral` — discovery and control of connected hardware peripherals.

The global `print(...)` function sends output directly to the player's chat.

## Performance Optimization & Large Codebases

The execution engine has been heavily optimized to allow extensive programs without degrading server TPS:

1. **Bounded Thread Pool:** Scripts run inside a managed worker pool (`MultiverseLua-Worker`), preventing thread exhaustion.
2. **Cooperative Time-Slicing:** Long-running math or simulation loops on advanced computers yield CPU cycles cooperatively when continuous bytecode instructions exceed the slice quota.
3. **Cooperative `sleep`:** Calling `sleep()` pauses the Lua thread cleanly and resets execution quotas.

---

## Hardware Peripherals & Machines

Placing any of these machine blocks **adjacent** (up, down, north, south, east, or west) to a Computer exposes their Lua APIs automatically.

### 1. Display Monitor (`monitor`)

Projects floating multiline text in the world using Minecraft's native `TextDisplay` entities.

```lua
monitor.clear()
monitor.write("&a=== SYSTEM STATUS ===")
monitor.write("&eCPU: &aNormal")
monitor.write("&bMemory: &f100%")

monitor.setLine(2, "&eCPU: &cALERT")
local text = monitor.getText()
```

### 2. Auto-Crafter (`crafter`)

Interfaces with the Minecraft 1.21 **Crafter** block for automated recipe manufacturing.

```lua
local items = crafter.getItems()
for slot, item in pairs(items) do
  print("Slot " .. slot .. ": " .. item.name .. " x" .. item.count)
end

crafter.setSlotDisabled(5, true)

local success = crafter.craft()
if success then
  print("&aItem crafted and dispensed!")
end
```

### 3. Inventory Transposer (`transposer`)

High-speed inspection, sorting, and routing of items between adjacent containers (chests, barrels, hoppers, shulkers).

```lua
local directions = transposer.getDirections()
local size = transposer.getSlotCount("north")

local item = transposer.getItem("north", 1)
if item then
  print("Found: " .. item.name .. " x" .. item.count)
end

-- Transfer 16 items from slot 1 in 'north' to 'south'
local moved = transposer.transferItem("north", "south", 1, 16)
print("Moved: " .. moved)
```

### 4. Sound Synthesizer (`speaker`)

Plays musical notes across 2 octaves, raw audio frequencies in Hz, and Minecraft sound effects.

```lua
speaker.playNote("harp", 12)
sleep(0.2)
speaker.playNote("bell", 16)

-- Play frequency in Hz (A4 = 440 Hz)
speaker.playTone(440.0)

-- Play vanilla sound effects
speaker.playSound("entity.player.levelup", 1.0, 1.2)
```

### Generic `peripheral` API

For multi-peripheral configurations:

```lua
local sides = peripheral.getNames() -- {"north", "up"}
local pType = peripheral.getType("up") -- "monitor"

local monitor = peripheral.wrap("up")
monitor.setText("Hello from top monitor")

local crafter = peripheral.find("crafter")
if crafter then
  crafter.craft()
end
```

---

## 5. Programmable Turtle (`turtle`)

The **Programmable Turtle** is a robotic mobile computer & constructor capable of navigating the world, mining blocks, placing blocks, managing a 16-slot inventory, and building entire structures from `.litematic` and `.nbt` blueprint files.

### Movement & Rotation

```lua
turtle.forward()    -- Moves 1 block forward in the facing direction
turtle.back()       -- Moves 1 block backward
turtle.up()         -- Moves 1 block up
turtle.down()       -- Moves 1 block down
turtle.turnLeft()   -- Turns 90 degrees to the left
turtle.turnRight()  -- Turns 90 degrees to the right
```

### Mining & Placing Blocks

```lua
turtle.dig()        -- Mines the block in front (adds to turtle inventory)
turtle.digUp()      -- Mines the block above
turtle.digDown()    -- Mines the block below

turtle.place()      -- Places block from selected slot in front
turtle.placeUp()    -- Places block above
turtle.placeDown()  -- Places block below
```

### Inventory & Fuel

```lua
turtle.select(1)           -- Selects slot 1 to 16
local slot = turtle.getSelectedSlot()
local count = turtle.getItemCount(1)
local item = turtle.getItemDetail(1) -- {name = "stone", count = 64}

local fuel = turtle.getFuelLevel()
turtle.refuel(10)          -- Consumes coal or blaze rods to refuel
```

### Blueprint Construction (`.litematic` & `.nbt`)

```lua
-- Load blueprint metadata
local bp = turtle.loadBlueprint("BP-A1B2")
print("Blueprint: " .. bp.name)
print("Dimensions: " .. bp.sizeX .. "x" .. bp.sizeY .. "x" .. bp.sizeZ)
print("Total Blocks: " .. bp.totalBlocks)

-- Start construction at specified target coordinates
turtle.buildBlueprint("BP-A1B2", 100, 64, 200)

-- Monitor progress
local prog = turtle.getBuildProgress()
print("Progress: " .. prog.percentage .. "% (" .. prog.current .. "/" .. prog.total .. ")")

-- Execution control
turtle.pauseBuild()
turtle.resumeBuild()
turtle.cancelBuild()
```

---

## 6. Web Portal & Blueprint Dashboard

Players can access the built-in **Web Portal** by executing in-game:
```text
/pc web
```

### Web Portal Features:
1. **File Upload Dropzone:** Drag and drop `.litematic` (Litematica) or `.nbt` (Vanilla Structure) files directly from your browser.
2. **Layer-by-Layer Visualizer:** Interactive 2D/3D slice visualizer with a Y-layer slider and block colors.
3. **Materials Required:** Instant breakdown of needed blocks and item counts.
4. **Remote Dispatch to Turtles:** Select any online Turtle, enter coordinates, and click **"⚡ INITIATE CONSTRUCTION"**.
5. **Live Job Monitoring:** Real-time progress bar with pause/cancel controls.