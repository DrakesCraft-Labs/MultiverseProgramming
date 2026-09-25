# Configuración

## config.yml

El archivo de configuración se genera automáticamente la primera vez que arranca el plugin en `plugins/MultiverseProgramming/config.yml`.

```yaml
# ==============================================================================
# MultiverseProgramming Configuration
# ==============================================================================

# Tipos de bloques asignados a computadoras y periféricos
computer-block: LECTERN
advanced-computer-block: ENCHANTING_TABLE
monitor-block: OCHRE_FROGLIGHT
crafter-block: CRAFTER
transposer-block: HOPPER
speaker-block: NOTE_BLOCK
turtle-block: DISPENSER
scanner-block: OBSERVER
cartographer-block: CARTOGRAPHY_TABLE
alchemist-block: BREWING_STAND
farmer-block: COMPOSTER
quarry-block: BLAST_FURNACE
npc-block: SCULK_CATALYST

# Activadores de recetas de crafteo (habilitar/deshabilitar por máquina)
enable-recipe-computer: true
enable-recipe-advanced-computer: true
enable-recipe-turtle: true
enable-recipe-crafter: true
enable-recipe-monitor: true
enable-recipe-speaker: true
enable-recipe-transposer: true
enable-recipe-floppy-disk: true
enable-recipe-scanner: true
enable-recipe-cartographer: true
enable-recipe-alchemist: true
enable-recipe-farmer: true
enable-recipe-quarry: true
enable-recipe-npc: true

# Activadores de funcionalidad (habilitar/deshabilitar mecánicas por máquina)
enable-computer: true
enable-advanced-computer: true
enable-turtle: true
enable-crafter: true
enable-monitor: true
enable-speaker: true
enable-transposer: true
enable-scanner: true
enable-cartographer: true
enable-alchemist: true
enable-farmer: true
enable-quarry: true
enable-npc: true

# Integración con protección de regiones (WorldGuard & ProtectionStones)
worldguard-protection-check: true
protection-stones-require-owner: false

# Límites de ejecución y seguridad
execution-timeout-ms: 3000
advanced-execution-timeout-ms: 0
max-instructions-per-run: 1000000
drop-disks-on-break: true
prevent-spam-delay-ms: 500

# Parámetros de automatización de tortugas
turtle-fuel-required: false
turtle-fuel-per-action: 1
turtle-initial-fuel: 1000
turtle-max-fuel: 100000
turtle-build-delay-ticks: 1
turtle-require-materials: false

# Portal Web y Almacenamiento de Esquemáticas
web-portal-enabled: true
web-portal-host: "0.0.0.0"
web-portal-port: 8080
web-public-url: ""
blueprint-player-quota-mb: 15.0
```

### Tabla de Opciones de Configuración

| Clave | Tipo | Por defecto | Descripción |
|---|---|---|---|
| `computer-block` | `string` | `LECTERN` | Material usado como bloque de computadora estándar |
| `advanced-computer-block` | `string` | `ENCHANTING_TABLE` | Material usado como bloque de computadora avanzada |
| `enable-recipe-*` | `boolean` | `true` | Interruptor independiente para activar/desactivar la receta de crafteo de cada máquina |
| `enable-*` | `boolean` | `true` | Interruptor independiente para activar/desactivar la funcionalidad de cada máquina |
| `worldguard-protection-check` | `boolean` | `true` | Evita que las tortugas y canteras operen en regiones de WorldGuard sin permisos |
| `protection-stones-require-owner` | `boolean` | `false` | Si es `true`, exige que el dueño de la tortuga sea propietario (no solo miembro) en ProtectionStones |
| `turtle-fuel-required` | `boolean` | `false` | Si es `true`, genera un cofre de combustible con holograma `"Place fuel here"` y consume combustible |
| `turtle-require-materials` | `boolean` | `false` | Si es `true`, genera un cofre de materiales con holograma `"Place construction blocks here"` |
| `execution-timeout-ms` | `long` | `3000` | Tiempo máximo de ejecución para computadoras estándar antes de interrumpir el hilo |
| `advanced-execution-timeout-ms` | `long` | `0` | Tiempo máximo para computadoras avanzadas (`0` = sin límite de tiempo) |
| `max-instructions-per-run` | `long` | `1000000` | Límite seguro de instrucciones de CPU por bloque de tiempo para evitar congelamientos |
| `blueprint-player-quota-mb` | `double` | `15.0` | Cuota máxima de almacenamiento en disco para esquemáticas por jugador (en MB) |

---

## Comandos

Todos los comandos usan el prefijo `/mvprog` (con alias `/pc`, `/computador`, `/computadora`, `/disco`).

### Comandos de Jugador
| Comando | Permiso | Descripción |
|---|---|---|
| `/mvprog help` | `multiverseprogramming.use` | Muestra la lista de comandos disponibles |
| `/mvprog web` | `multiverseprogramming.use` | Muestra el enlace al portal web para subir esquemas `.litematic` y `.nbt` |
| `/mvprog get <código\|url>` | `multiverseprogramming.use` | Descarga esquemas desde la nube (Bytebin / GitHub) consumiendo cuota personal |
| `/mvprog quota` | `multiverseprogramming.use` | Consulta el almacenamiento utilizado y la cuota disponible |
| `/mvprog bp list` | `multiverseprogramming.use` | Lista todos los esquemas guardados en el servidor |
| `/mvprog bp quota` | `multiverseprogramming.use` | Muestra el estado de almacenamiento |
| `/mvprog bp delete <id>` | `multiverseprogramming.use` | Elimina un esquema propio para liberar cuota de disco |

### Comandos de Administrador
| Comando | Permiso | Descripción |
|---|---|---|
| `/mvprog build <bp\|código> <x> <y> <z> [tortuga] [clear] [orientación]` | `multiverseprogramming.admin` | Ordena a una tortuga construir una esquemática en coordenadas con rotación (`NORTH`, `EAST`, `SOUTH`, `WEST`, `0`, `90`, `180`, `270`) y despeje de obstáculos |
| `/mvprog quota <jugador>` | `multiverseprogramming.admin` | Consulta el almacenamiento y cuota de un jugador específico |
| `/mvprog getbypass <código\|url>` | `multiverseprogramming.admin` | Descarga esquemáticas omitiendo las cuotas de almacenamiento de jugadores |
| `/mvprog bp clean [días]` | `multiverseprogramming.admin` | Purga esquemáticas no ancladas más antiguas que los días indicados |
| `/mvprog give <objeto>` | `multiverseprogramming.admin` | Otorga cualquier ítem custom del plugin (soporta nombres en inglés y español) |
| `/mvprog reload` | `multiverseprogramming.admin` | Recarga `config.yml` y recetas dinámicamente sin reiniciar el servidor |

---

## Seguridad y Protección de Regiones

El plugin se integra automáticamente con **WorldGuard**, **ProtectionStones** y **CoreProtect**:
1. **Verificación de permisos de región**: Cuando una tortuga o cantera intenta construir o minar, el área de trabajo se valida contra las regiones protegidas. Si el dueño no tiene permisos de construcción en la región, la acción se cancela de inmediato.
2. **Registro de bloques en CoreProtect**: Todas las rupturas de bloques provocadas por tortugas y canteras quedan registradas en el historial de CoreProtect con sus metadatos y coordenadas.
3. **Protección contra duplicación**: Se bloquea la inserción de libros escritos con exploits NBT o contenedores anidados en marcos de ítems, vasijas y atriles para garantizar la estabilidad económica del servidor.