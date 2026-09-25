# MultiverseProgramming // Developer Documentation Hub 🛠️
# Centro de Documentación para Desarrolladores 🛠️

Welcome to the internal technical documentation of **MultiverseProgramming**. This section details the complete software architecture, codebase structure, subsystem lifecycles, and the communication bridge connecting the Paper Minecraft plugin with the standalone Web Portal.

Bienvenido a la documentación técnica interna de **MultiverseProgramming**. Este apartado detalla a fondo la arquitectura de software, la estructura del código fuente, el ciclo de vida de los subsistemas y el puente de comunicación que conecta el plugin de Paper Minecraft con el Portal Web interactivo.

---

## 📚 Table of Contents / Índice de Contenidos

### 🇬🇧 English Documentation
1. **[Core Architecture & Subsystems (English)](Architecture-EN.md)**
   - Plugin lifecycle (`MultiverseProgrammingPlugin`)
   - Sandboxed Lua runtime (`LuaRunner`) & instruction limiting
   - Threading model & synchronous dispatching (`SyncDispatcher`)
   - Floppy disk storage & duplication exploit security
   - NBT parsing (`NbtReader`) and 3D Blueprint rotation math (`BlueprintRotator`)

2. **[Web Portal & Communication Bridge (English)](Web-Portal-and-Bridge-EN.md)**
   - Embedded HTTP Server (`WebServerManager`) & port failover
   - Single-Page Application (SPA) architecture (`dashboard.html`, Three.js 3D WebGL engine)
   - REST API specification (`/api/blueprints`, `/api/upload`, `/api/turtles`, `/api/build`, etc.)
   - Bidirectional communication flow between browser and Minecraft server tick loop
   - Base64 payload decoding, validation, and storage quotas

3. **[Turtles, Quarry Engine & Peripherals (English)](Turtles-and-Peripherals-EN.md)**
   - Turtle state machine, inventory mechanics & fuel accumulator
   - Lateral Quarry Engine upgrade: physical lockstep attachment, dual holographic chests & auto-pause
   - Peripheral discovery (`PeripheralManager`) and deep dive into the 9 custom peripherals
   - Claim protection integrations (WorldGuard, GriefPrevention, Towny, Lands, Residence)
   - CoreProtect audit logging bridge (`CoreProtectBridge`)

---

### 🇪🇸 Documentación en Español
1. **[Arquitectura del Núcleo y Subsistemas (Español)](Architecture-ES.md)**
   - Ciclo de vida del plugin (`MultiverseProgrammingPlugin`)
   - Entorno aislado de Lua (`LuaRunner`) y cuota de instrucciones de CPU
   - Modelo de hilos y despacho síncrono seguro (`SyncDispatcher`)
   - Almacenamiento en disquetes y prevención de exploits de duplicación
   - Parser binario NBT (`NbtReader`) y matemáticas de rotación 3D (`BlueprintRotator`)

2. **[Portal Web y Puente de Comunicación (Español)](Web-Portal-and-Bridge-ES.md)**
   - Servidor HTTP embebido (`WebServerManager`) y conmutación por error de puertos
   - Arquitectura SPA del portal (`dashboard.html`, motor Three.js WebGL 3D)
   - Especificación completa de la API REST (`/api/blueprints`, `/api/upload`, `/api/turtles`, etc.)
   - Flujo de comunicación bidireccional entre navegador y el bucle principal de Minecraft
   - Decodificación Base64, validación de esquemas y cuotas por jugador

3. **[Tortugas, Motor de Cantera y Periféricos (Español)](Turtles-and-Peripherals-ES.md)**
   - Máquina de estados de la Tortuga, inventario y acumulador de combustible
   - Mejora de Cantera Lateral (Quarry Engine): acoplamiento físico en lockstep, cofres duales y pausa automática
   - Detección dinámica de periféricos (`PeripheralManager`) y análisis de los 9 periféricos
   - Protección de reclamos (WorldGuard, GriefPrevention, Towny, Lands, Residence)
   - Auditoría y registro con CoreProtect (`CoreProtectBridge`)

---

## 🧭 Repository Map / Mapa del Repositorio

```text
MultiverseProgramming/
├── src/main/java/com/multiverse/programming/
│   ├── MultiverseProgrammingPlugin.java  # Main plugin entry point (JavaPlugin)
│   ├── ConfigManager.java                # config.yml reader, defaults, validation
│   ├── DiskManager.java                  # Floppy disk NBT, item creation & program IO
│   ├── LuaRunner.java                    # Sandboxed LuaJ VM, instruction counter hook
│   ├── ComputerCommand.java              # /mvprog command handler & tab completions
│   ├── ComputerListener.java             # Computer block GUI & execution handler
│   ├── ItemSecurityListener.java         # Duplication exploit & container protection
│   ├── RecipeManager.java                # Dynamic shapeless/shaped crafting recipes
│   ├── blueprint/                        # NBT stream reading, litematica parser, rotator
│   │   ├── Blueprint.java
│   │   ├── BlueprintManager.java
│   │   ├── BlueprintParser.java
│   │   ├── BlueprintRotator.java
│   │   └── NbtReader.java
│   ├── peripheral/                       # Peripherals API & 9 custom machine adapters
│   │   ├── Peripheral.java
│   │   ├── PeripheralManager.java
│   │   ├── SyncDispatcher.java           # Async-to-Sync Bukkit bridge
│   │   ├── MonitorPeripheral.java
│   │   ├── CrafterPeripheral.java
│   │   ├── TransposerPeripheral.java
│   │   ├── SpeakerPeripheral.java
│   │   ├── ScannerPeripheral.java
│   │   ├── CartographerPeripheral.java
│   │   ├── AlchemistPeripheral.java
│   │   ├── FarmerPeripheral.java
│   │   ├── QuarryPeripheral.java
│   │   └── NpcPeripheral.java
│   ├── protection/                       # Third-party claim hooks & CoreProtect logging
│   │   ├── ProtectionManager.java
│   │   └── CoreProtectBridge.java
│   ├── turtle/                           # Autonomous mobile turtle block & quarry upgrade
│   │   ├── Turtle.java
│   │   ├── TurtleManager.java
│   │   ├── TurtlePeripheral.java
│   │   ├── TurtleGUI.java
│   │   └── TurtleListener.java
│   └── web/                              # Embedded HTTP server & dashboard router
│       ├── WebServerManager.java
│       └── WebDashboardHtml.java
├── src/main/resources/
│   ├── config.yml                        # Server configuration template
│   ├── plugin.yml                        # Paper/Spigot plugin descriptor
│   └── dashboard.html                    # Single-Page Web Portal (Three.js WebGL)
└── wiki/
    ├── dev/                              # Developer documentation (EN / ES)
    ├── en/                               # Player wiki (English)
    └── es/                               # Player wiki (Spanish)
```
