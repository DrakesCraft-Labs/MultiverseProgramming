# Programación en Lua

El plugin integra **Luaj 3.0** (Lua en Java puro) totalmente aislado y sandboxed para Paper/Purpur 1.21.11.

## Librerías disponibles

Los programas pueden usar las librerías estándar de Lua seguras:

- `string` — `string.format`, `string.sub`, …
- `table` — `table.insert`, `table.concat`, …
- `math` — `math.random`, `math.floor`, …
- `coroutine`
- `bit32`
- `sleep(ms_o_segundos)` — pausa cooperativa no bloqueante (ej. `sleep(0.5)` para 500 ms, o `sleep(100)` para 100 ms).
- `peripheral` — detección e interacción con periféricos conectados.

La función global `print(...)` se captura y se envía al chat del jugador que ejecuta.

## Optimización y Ejecución de Códigos Grandes

El motor de ejecución ha sido optimizado con salvaguardas avanzadas para permitir scripts masivos sin afectar el TPS del servidor:

1. **Pool de Hilos Acotado:** Los programas corren en un pool de hilos administrado (`MultiverseLua-Worker`), previniendo agotamiento de recursos JVM.
2. **Cooperative Time-Slicing:** Bucles extensos de cálculo en computadoras avanzadas aplican micro-pausas cooperativas si exceden la cuota de instrucciones continua, permitiendo compartir los núcleos de la CPU con los hilos principales del servidor.
3. **Pausa con `sleep`:** El uso de `sleep(tiempo)` reinicia la cuota de instrucciones y cede el hilo de forma segura.

---

## Periféricos y Máquinas Conectables

Colocar cualquiera de estas máquinas de forma **adyacente** (arriba, abajo, norte, sur, este u oeste) a una Computadora activa sus APIs directamente en Lua.

### 1. Display Monitor (`monitor`)

Proyecta texto flotante en tiempo real en el mundo utilizando entidades nativas `TextDisplay` de Minecraft.

```lua
-- Limpiar pantalla
monitor.clear()

-- Escribir texto (soporta códigos de color con & o §)
monitor.write("&a=== ESTADO DEL SISTEMA ===")
monitor.write("&eCPU: &aNormal")
monitor.write("&bMemoria: &f100%")

-- Modificar una línea específica (1-indexado)
monitor.setLine(2, "&eCPU: &cALERTA")

-- Leer contenido
local texto = monitor.getText()
```

### 2. Auto-Crafter (`crafter`)

Controla el bloque **Crafter** de Minecraft 1.21 para automatizar ensamblaje de recetas.

```lua
-- Inspeccionar objetos en la cuadrícula 3x3
local items = crafter.getItems()
for slot, item in pairs(items) do
  print("Slot " .. slot .. ": " .. item.name .. " x" .. item.count)
end

-- Deshabilitar o habilitar una ranura (1 a 9)
crafter.setSlotDisabled(5, true)

-- Ejecutar fabricación
local exito = crafter.craft()
if exito then
  print("&a¡Objeto crafteado y dispensado!")
end
```

### 3. Inventory Transposer (`transposer`)

Inspecciona y transfiere ítems a alta velocidad entre contenedores adyacentes (cofres, barriles, tolvas, etc.).

```lua
-- Ver direcciones que tienen contenedores conectados
local lados = transposer.getDirections()

-- Obtener cantidad de ranuras de un cofre al norte
local tam = transposer.getSlotCount("north")

-- Ver ítem en el slot 1
local item = transposer.getItem("north", 1)
if item then
  print("Encontrado: " .. item.name .. " x" .. item.count)
end

-- Transferir 16 ítems del slot 1 del cofre 'north' al cofre 'south'
local movidos = transposer.transferItem("north", "south", 1, 16)
print("Movidos: " .. movidos)
```

### 4. Sound Synthesizer (`speaker`)

Reproduce notas musicales con afinación exacta, frecuencias de audio en Hz y efectos de sonido de Minecraft.

```lua
-- Tocar notas (instrumento, nota de 0 a 24 semitonos)
speaker.playNote("harp", 12)
sleep(0.2)
speaker.playNote("bell", 16)

-- Tocar un tono por frecuencia en Hertz (ej. A4 = 440 Hz)
speaker.playTone(440.0)

-- Reproducir efectos de sonido de Minecraft
speaker.playSound("entity.player.levelup", 1.0, 1.2)
```

### API Genérica `peripheral`

Para configuraciones con múltiples periféricos del mismo tipo:

```lua
local nombres = peripheral.getNames() -- {"north", "up"}
local tipo = peripheral.getType("up")  -- "monitor"

local miPantalla = peripheral.wrap("up")
miPantalla.setText("Hola desde la parte superior")

local miCrafter = peripheral.find("crafter")
if miCrafter then
  miCrafter.craft()
end
```

---

## 5. Programmable Turtle (`turtle`)

La **Tortuga Programable** es un autómata robótico móvil y constructor capaz de desplazarse por el mundo, minar, colocar bloques, almacenar ítems en 16 ranuras internas y construir estructuras completas a partir de esquemas `.litematic` y `.nbt`.

### Métodos de Movimiento y Giro

```lua
turtle.forward()    -- Avanza 1 bloque en la dirección que mira
turtle.back()       -- Retrocede 1 bloque
turtle.up()         -- Sube 1 bloque
turtle.down()       -- Baja 1 bloque
turtle.turnLeft()   -- Gira 90 grados a la izquierda
turtle.turnRight()  -- Gira 90 grados a la derecha
```

### Minería y Colocación de Bloques

```lua
turtle.dig()        -- Mina el bloque al frente (guarda en su inventario)
turtle.digUp()      -- Mina el bloque superior
turtle.digDown()    -- Mina el bloque inferior

turtle.place()      -- Coloca el bloque de la ranura seleccionada al frente
turtle.placeUp()    -- Coloca hacia arriba
turtle.placeDown()  -- Coloca hacia abajo
```

### Inventario y Combustible

```lua
turtle.select(1)           -- Selecciona la ranura 1 a 16
local slot = turtle.getSelectedSlot()
local cant = turtle.getItemCount(1)
local item = turtle.getItemDetail(1) -- {name = "stone", count = 64}

local fuel = turtle.getFuelLevel()
turtle.refuel(10)          -- Consume carbón o varas de blaze para recargar combustible
```

### Construcción de Diseños (`.litematic` y `.nbt`)

```lua
-- Cargar metadatos de un diseño subido al portal web o servidor
local bp = turtle.loadBlueprint("BP-A1B2")
print("Diseño: " .. bp.name)
print("Dimensiones: " .. bp.sizeX .. "x" .. bp.sizeY .. "x" .. bp.sizeZ)
print("Bloques totales: " .. bp.totalBlocks)

-- Iniciar construcción en las coordenadas especificadas (o en su posición si se omiten)
turtle.buildBlueprint("BP-A1B2", 100, 64, 200)

-- Monitorear progreso
local prog = turtle.getBuildProgress()
print("Progreso: " .. prog.percentage .. "% (" .. prog.current .. "/" .. prog.total .. ")")

-- Control de ejecución
turtle.pauseBuild()
turtle.resumeBuild()
turtle.cancelBuild()
```

---

## 6. Portal Web y Visor de Diseños

Los jugadores pueden acceder al **Portal Web** del servidor ejecutando en el juego:
```text
/pc web
```

### Características del Portal Web:
1. **Subida de Archivos:** Arrastra y suelta directamente archivos `.litematic` (Litematica) o `.nbt` (Vanilla Structure Blocks).
2. **Visor 3D y Desglose de Capas:** Visualiza en el navegador capa por capa (eje Y) con slider interactivo y colores por tipo de bloque.
3. **Lista de Materiales Requeridos:** Muestra la lista exacta de bloques y cantidades necesarias para completar la construcción.
4. **Despacho Remoto a Tortugas:** Selecciona cualquier tortuga activa en el servidor, asigna las coordenadas deseadas y presiona **"⚡ INITIATE CONSTRUCTION"**.
5. **Progreso en Vivo:** Monitorea en tiempo real el porcentaje completado, bloques colocados y pausa o cancela el trabajo remotamente.