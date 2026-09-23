# Primeros pasos

## 1. Instalar el plugin

1. Pon `MultiverseProgramming.jar` en la carpeta `plugins/` de tu servidor.
2. Reinicia el servidor.
3. Comprueba `plugins/MultiverseProgramming/config.yml` — se genera en el primer arranque.
4. Verifica en la consola el mensaje `MultiverseProgramming enabled`.

## 2. Consigue un disquete

Craftea un **Floppy Disk** (ver [Recetas](Recipes.md)).

Recibes un **Floppy Disk** —un libro y pluma con una plantilla de Lua ya incluida en la primera página.

## 3. Escribe un programa

Abre el disquete y escribe tu programa en sus páginas. Cada página es parte del programa (las páginas se unen con saltos de línea).

Un programa sencillo:

```lua
-- mi primer programa
local mensaje = "hola desde Lua"
print(mensaje)
```

## 4. Crea / abre una computadora

Coloca un **atril (lectern)** y haz **clic derecho** sobre él. Se abre la interfaz de la computadora.

> El bloque es configurable en `config.yml` (`computer-block`). Si lo cambias, el nuevo bloque pasa a ser la computadora.

## 5. Validar y ejecutar

- Pon el disquete en la **ranura 0** (la primera casilla vacía).
- Haz clic en el botón verde **`✔ Validate Code`** (la esmeralda, ranura 8).

Dos resultados posibles:

- **Error de sintaxis** → la interfaz se cierra y el error de Lua (con número de línea) sale en rojo en el chat.
- **Código válido** → el programa se ejecuta inmediatamente **en la sandbox**. Todo lo que haga `print()` se muestra en tu chat; si no genera salida verás `(no output)`.

## Errores comunes

- Meter el disquete en otra ranura → solo la ranura 0 acepta disquetes.
- Bucle infinito → el programa se interrumpe tras `execution-timeout-ms` (por defecto 3000).