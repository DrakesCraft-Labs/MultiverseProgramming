# Programación en Lua

El plugin integra **Luaj 3.0** (Lua en Java puro). No necesita instalar nada más.

## Librerías disponibles

Los programas pueden usar las librerías estándar de Lua seguras:

- `string` — `string.format`, `string.sub`, …
- `table` — `table.insert`, `table.concat`, …
- `math` — `math.random`, `math.floor`, …
- `coroutine`
- `bit32`

La función global `print(...)` se captura y se envía al chat del jugador que ejecuta.

## Funciones desactivadas / eliminadas

Por seguridad se ponen a `nil`:

| Global | Motivo |
|---|---|
| `io` | Sin acceso a archivos |
| `os` | Sin comandos de sistema, sin trucos de hora, sin salir |
| `luajava` | Sin acceso a clases de Java |
| `package` | Sin `require` / carga de módulos |
| `dofile`, `loadfile` | Sin leer archivos del disco |

## Límites de ejecución

Cada programa corre en su propio hilo con un timeout configurable en:

```yaml
execution-timeout-ms: 3000
```

- Si el programa supera el límite se interrumpe y se muestra una advertencia: `<ejecución interrumpida: supera 3000 ms>`.
- El `StackOverflowError` (recursión infinita) también se captura y se notifica.

## Ejemplos

### Bucle y matemáticas

```lua
print("Primeros 5 cuadrados:")
for i = 1, 5 do
  print(i, i * i)
end
```

### Tablas

```lua
local inventario = { "manzana", "hierro", "diamante" }
table.insert(inventario, "redstone")
print("Tienes " .. #inventario .. " objetos")
for _, obj in ipairs(inventario) do
  print("- " .. obj)
end
```

### Funciones de string

```lua
local nombre = "Mundo"
print(string.format("Hola, %s!", nombre))
print(string.upper(nombre))
```

## Reporte de errores

Los errores de sintaxis o ejecución se muestran en chat con el prefijo `✘`, tal y como los reporta Lua, incluyendo el número de línea cuando es posible:

```
[Computadora] ✘ string:2: 'end' expected
```

## Qué no está disponible (todavía)

Actualmente **no hay funciones de interacción con el mundo** (sin turtle que se mueva o construya). Lua se limita a computación pura y salida con `print`. Si quieres una API para mover jugadores, colocar bloques o leer redstone, ese sería el siguiente paso de este plugin — todo está preparado, ya que la computadora se comunica con el jugador a través del chat.