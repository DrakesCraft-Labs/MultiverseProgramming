# Configuration

## config.yml

The configuration file is created automatically at `plugins/MultiverseProgramming/config.yml` on first run.

```yaml
# ==============================================================================
# MultiverseProgramming Configuration
# ==============================================================================

# Computer block types
computer-block: LECTERN
advanced-computer-block: ENCHANTING_TABLE
monitor-block: OCHRE_FROGLIGHT
crafter-block: CRAFTER
transposer-block: HOPPER
speaker-block: NOTE_BLOCK
turtle-block: DISPENSER

# Crafting Recipes Toggles (enable/disable crafting per machine)
enable-recipe-computer: true
enable-recipe-advanced-computer: true
enable-recipe-turtle: true
enable-recipe-crafter: true
enable-recipe-monitor: true
enable-recipe-speaker: true
enable-recipe-transposer: true
enable-recipe-floppy-disk: true

# Functionality Toggles (enable/disable machine mechanics)
enable-computer: true
enable-advanced-computer: true
enable-turtle: true
enable-crafter: true
enable-monitor: true
enable-speaker: true
enable-transposer: true

# Region Protection Integration
worldguard-protection-check: true
protection-stones-require-owner: false

# Execution and Safety Limits
execution-timeout-ms: 3000
advanced-execution-timeout-ms: 0
max-instructions-per-run: 1000000
drop-disks-on-break: true
prevent-spam-delay-ms: 500

# Turtle Automation Settings
turtle-fuel-required: false
turtle-fuel-per-action: 1
turtle-initial-fuel: 1000
turtle-max-fuel: 100000
turtle-build-delay-ticks: 1
turtle-require-materials: false

# Blueprint Nexus & Web Server
web-portal-enabled: true
web-portal-host: "0.0.0.0"
web-portal-port: 8080
web-public-url: ""
blueprint-player-quota-mb: 15.0
```

### Configuration Options Reference

| Key | Type | Default | Description |
|---|---|---|---|
| `computer-block` | `string` | `LECTERN` | Material used as standard computer block |
| `advanced-computer-block` | `string` | `ENCHANTING_TABLE` | Material used as advanced computer block |
| `enable-recipe-*` | `boolean` | `true` | Independent toggle to enable or disable crafting recipe for each device |
| `enable-*` | `boolean` | `true` | Independent toggle to enable or disable gameplay functionality for each device |
| `worldguard-protection-check` | `boolean` | `true` | Prevents turtles from building in WorldGuard regions without membership/ownership |
| `protection-stones-require-owner` | `boolean` | `false` | When `true`, requires the turtle owner to be the region owner (not just member) in ProtectionStones |
| `turtle-fuel-required` | `boolean` | `false` | When `true`, spawns a dedicated Fuel Chest with hologram `"Place fuel here"` and requires fuel |
| `turtle-require-materials` | `boolean` | `false` | When `true`, spawns a Supply Chest with hologram `"Place construction blocks here"` |
| `execution-timeout-ms` | `long` | `3000` | Max run time for standard computers before timeout |
| `advanced-execution-timeout-ms` | `long` | `0` | Max run time for advanced computers (`0` = no limit) |
| `max-instructions-per-run` | `long` | `1000000` | Safety instruction ceiling to prevent infinite loops from hanging the thread |
| `blueprint-player-quota-mb` | `double` | `15.0` | Maximum blueprint disk storage allocation per player |

---

## Commands

All commands are prefixed with `/mvprog` (aliases `/pc`, `/computador`, `/computadora`, `/disco`).

### Player Commands
| Command | Permission | Description |
|---|---|---|
| `/mvprog help` | `multiverseprogramming.use` | Displays available commands and usage guide |
| `/mvprog web` | `multiverseprogramming.use` | Displays Web Dashboard link for uploading `.litematic` & `.nbt` |
| `/mvprog get <code\|url>` | `multiverseprogramming.use` | Downloads blueprint from cloud nexus (counts towards quota) |
| `/mvprog quota` | `multiverseprogramming.use` | Displays player's blueprint storage usage and remaining quota |
| `/mvprog bp list` | `multiverseprogramming.use` | Lists all saved blueprints and sizes |
| `/mvprog bp quota` | `multiverseprogramming.use` | Shows storage quota status |
| `/mvprog bp delete <id>` | `multiverseprogramming.use` | Deletes a personal blueprint to free up quota |

### Administrator Commands
| Command | Permission | Description |
|---|---|---|
| `/mvprog build <bp\|code> <x> <y> <z> [turtle] [clear] [orientation]` | `multiverseprogramming.admin` | Orders a turtle to construct a blueprint at coordinates with optional orientation (`NORTH`, `EAST`, `SOUTH`, `WEST`, `0`, `90`, `180`, `270`) and obstruction clearing |
| `/mvprog quota <player>` | `multiverseprogramming.admin` | Checks storage quota and disk usage of a specific player |
| `/mvprog getbypass <code\|url>` | `multiverseprogramming.admin` | Downloads blueprints bypassing personal storage quotas |
| `/mvprog bp clean [days]` | `multiverseprogramming.admin` | Purges unpinned blueprints older than specified days |
| `/mvprog give <item>` | `multiverseprogramming.admin` | Gives custom plugin items (`floppydisk`, `computer`, `advancedcomputer`, `turtle`, `monitor`, `crafter`, `transposer`, `speaker`) |
| `/mvprog reload` | `multiverseprogramming.admin` | Reloads `config.yml` and crafting recipes dynamically without server restart |

---

## Region Protection & Security

The plugin automatically hooks into **WorldGuard** and **ProtectionStones** (without hard dependencies):
1. **Region Permission Verification**: When a turtle initiates a build, the bounding volume is verified against claims. If the turtle's owner is not a member or owner of the claim, the build is cancelled immediately.
2. **Owner-Only Restrictions**: By enabling `protection-stones-require-owner: true`, admins can enforce that only region owners (not regular members) can build using turtles inside claims.
3. **Admin Bypass**: Server administrators with `multiverseprogramming.admin` or `*` bypass claim restrictions.
4. **Dupe & Chunk-Ban Prevention**: Turtles and automated containers strictly prohibit storing nested containers (e.g. shulker boxes inside chests) and writable/written books inside container inventories during construction.