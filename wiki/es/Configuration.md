# Configuración

## config.yml

Se crea automáticamente la primera vez que el plugin arranca (`plugins/MultiverseProgramming/config.yml`).

```yaml
# Bloque que actúa como computadora (clic derecho abre la interfaz).
computer-block: LECTERN

# Tiempo máximo de ejecución de un programa, en milisegundos.
execution-timeout-ms: 3000

# Bloque que actúa como computadora avanzada (bucles, sin timeout corto).
advanced-computer-block: ENCHANTING_TABLE

# Tiempo máximo de ejecución en la avanzada, en milisegundos. 0 = sin límite.
advanced-execution-timeout-ms: 0
```

| Clave | Tipo | Por defecto | Descripción |
|---|---|---|---|
| `computer-block` | `string` | `LECTERN` | Cualquier nombre de `Material` usado como bloque computadora |
| `execution-timeout-ms` | `long` | `3000` | Cuánto puede tardar un programa antes de ser interrumpido |
| `advanced-computer-block` | `string` | `ENCHANTING_TABLE` | Cualquier nombre de `Material` usado como computadora avanzada |
| `advanced-execution-timeout-ms` | `long` | `0` | Tiempo máximo en la avanzada; `0` significa sin límite |

La config se carga al arrancar el servidor. **Reinicia** el servidor después de editarla.

## Comandos

| Comando | Aliases | Descripción |
|---|---|---|
| `/pc give <item>` | `floppydisk`, `computer`, `advancedcomputer` | Admin: te da un objeto custom |
| `/pc help` | — | Lista los comandos |
| `/pc` | — | Igual que help |

Alias globales del plugin: `/pc`, `/computador`, `/computadora`, `/disco`.

## Permisos

| Nodo | Por defecto | Descripción |
|---|---|---|
| `multiverseprogramming.use` | `true` | Permite usar computadoras y ejecutar disquetes |
| `multiverseprogramming.admin` | `op` | Permite dar objetos custom (`/pc give`) |

Ejemplo para restringir:

```yaml
# ejemplo de plugin de permisos
multiverseprogramming.use: false
```

## Mensajes en chat

Los mensajes del chat están en inglés, con el prefijo `[Computer]`:

- `Running program…` — el programa se está ejecutando.
- `✘ <error>` — error de sintaxis/ejecución.
- `(no output)` — el programa corrió pero no imprimió nada.

## Textos del plugin

- **Títulos de la interfaz**: `Computer` y `Advanced Computer`
- **Nombre del disquete**: `Floppy Disk`

## Build y dependencias

- Java **21**
- `paper-api` versión **1.21.11-R0.1-SNAPSHOT** (scope provided, no se incluye)
- `luaj-jse` **3.0.1** (incluido con Maven Shade)

Para compilar:

```powershell
mvn -q -DskipTests package
```

Salida: `target/MultiverseProgramming-<version>.jar` (coincide con `<version>` del pom)