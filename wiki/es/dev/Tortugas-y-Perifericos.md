# Tortugas, Motor de Cantera y Ecosistema de Periféricos (Español) 🐢⚙️

Este documento describe el diseño interno de la **Tortuga Programable**, el módulo de **Mejora Lateral de Cantera (Quarry Engine)**, la **Arquitectura de Periféricos** y los puentes de auditoría y protección de reclamos.

---

## 1. Arquitectura de la Tortuga y Máquina de Estados (`Turtle.java`)

Una Tortuga es un bloque móvil autónomo en el mundo capaz de desplazarse, colocar y romper bloques y construir estructuras volumétricas completas.

```
       ┌────────────────────────────────────────────────────────┐
       │                       Status.IDLE                      │
       └──────────────┬──────────────────────────┬──────────────┘
                      │ startBuild()             │ startQuarry()
                      ▼                          ▼
       ┌────────────────────────┐      ┌────────────────────────┐
       │     Status.BUILDING    │      │      Status.MINING     │
       └──────────┬─────────────┘      └──────────┬─────────────┘
                  │ pauseBuild()                  │ pauseQuarry() / Cofre Lleno
                  ▼                               ▼
       ┌────────────────────────────────────────────────────────┐
       │                      Status.PAUSED                     │
       └────────────────────────────────────────────────────────┘
```

### Componentes Fundamentales:
- **Seguimiento Espacial**: Registrado en [`TurtleManager`](file:///c:/Users/danie/OneDrive/Documentos/Github/Personal/MultiverseProgramming/src/main/java/com/multiverse/programming/turtle/TurtleManager.java) mediante dos tablas `ConcurrentHashMap`: `Ubicación -> Tortuga` e `ID -> Tortuga`.
- **Orientación Direccional**: Normalizada a ejes horizontales (`NORTH`, `EAST`, `SOUTH`, `WEST`).
- **Sistema de Inventario**: 16 ranuras accesibles tanto desde la GUI in-game como desde el entorno Lua (`turtle.select(slot)`, `turtle.getItemDetail()`).
- **Hologramas de Texto**: Utiliza entidades nativas `TextDisplay` de Paper 1.21 con respaldo automático a `ArmorStand` invisibles si el mundo no soporta displays.

---

## 2. Módulo de Mejora Lateral de Cantera (Quarry Engine)

El Quarry Engine convierte a la tortuga en un excavador volumétrico autónomo. Ilustra el patrón de mejora de bloques adjuntos:

### A. Validación de Acoplamiento Lateral
Antes de iniciar la excavación, la tortuga verifica que el bloque del motor (`BLAST_FURNACE` por defecto) esté físicamente a su lado **izquierdo** o **derecho** respecto a su dirección actual:
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
Si el bloque no está presente, rechaza la operación con `"No Quarry Engine attached to the lateral side of the turtle"`.

### B. Despliegue de Cofres Duales con Hologramas
Al iniciar la excavación, la tortuga coloca automáticamente:
1. **Cofre de Almacenamiento de Bloques**: Ubicado directamente detrás de la tortuga, con el holograma flotante `"§e📦 Mined Blocks Storage"`.
2. **Cofre de Combustible**: Ubicado contiguo al de almacenamiento, con el holograma `"§6⚡ Place fuel here"`.

### C. Detección de Cofre Lleno y Pausa Automática
Para evitar destruir bloques del mundo cuando el almacenamiento está agotado, [`isQuarryStorageFull()`](file:///c:/Users/danie/OneDrive/Documentos/Github/Personal/MultiverseProgramming/src/main/java/com/multiverse/programming/turtle/Turtle.java) inspecciona el cofre:
1. Si todas las ranuras están ocupadas y no se pueden apilar ítems, la tortuga se pausa de inmediato (`"Paused: Mined blocks storage chest is full"`).
2. El bloque objetivo **no** se rompe hasta que haya espacio disponible.
3. Al vaciar el cofre y reanudar con `turtle.resumeQuarry()` o desde la GUI in-game, la tortuga continúa limpiamente.

### D. Reubicación Física en Sincronía (`relocateTurtleWithEngine()`)
A medida que la tortuga excava y desciende por capas, el bloque del motor viaja junto a ella:
- En movimientos lineales (`forward`, `back`, `up`, `down`), se verifica que ni la tortuga ni el motor choquen con obstáculos.
- En giros (`turnLeft`, `turnRight`), el motor pivota alrededor de la tortuga manteniendo su posición lateral relativa.
- Al finalizar, la tortuga y el motor regresan a la posición de partida.

### E. Tasa de Consumo de Combustible +20%
El motor consume un 20% más de combustible (tasa de 1.20x). Para prevenir discrepancias por redondeo en números de punto flotante (IEEE 754) a lo largo de miles de operaciones, el cálculo utiliza acumuladores de puntos enteros:
```java
quarryFuelPoints += 120; // 120 puntos por acción
int toDeduct = quarryFuelPoints / 100;
quarryFuelPoints %= 100;
this.fuel -= toDeduct;
```

---

## 3. Arquitectura de Periféricos (`PeripheralManager.java`)

Los periféricos son bloques modulares colocados junto a una Computadora o Tortuga. Implementan la interfaz [`Peripheral`](file:///c:/Users/danie/OneDrive/Documentos/Github/Personal/MultiverseProgramming/src/main/java/com/multiverse/programming/peripheral/Peripheral.java):

```java
public interface Peripheral {
    String getType();
    Location getLocation();
    LuaTable toLuaTable();
}
```

Al ejecutarse un programa, `PeripheralManager.bindAll(globals, plugin, location, isTurtle)` escanea las 6 caras adyacentes (`NORTH`, `SOUTH`, `EAST`, `WEST`, `UP`, `DOWN`) e inyecta las funciones en el entorno Lua:

| Periférico | Bloque | Variable Lua | Funciones Clave |
| :--- | :--- | :--- | :--- |
| **Monitor** | Carteles y Carteles Colgantes | `monitor` | `write(text)`, `setCursor(x,y)`, `clear()`, `renderBanner(ascii)` |
| **Crafter** | `CRAFTER` | `crafter` | `craft()`, `canCraft()`, `getRecipe()`, `inspectGrid()` |
| **Transposer** | `HOPPER` | `transposer` | `transferItem(fromDir, toDir, slot, count)`, `getInventorySize()` |
| **Speaker** | `NOTE_BLOCK` | `speaker` | `playNote(instrument, pitch)`, `playTone(freq)`, `playSound(snd)` |
| **Scanner** | `OBSERVER` | `scanner` | `scanEntities(radius)`, `scanPlayers(radius)`, `scanBlocks(radius, mat)` |
| **Cartographer** | `CARTOGRAPHY_TABLE` | `cartographer` | `getBiome()`, `createMap(scale)`, `renderToMonitor(side, scale)` |
| **Alchemist** | `BREWING_STAND` | `alchemist` | `brew(type, [modifier], [splash])`, `getRecipes()`, `inspectStand()` |
| **Farmer** | `COMPOSTER` | `farmer` | `inspectCrop(dir)`, `harvest(dir)`, `replant(dir)`, `fertilize(dir)` |
| **NPC** | `SCULK_CATALYST` | `npc` | `say(msg)`, `ask(jugador, pregunta, opciones)`, `setName(name)`, `getLastResponse()` |

> [!NOTE]
> El **Motor de Cantera** (`BLAST_FURNACE`) **no** es un periférico de computadora. Como se detalla en la [Sección 2](#2-módulo-de-mejora-lateral-de-cantera-quarry-engine), opera de forma exclusiva como una mejora de acoplamiento lateral para la Tortuga Programable controlada a través de `turtle.quarry(...)`.

---

## 4. Integraciones de Auditoría y Protección de Reclamos

### Protección de Terrenos (`ProtectionManager.java`)
Antes de que una Tortuga coloque o rompa un bloque en el mundo, `ProtectionManager.checkBuildArea()` consulta a los plugins de protección activos:
- **WorldGuard**: Valida la bandera `BUILD` y membresía de región mediante `WorldGuardPlugin.inst().createProtectionQuery()`.
- **GriefPrevention**: Verifica la confianza del reclamo con `GriefPrevention.instance.dataStore.getClaimAt()`.
- **Towny**: Verifica permisos de construcción en parcelas municipales.
- **Lands** y **Residence**: Verifica banderas del reclamo para el UUID del propietario de la tortuga.

Si el terreno está protegido, la Tortuga se detiene inmediatamente con un mensaje de error y no modifica el mundo.

### Auditoría con CoreProtect (`CoreProtectBridge.java`)
Todas las acciones de colocación y eliminación de bloques realizadas por Tortugas, Computadoras y Canteras se registran en CoreProtect:
```java
CoreProtectBridge.logRemoval(owner, turtleId, location, oldMaterial, oldBlockData);
CoreProtectBridge.logPlacement(owner, turtleId, location, newMaterial, newBlockData);
```
Los administradores pueden utilizar `/co inspect` o `/co lookup` para ver exactamente qué ID de Tortuga y qué jugador propietario colocó o destruyó cualquier bloque.
