# Computadora y disquetes

## El bloque computadora

Por defecto la computadora es un **atril (lectern)**. Al hacer clic derecho se abre la interfaz y se cancela el comportamiento normal del atril.

Para usar otro bloque como computadora, edita `config.yml`:

```yaml
computer-block: LECTERN
```

Sirve cualquier nombre de `Material` (p. ej. `BARREL`, `DISPENSER`, `JUKEBOX`). Recuerda reiniciar para aplicar los cambios de config.

### Diseño de la interfaz

La interfaz es una fila de 9 ranuras:

```
[0] [1] [2] [3] [4] [5] [6] [7] [8]
```

- **Ranura 0 — compartimento del disquete.** Solo se pueden insertar disquetes (libros). Cualquier otro objeto se rechaza con un mensaje en chat.
- **Ranuras 1–7 — decoración.** Cristal negro; no se pueden mover ni reemplazar.
- **Ranura 8 — botón de validar.** Una esmeralda verde con la etiqueta `✔ Validar código`. Al hacer clic valida el código del disquete.

Al cerrarse la interfaz (con error o con éxito), el disquete vuelve automáticamente al inventario del jugador.

## Disquetes

Los disquetes se crean con:

```
/pc disco
```

El objeto es un **libro y pluma** renombrado a "Disquete". La computadora acepta tanto libros sin firmar como firmados.

- El programa es la **secuencia de páginas** (se unen con `\n`).
- El disquete es la **fuente de verdad**: no hay "guardado en la computadora". Si cambias el disquete, cambias el programa.
- Puedes tener varios programas guardando varios disquetes en tu inventario.

### Reconocer un disquete

Cualquier `WRITABLE_BOOK` o `WRITTEN_BOOK` en tu mano se trata como disquete para `/pc run`. Otros tipos de objeto se rechazan.

## Ciclo de vida de un programa

```
escribir código en un disquete → insertar disquete → validar (botón) → sujetar disquete → /pc run → salida en chat
```

La validación solo **compila** el código (comprobación de sintaxis). La ejecución ocurre únicamente con `/pc run`.