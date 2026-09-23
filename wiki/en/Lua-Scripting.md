# Lua Scripting

The plugin embeds **Luaj 3.0** (Lua in pure Java). No external install is needed.

## Available libraries

Programs can use the safe standard Lua libraries:

- `string` — `string.format`, `string.sub`, …
- `table` — `table.insert`, `table.concat`, …
- `math` — `math.random`, `math.floor`, …
- `coroutine`
- `bit32`

The global function `print(...)` is captured and sent to the executing player's chat.

## Removed / disabled features

For safety these are set to `nil`:

| Global | Reason |
|---|---|
| `io` | No file access |
| `os` | No OS commands, no time tricks, no exit |
| `luajava` | No access to Java classes |
| `package` | No `require` / module loading |
| `dofile`, `loadfile` | No reading files from disk |

## Execution limits

Every program runs on its own worker thread with a timeout configured in:

```yaml
execution-timeout-ms: 3000
```

- If a program exceeds the limit it is interrupted and a warning is shown: `<execution interrupted: exceeds 3000 ms>`.
- `StackOverflowError` (infinite recursion) is also caught and reported.

## Examples

### Loop and math

```lua
print("Primeros 5 cuadrados:")
for i = 1, 5 do
  print(i, i * i)
end
```

### Tables

```lua
local inventario = { "manzana", "hierro", "diamante" }
table.insert(inventario, "redstone")
print("Tienes " .. #inventario .. " objetos")
for _, obj in ipairs(inventario) do
  print("- " .. obj)
end
```

### String functions

```lua
local nombre = "Mundo"
print(string.format("Hola, %s!", nombre))
print(string.upper(nombre))
```

## Error reporting

Syntax or runtime errors are shown in chat prefixed with `✘`, exactly as reported by Lua, including the line number where possible:

```
[Computer] ✘ string:2: 'end' expected
```

## What is not available (yet)

There are currently **no world interaction functions** (no turtle move/build). Lua is restricted to pure computation and `print` output. If you want an API to move players, place blocks or read redstone, that would be the next step for this plugin — everything is ready for it, since the computer already communicates with the player through the chat.