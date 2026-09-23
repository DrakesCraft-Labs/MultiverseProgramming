# Configuración

## config.yml

Se crea automáticamente la primera vez que el plugin arranca (`plugins/MultiverseProgramming/config.yml`).

```yaml
# Bloque que actúa como computadora (clic derecho abre la interfaz).
computer-block: LECTERN

# Tiempo máximo de ejecución de un programa, en milisegundos.
execution-timeout-ms: 3000
```

| Clave | Tipo | Por defecto | Descripción |
|---|---|---|---|
| `computer-block` | `string` | `LECTERN` | Cualquier nombre de `Material` usado como bloque computadora |
| `execution-timeout-ms` | `long` | `3000` | Cuánto puede tardar un programa antes de ser interrumpido |

La config se carga al arrancar el servidor. **Reinicia** el servidor después de editarla.

## Comandos

| Comando | Aliases | Descripción |
|---|---|---|
| `/pc run` | `ejecutar` | Ejecuta el programa del disquete en la mano |
| `/pc give <item>` | `floppydisk`, `computer` | Admin: te da un objeto custom |
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

- `✔ Code is valid...` — el código compila.
- `✘ <error>` — error de sintaxis/ejecución.
- `(no output)` — el programa corrió pero no imprimió nada.

## Textos del plugin

- **Título de la interfaz**: `Multiverse - Computer`
- **Nombre del disquete**: `Floppy Disk`

## Build y dependencias

- Java **21**
- `paper-api` versión **1.21.11-R0.1-SNAPSHOT** (scope provided, no se incluye)
- `luaj-jse` **3.0.1** (incluido con Maven Shade)

Para compilar:

```powershell
mvn -q -DskipTests package
```

Salida: `target/MultiverseProgramming.jar`