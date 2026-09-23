# Configuration

## config.yml

Created automatically the first time the plugin runs (`plugins/MultiverseProgramming/config.yml`).

```yaml
# Block that acts as a computer (right-click opens the GUI).
computer-block: LECTERN

# Maximum execution time of a program, in milliseconds.
execution-timeout-ms: 3000
```

| Key | Type | Default | Description |
|---|---|---|---|
| `computer-block` | `string` | `LECTERN` | Any `Material` name used as the computer block |
| `execution-timeout-ms` | `long` | `3000` | How long a program may run before being interrupted |

Config is loaded at server start. **Restart** the server after editing.

## Commands

| Command | Aliases | Description |
|---|---|---|
| `/pc disco` | `disk`, `nuevo` | Gives the player a floppy disk |
| `/pc run` | `ejecutar` | Runs the program on the disk in hand |
| `/pc help` | — | Lists the commands |
| `/pc` | — | Same as help |

Full plugin aliases: `/pc`, `/computador`, `/computadora`, `/disco`.

## Permissions

| Node | Default | Description |
|---|---|---|
| `multiverseprogramming.use` | `true` | Allows using computers and running disks |

Example of restricting:

```yaml
# permissions plugin example
multiverseprogramming.use: false
```

## chat messages

Chat messages are in Spanish, prefixed with `[Computadora]`:

- `✔ Código válido...` — code compiles.
- `✘ <error>` — syntax/runtime error.
- `(sin salida)` — program ran but printed nothing.

## Plugin messages

- **Gui title**: `Multiverse - Computadora`
- **Disk display name**: `Disquete`

## Build & dependencies

- Java **21**
- `paper-api` version **1.21.11-R0.1-SNAPSHOT** (provided scope, not bundled)
- `luaj-jse` **3.0.1** (bundled via Maven Shade)

To build:

```powershell
mvn -q -DskipTests package
```

Output: `target/MultiverseProgramming.jar`