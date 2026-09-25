# Computadora y disquetes

## El bloque computadora

Por defecto la computadora es un **atril (lectern)**. Al hacer clic derecho se abre la interfaz y se cancela el comportamiento normal del atril.

Para usar otro bloque como computadora, edita `config.yml`:

```yaml
computer-block: LECTERN
enable-computer: true
enable-recipe-computer: true
```

Sirve cualquier nombre de `Material` (p. ej. `BARREL`, `DISPENSER`, `JUKEBOX`). Puedes aplicar los cambios al instante sin reiniciar el servidor usando `/mvprog reload`.

## La computadora avanzada

Bloque por defecto: **mesa de encantamientos**. Consíguela con `/mvprog give advancedcomputer` o crafteándola (ver [Recetas](Recipes.md)). Se comporta como la normal con importantes mejoras:

- **Sin timeout corto & Bucles continuos**: Los programas pueden ejecutarse de forma prolongada o continua (`advanced-execution-timeout-ms`, `0` = sin límite por defecto).
- **Persistencia del disquete**: El disquete permanece dentro de la máquina incluso después de cerrar la interfaz; puedes retirarlo cuando quieras.
- **Indicador dinámico de estado (Ejecutar / Detener)**:
  - **Ranura 8 (En reposo)**: Muestra una esmeralda verde con el título `▶ Run Program`. Al hacer clic, valida el disquete y lanza el programa.
  - **Ranura 8 (En ejecución)**: Se transforma en un bloque de redstone resplandeciente titulado `⏹ Stop Program`. Al hacer clic, detiene inmediatamente la ejecución del programa.
- **Detención desacoplada**: Puedes extraer el disquete mientras el programa está corriendo y pulsar `⏹ Stop Program` en cualquier momento para finalizar el hilo de ejecución.

### Diseño de la interfaz

La interfaz es una fila de 9 ranuras:

```
[0] [1] [2] [3] [4] [5] [6] [7] [8]
```

- **Ranura 0 — Compartimento del disquete.** Solo se pueden insertar disquetes (libros). Cualquier otro objeto se rechaza con un mensaje en chat.
- **Ranuras 1–7 — Decoración protectora.** Paneles de cristal tintado; no se pueden retirar.
- **Ranura 8 — Botón de acción.**
  - Computadora estándar: Esmeralda verde `✔ Validate Code`.
  - Computadora avanzada: Esmeralda `▶ Run Program` o Bloque de Redstone `⏹ Stop Program`.

Al cerrarse la interfaz de una computadora normal, el disquete vuelve automáticamente al inventario del jugador. En la computadora avanzada, el disquete permanece guardado adentro.

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