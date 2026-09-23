# Configuration

## config.yml

Created automatically the first time the plugin runs (`plugins/MultiverseProgramming/config.yml`).

```yaml
# Block that acts as a computer (right-click opens the GUI).
computer-block: LECTERN

# Maximum execution time of a program, in milliseconds.
execution-timeout-ms: 3000

# Block that acts as an advanced computer (loops, no short timeout).
advanced-computer-block: ENCHANTING_TABLE

# Maximum execution time on an advanced computer, in milliseconds. 0 = no limit.
advanced-execution-timeout-ms: 0
```

| Key | Type | Default | Description |
|---|---|---|---|
| `computer-block` | `string` | `LECTERN` | Any `Material` name used as the computer block |
| `execution-timeout-ms` | `long` | `3000` | How long a program may run before being interrupted |
| `advanced-computer-block` | `string` | `ENCHANTING_TABLE` | Any `Material` name used as the advanced computer |
| `advanced-execution-timeout-ms` | `long` | `0` | Max run time on the advanced computer; `0` means no limit |

Config is loaded at server start. **Restart** the server after editing.

## Commands

| Command | Aliases | Description |
|---|---|---|
| `/pc give <item>` | `floppydisk`, `computer`, `advancedcomputer` | Admin: gives you a custom item |
| `/pc help` | — | Lists the commands |
| `/pc` | — | Same as help |

Full plugin aliases: `/pc`, `/computador`, `/computadora`, `/disco`.

## Permissions

| Node | Default | Description |
|---|---|---|
| `multiverseprogramming.use` | `true` | Allows using computers and running disks |
| `multiverseprogramming.admin` | `op` | Allows giving custom items (`/pc give`) |

Example of restricting:

```yaml
# permissions plugin example
multiverseprogramming.use: false
```

## Chat messages

Chat messages are prefixed with `[Computer]`:

- `Running program…` — the program is executing.
- `✘ <error>` — syntax/runtime error.
- `(no output)` — program ran but printed nothing.

## Plugin messages

- **GUI titles**: `Computer` and `Advanced Computer`
- **Disk display name**: `Floppy Disk`

## Build & dependencies

- Java **21**
- `paper-api` version **1.21.11-R0.1-SNAPSHOT** (provided scope, not bundled)
- `luaj-jse` **3.0.1** (bundled via Maven Shade)

To build:

```powershell
mvn -q -DskipTests package
```

Output: `target/MultiverseProgramming-<version>.jar` (matches the pom's `<version>`)