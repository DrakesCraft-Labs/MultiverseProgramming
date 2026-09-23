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

- Si el programa supera el límite se interrumpe y se muestra una advertencia: `<execution interrupted: exceeds 3000 ms>`.
- El `StackOverflowError` (recursión infinita) también se captura y se notifica.

En una **computadora avanzada** el límite es `advanced-execution-timeout-ms` (por defecto `0` = sin límite), así que los bucles pueden correr hasta terminar, la salida se transmite en vivo al chat y volver a pulsar el botón detiene el programa.

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
[Computer] ✘ string:2: 'end' expected
```