# Computadora y disquetes

## El bloque computadora

Por defecto la computadora es un **atril (lectern)**. Al hacer clic derecho se abre la interfaz y se cancela el comportamiento normal del atril.

Para usar otro bloque como computadora, edita `config.yml`:

```yaml
computer-block: LECTERN
```

Sirve cualquier nombre de `Material` (p. ej. `BARREL`, `DISPENSER`, `JUKEBOX`). Recuerda reiniciar para aplicar los cambios de config.

## La computadora avanzada

Bloque por defecto: **mesa de encantamientos**. Consíguela con `/pc give advancedcomputer` y colócala. Se comporta como la normal con dos diferencias:

- **Sin timeout corto** — los programas con bucles pueden correr hasta terminar (`advanced-execution-timeout-ms`, `0` = sin límite por defecto). Volver a pulsar el botón **detiene** el programa en curso.
- **Mantiene el disquete dentro** — el disquete permanece en la máquina aunque cierres la interfaz; puedes retirarlo cuando quieras.

### Diseño de la interfaz

La interfaz es una fila de 9 ranuras:

```
[0] [1] [2] [3] [4] [5] [6] [7] [8]
```

- **Ranura 0 — compartimento del disquete.** Solo se pueden insertar disquetes (libros). Cualquier otro objeto se rechaza con un mensaje en chat.
- **Ranuras 1–7 — decoración.** Cristal negro; no se pueden mover ni reemplazar.
- **Ranura 8 — botón de validar y ejecutar.** Una esmeralda verde con la etiqueta `✔ Validate Code`. Al hacer clic comprueba el código del disquete y, si es válido, ejecuta el programa.

Al cerrarse la interfaz (con error o con éxito), el disquete vuelve automáticamente al inventario del jugador.

## Disquetes

Los disquetes se crean en la mesa de crafteo (ver [Recetas](Recipes.md) para la mesa exacta).

El objeto es un **libro y pluma** renombrado a "Floppy Disk". La computadora acepta tanto libros sin firmar como firmados.

- El programa es la **secuencia de páginas** (se unen con `\n`).
- El disquete es la **fuente de verdad**: no hay "guardado en la computadora". Si cambias el disquete, cambias el programa.
- Puedes tener varios programas guardando varios disquetes en tu inventario.

### Reconocer un disquete

Cualquier `WRITABLE_BOOK` o `WRITTEN_BOOK` se reconoce como disquete. Otros tipos de objeto se rechazan.

## Ciclo de vida de un programa

```
escribir código en un disquete → insertar disquete → pulsar el botón → el programa se ejecuta → salida en chat
```