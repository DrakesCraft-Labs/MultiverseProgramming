# Web Portal, Embedded Server & Communication Bridge (English) 🌐

This document provides a deep technical breakdown of the **Blueprint Nexus Web Portal**, the **Embedded HTTP Server (`WebServerManager`)**, and the bidirectional communication bridge that connects web browsers to the live Minecraft server.

---

## 1. High-Level Communication Architecture

The Web Portal allows players and administrators to upload Minecraft schematics (`.litematic` and `.nbt`), preview structures in interactive 3D WebGL, inspect layer-by-layer cross-sections, and remotely order in-game **Turtles** to construct them.

```
┌────────────────────────────────────────────────────────┐
│                   Web Browser Client                   │
│   (dashboard.html: Three.js 3D Engine, Slicer, UI)     │
└───────────────────────────┬────────────────────────────┘
                            │ HTTP / JSON REST Requests
                            ▼
┌────────────────────────────────────────────────────────┐
│             Embedded HttpServer (Paper Server)         │
│   Port: 8080 (or auto-fallback 8081..8089)             │
│   Executor: 4 Daemon Workers ("MultiverseWeb-Worker")  │
└───────────────────────────┬────────────────────────────┘
                            │
                            ├── Reads Blueprints Cache
                            ├── Validates Storage Quotas
                            │
                            │ Bukkit.getScheduler().runTask(plugin, ...)
                            ▼
┌────────────────────────────────────────────────────────┐
│               Minecraft Main Tick Thread               │
│   (Turtle State Machine, World Blocks, Holograms)      │
└────────────────────────────────────────────────────────┘
```

---

## 2. Embedded HTTP Server (`WebServerManager.java`)

MultiverseProgramming embeds the JDK's native, lightweight `com.sun.net.httpserver.HttpServer`. This avoids shipping heavy third-party web frameworks (Netty, Jetty, Spring) while achieving sub-millisecond request latencies.

### A. Automatic Port Conflict Resolution
In multi-server networks or shared hosting environments, port conflicts are common. `WebServerManager.start()` implements automatic sequential fallback:
```java
int preferredPort = plugin.getConfigManager().getWebPortalPort(); // e.g. 8080
int maxAttempts = 10;

for (int attempt = 0; attempt < maxAttempts; attempt++) {
    int candidatePort = preferredPort + attempt;
    try {
        createdServer = HttpServer.create(new InetSocketAddress(bindAddress, candidatePort), 0);
        boundPort = candidatePort;
        if (attempt > 0) {
            plugin.getLogger().warning("[WebPortal] Configured port " + preferredPort 
                + " was busy! Automatically switched to fallback port " + boundPort);
        }
        break;
    } catch (BindException e) {
        // Tries next sequential port up to candidatePort + 9
    }
}
```

### B. Concurrency & Threading Model
- **Worker Pool**: `ThreadPoolExecutor` with **4 daemon threads** named `MultiverseWeb-Worker`.
- **Async Execution**: Incoming HTTP connections are handled independently of the Minecraft server tick loop. Heavy operations (Base64 decoding, Gzip inflation, JSON serialization) never cause TPS drops.
- **Main Thread Handoff**: When a web handler needs to mutate game state (e.g., dispatching a build to a Turtle), it schedules execution onto the server's primary thread using `Bukkit.getScheduler().runTask(plugin, () -> turtle.startBuild(...))`.

### C. CORS & Cross-Origin Security
Every endpoint implements standard Cross-Origin Resource Sharing (CORS) headers:
- `Access-Control-Allow-Origin: *`
- `Access-Control-Allow-Methods: GET, POST, OPTIONS`
- `Access-Control-Allow-Headers: Content-Type, Authorization`
- `OPTIONS` preflight requests are caught and answered immediately with HTTP `204 No Content`.

---

## 3. Complete REST API Specification

| Endpoint | Method | Description | Request Body | Response Body |
| :--- | :---: | :--- | :--- | :--- |
| `/` | `GET` | Serves the web dashboard SPA HTML | None | HTML stream (`text/html`) |
| `/api/blueprints` | `GET` | Lists all registered blueprints | None | Array of blueprint metadata |
| `/api/blueprints/{id}` | `GET` | Fetches a specific blueprint with blocks | None | Blueprint JSON object |
| `/api/upload` | `POST` | Uploads a `.litematic` or `.nbt` file | JSON with base64 data | `{ ok: true, blueprint: {...} }` |
| `/api/turtles` | `GET` | Lists active turtles and build progress | None | Array of Turtle status objects |
| `/api/build` | `POST` | Dispatches build to a Turtle | JSON dispatch parameters | `{ ok: true, message: "..." }` |
| `/api/pause` | `POST` | Pauses or resumes an active build | `{ turtleId: "T-001" }` | `{ ok: true, status: "..." }` |
| `/api/cancel` | `POST` | Cancels an active build task | `{ turtleId: "T-001" }` | `{ ok: true }` |
| `/api/quota` | `GET` | Checks player blueprint storage quota | Query param: `?player=Name` | Storage quota breakdown |
| `/api/delete` | `POST` | Deletes a stored blueprint | `{ blueprintId: "...", player: "..." }` | `{ ok: true }` |

---

## 4. Deep Dive into Key Endpoints

### A. Blueprint Upload Pipeline (`POST /api/upload`)
```json
{
  "filename": "castle_tower.litematic",
  "data": "H4sICDy...",
  "player": "Steve"
}
```
1. **Extraction**: The JSON payload is parsed; base64 payload is decoded into a raw `byte[]`.
2. **Quota Verification**: `BlueprintManager.getPlayerUsageBytes(player)` checks if adding `rawBytes.length` exceeds `blueprint-player-quota-mb` (default: 10 MB). If exceeded, HTTP `400 Bad Request` is returned.
3. **Format Parsing**: `BlueprintParser.parse(filename, rawBytes)` inspects magic bytes, inflates Gzip stream, and constructs an immutable [`Blueprint`](file:///c:/Users/danie/OneDrive/Documentos/Github/Personal/MultiverseProgramming/src/main/java/com/multiverse/programming/blueprint/Blueprint.java) instance.
4. **Disk Storage**: The file is persisted to `plugins/MultiverseProgramming/blueprints/` and indexed in memory.

### B. Remote Turtle Dispatch (`POST /api/build`)
```json
{
  "blueprintId": "BP-7A9B",
  "turtleId": "T-001",
  "x": 120,
  "y": 64,
  "z": -250,
  "clear": true,
  "orientation": "EAST"
}
```
1. **Entity Lookup**: Retrieves `Turtle` by ID from `TurtleManager`.
2. **Rotation Calculation**: Resolves `orientation` ("NORTH", "EAST", "SOUTH", "WEST", or degrees) via `BlueprintRotator.normalizeRotation()`.
3. **Primary Thread Dispatch**: Dispatches `turtle.startBuild(bp, origin, delay, requireMaterials, clear, rotation, onDone, onError)` onto the Bukkit scheduler.
4. **Live Response**: Returns HTTP `200 OK` confirming the dispatch.

---

## 5. Web Frontend Architecture (`dashboard.html`)

The frontend is a modern, zero-dependency Single-Page Application (SPA) embedded directly inside the JAR at `src/main/resources/dashboard.html`.

### A. Three.js 3D WebGL Visualization
- **Renderer**: Uses `THREE.WebGLRenderer` with antialiasing and sRGB encoding.
- **Voxel Palette**: A palette dictionary maps Minecraft block tags (e.g. `minecraft:stone`, `minecraft:oak_planks`, `minecraft:water`) to hex color codes.
- **Mesh Optimization**: Rather than creating individual meshes with separate draw calls, blocks are rendered as instanced voxels or batched geometries to ensure smooth 60 FPS performance even on large builds.
- **Camera Controls**: Features mouse drag-to-orbit, right-click to pan, and wheel to zoom.

### B. 2D Layer Slicer
- A vertical range slider filters voxels by Y layer ($Y = 0 \dots H$).
- Allows players to inspect internal wiring, redstone, and structural slices layer-by-layer before building.

### C. Bilingual Localization (i18n)
- The header includes a language toggle button (`EN / ES`).
- All UI strings, tooltip badges, and alert messages are dynamically switched without triggering page reloads.
