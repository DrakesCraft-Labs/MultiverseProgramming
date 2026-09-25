# Recetas

Todos los objetos custom se consiguen crafteando en supervivencia — sin comandos para los jugadores normales.

## Floppy Disk

Forma (exactamente como en la mesa de crafteo):

```text
+---+---+---+
|   | P |   |
+---+---+---+
| P | I | P |
+---+---+---+
|   | P |   |
+---+---+---+
```

**Leyenda:** `P` = Papel · `I` = Lingote de hierro  
**Resultado:** 1 **Floppy Disk**

## Computadora (atril)

La computadora es un atril (lectern). Forma (receta vanilla del atril):

```text
+---+---+---+
| S | S | S |
+---+---+---+
|   | B |   |
+---+---+---+
|   | S |   |
+---+---+---+
```

**Leyenda:** `S` = Losa de madera (cualquier madera) · `B` = Librería  
**Resultado:** 1 **Computadora** (atril)

## Computadora avanzada (mesa de encantamientos)

La computadora avanzada es una **mesa de encantamientos** y depende de la computadora normal:

```text
+---+---+---+
|   | C |   |
+---+---+---+
| D | O | D |
+---+---+---+
| O | O | O |
+---+---+---+
```

**Leyenda:** `C` = Computadora (atril) · `D` = Diamante · `O` = Obsidiana  
**Resultado:** 1 **Computadora avanzada** (mesa de encantamientos)

## Display Monitor (Pantalla Holográfica)

```text
+---+---+---+
| G | G | G |
+---+---+---+
| G | L | G |
+---+---+---+
| R | R | R |
+---+---+---+
```

**Leyenda:** `G` = Cristal · `L` = Piedra luminosa (Glowstone) · `R` = Redstone  
**Resultado:** 1 **Display Monitor**

## Auto-Crafter (Ensamblador 1.21)

```text
+---+---+---+
| I | I | I |
+---+---+---+
| I | C | I |
+---+---+---+
| R | D | R |
+---+---+---+
```

**Leyenda:** `I` = Lingote de hierro · `C` = Mesa de crafteo · `R` = Redstone · `D` = Soltador (Dropper)  
**Resultado:** 1 **Auto-Crafter**

## Inventory Transposer (Clasificador / Transpositor)

```text
+---+---+---+
| I |   | I |
+---+---+---+
| I | C | I |
+---+---+---+
|   | R |   |
+---+---+---+
```

**Leyenda:** `I` = Lingote de hierro · `C` = Cofre · `R` = Redstone  
**Resultado:** 1 **Inventory Transposer**

## Sound Synthesizer (Sintetizador de Sonidos)

```text
+---+---+---+
| P | P | P |
+---+---+---+
| P | N | P |
+---+---+---+
| P | R | P |
+---+---+---+
```

**Leyenda:** `P` = Tablones de madera · `N` = Bloque musical (Note Block) · `R` = Redstone  
**Resultado:** 1 **Sound Synthesizer**

## Programmable Turtle (Tortuga Robótica & Constructora)

```text
+---+---+---+
| I | C | I |
+---+---+---+
| I | P | I |
+---+---+---+
| I | R | I |
+---+---+---+
```

**Leyenda:** `I` = Lingote de hierro · `C` = Cofre · `P` = Computadora (atril) · `R` = Redstone  
**Resultado:** 1 **Programmable Turtle** (Dispenser robótico)

## Block & Entity Scanner (Escáner de Entidades y Bloques)

```text
+---+---+---+
| C | C | C |
+---+---+---+
|   | Q |   |
+---+---+---+
|   | R |   |
+---+---+---+
```

**Leyenda:** `C` = Adoquín (Cobblestone) · `Q` = Cuarzo · `R` = Redstone  
**Resultado:** 1 **Block & Entity Scanner** (Observador)

## Cartographer & Map Renderer (Cartógrafo y Proyector)

```text
+---+---+---+
| P | P |   |
+---+---+---+
| W | W |   |
+---+---+---+
| W | W |   |
+---+---+---+
```

**Leyenda:** `P` = Papel · `W` = Tablones de roble (Oak Planks)  
**Resultado:** 1 **Cartographer & Map Renderer** (Mesa de cartografía)

## Potion & Alchemical Synthesizer (Sintetizador Alquímico)

```text
+---+---+---+
|   | B |   |
+---+---+---+
| C | C | C |
+---+---+---+
|   | R |   |
+---+---+---+
```

**Leyenda:** `B` = Vara de blaze · `C` = Adoquín (Cobblestone) · `R` = Redstone  
**Resultado:** 1 **Potion & Alchemical Synthesizer** (Soporte para pociones)

## Farming / Harvesting Module (Módulo Agrícola)

```text
+---+---+---+
| W |   | W |
+---+---+---+
| W |   | W |
+---+---+---+
| W | W | W |
+---+---+---+
```

**Leyenda:** `W` = Losa de roble (Oak Slab)  
**Resultado:** 1 **Farming / Harvesting Module** (Compostador)

## Autonomous Quarry Excavator & Turtle Quarry Engine (Cantera / Mejora de Tortuga)

> [!NOTE]
> Este objeto cumple **dos propósitos**:
> 1. Colocado junto a una Computadora, funciona como **Cantera Excavadora Estacionaria** controlada mediante el módulo Lua `quarry`.
> 2. Colocado de forma **lateral** (a la izquierda o derecha) de una Tortuga Programable, opera como la **Mejora de Motor de Cantera (Quarry Engine)** para minería volumétrica móvil mediante `turtle.quarry(...)`.

```text
+---+---+---+
| I | I | I |
+---+---+---+
| I | F | I |
+---+---+---+
| S | S | S |
+---+---+---+
```

**Leyenda:** `I` = Lingote de hierro · `F` = Horno (Furnace) · `S` = Piedra lisa (Smooth Stone)  
**Resultado:** 1 **Quarry Excavator / Turtle Quarry Engine** (Alto horno)

## NPC Chatbot & Quest Interposer (Núcleo de Diálogo NPC)

```text
+---+---+---+
|   | G |   |
+---+---+---+
| A | C | A |
+---+---+---+
|   | R |   |
+---+---+---+
```

**Leyenda:** `G` = Lingote de oro · `A` = Fragmento de amatista · `C` = Libro · `R` = Redstone  
**Resultado:** 1 **NPC Chatbot & Quest Interposer** (Catalizador de sculk)

## Nota para administradores

Los operadores pueden conseguir los objetos directamente usando:
```text
/mvprog give <floppydisk|computer|advancedcomputer|monitor|crafter|transposer|speaker|turtle|scanner|cartographer|alchemist|farmer|quarry|npc>
```
También soporta alias en español como `disco`, `computadora`, `pantalla`, `ensamblador`, `transpositor`, `parlante`, `tortuga`, `escaner`, `cartografo`, `alquimista`, `granjero`, `cantera`, `chatbot`.  
Requiere el permiso `multiverseprogramming.admin` (por defecto `op`).