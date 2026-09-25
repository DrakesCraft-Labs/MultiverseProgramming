# Crafting Recipes

All custom items can be obtained via vanilla crafting in survival mode.

## Floppy Disk

```text
+---+---+---+
|   | P |   |
+---+---+---+
| P | I | P |
+---+---+---+
|   | P |   |
+---+---+---+
```

**Legend:** `P` = Paper · `I` = Iron Ingot  
**Result:** 1 **Floppy Disk**

## Computer (Lectern)

```text
+---+---+---+
| S | S | S |
+---+---+---+
|   | B |   |
+---+---+---+
|   | S |   |
+---+---+---+
```

**Legend:** `S` = Wooden Slab (any wood) · `B` = Bookshelf  
**Result:** 1 **Computer** (Lectern)

## Advanced Computer (Enchanting Table)

```text
+---+---+---+
|   | C |   |
+---+---+---+
| D | O | D |
+---+---+---+
| O | O | O |
+---+---+---+
```

**Legend:** `C` = Computer (Lectern) · `D` = Diamond · `O` = Obsidian  
**Result:** 1 **Advanced Computer** (Enchanting Table)

## Display Monitor (Holographic Screen)

```text
+---+---+---+
| G | G | G |
+---+---+---+
| G | L | G |
+---+---+---+
| R | R | R |
+---+---+---+
```

**Legend:** `G` = Glass · `L` = Glowstone · `R` = Redstone Dust  
**Result:** 1 **Display Monitor**

## Auto-Crafter (1.21 Assembler)

```text
+---+---+---+
| I | I | I |
+---+---+---+
| I | C | I |
+---+---+---+
| R | D | R |
+---+---+---+
```

**Legend:** `I` = Iron Ingot · `C` = Crafting Table · `R` = Redstone Dust · `D` = Dropper  
**Result:** 1 **Auto-Crafter**

## Inventory Transposer (Container Router)

```text
+---+---+---+
| I |   | I |
+---+---+---+
| I | C | I |
+---+---+---+
|   | R |   |
+---+---+---+
```

**Legend:** `I` = Iron Ingot · `C` = Chest · `R` = Redstone Dust  
**Result:** 1 **Inventory Transposer**

## Sound Synthesizer (Audio Speaker)

```text
+---+---+---+
| P | P | P |
+---+---+---+
| P | N | P |
+---+---+---+
| P | R | P |
+---+---+---+
```

**Legend:** `P` = Wooden Planks · `N` = Note Block · `R` = Redstone Dust  
**Result:** 1 **Sound Synthesizer**

## Programmable Turtle (Robotic Mobile Computer & Constructor)

```text
+---+---+---+
| I | C | I |
+---+---+---+
| I | P | I |
+---+---+---+
| I | R | I |
+---+---+---+
```

**Legend:** `I` = Iron Ingot · `C` = Chest · `P` = Computer (Lectern) · `R` = Redstone Dust  
**Result:** 1 **Programmable Turtle** (Robotic Dispenser)

## Block & Entity Scanner (Observer)

```text
+---+---+---+
| C | C | C |
+---+---+---+
|   | Q |   |
+---+---+---+
|   | R |   |
+---+---+---+
```

**Legend:** `C` = Cobblestone · `Q` = Quartz · `R` = Redstone Dust  
**Result:** 1 **Block & Entity Scanner** (Observer)

## Cartographer & Map Renderer (Cartography Table)

```text
+---+---+---+
| P | P |   |
+---+---+---+
| W | W |   |
+---+---+---+
| W | W |   |
+---+---+---+
```

**Legend:** `P` = Paper · `W` = Oak Planks  
**Result:** 1 **Cartographer & Map Renderer** (Cartography Table)

## Potion & Alchemical Synthesizer (Brewing Stand)

```text
+---+---+---+
|   | B |   |
+---+---+---+
| C | C | C |
+---+---+---+
|   | R |   |
+---+---+---+
```

**Legend:** `B` = Blaze Rod · `C` = Cobblestone · `R` = Redstone Dust  
**Result:** 1 **Potion & Alchemical Synthesizer** (Brewing Stand)

## Farming / Harvesting Module (Composter)

```text
+---+---+---+
| W |   | W |
+---+---+---+
| W |   | W |
+---+---+---+
| W | W | W |
+---+---+---+
```

**Legend:** `W` = Oak Slab  
**Result:** 1 **Farming / Harvesting Module** (Composter)

## Autonomous Quarry Excavator & Turtle Quarry Engine (Blast Furnace)

> [!NOTE]
> This item serves **dual purposes**:
> 1. Placed next to a Computer, it operates as an **Autonomous Stationary Quarry Excavator** controlled via the `quarry` Lua API.
> 2. Placed **laterally** (directly to the left or right) next to a Programmable Turtle, it functions as the **Quarry Engine Upgrade Attachment** for mobile volumetric strip-mining controlled via `turtle.quarry(...)`.

```text
+---+---+---+
| I | I | I |
+---+---+---+
| I | F | I |
+---+---+---+
| S | S | S |
+---+---+---+
```

**Legend:** `I` = Iron Ingot · `F` = Furnace · `S` = Smooth Stone  
**Result:** 1 **Quarry Excavator / Turtle Quarry Engine** (Blast Furnace)

## NPC Chatbot & Quest Interposer (Sculk Catalyst)

```text
+---+---+---+
|   | G |   |
+---+---+---+
| A | C | A |
+---+---+---+
|   | R |   |
+---+---+---+
```

**Legend:** `G` = Gold Ingot · `A` = Amethyst Shard · `C` = Book · `R` = Redstone Dust  
**Result:** 1 **NPC Chatbot & Quest Interposer** (Sculk Catalyst)

## Note for Administrators

Operators can dispense items directly using:
```text
/mvprog give <floppydisk|computer|advancedcomputer|monitor|crafter|transposer|speaker|turtle|scanner|cartographer|alchemist|farmer|quarry|npc>
```
Requires permission `multiverseprogramming.admin` (default: `op`).