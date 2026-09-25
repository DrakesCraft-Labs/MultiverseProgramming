# Portal Web, Servidor Embebido y Puente con el Plugin (Español) 🌐

Este documento detalla a fondo la arquitectura del **Portal Web (Blueprint Nexus)**, el **Servidor HTTP Embebido (`WebServerManager`)** y el puente de comunicación bidireccional que conecta los navegadores web con el servidor de Minecraft en tiempo real.

---

## 1. Arquitectura de Comunicación General

El Portal Web permite a los jugadores y administradores subir esquemas de Minecraft (`.litematic` y `.nbt`), previsualizar estructuras en 3D interactivo con WebGL, inspeccionar cortes capa por capa y ordenar a las **Tortugas** del juego que construyan de forma autónoma.

```
┌────────────────────────────────────────────────────────┐
│                   Cliente Navegador Web                │
│   (dashboard.html: Motor Three.js 3D, Visor, UI)       │
└───────────────────────────┬────────────────────────────┘
                            │ Peticiones HTTP / JSON REST
                            ▼
┌────────────────────────────────────────────────────────┐
│             Servidor HttpServer Embebido (Paper)       │
│   Puerto: 8080 (o puertos de reserva 8081..8089)       │
│   Hilos: 4 Workers Daemon ("MultiverseWeb-Worker")     │
└───────────────────────────┬────────────────────────────┘
                            │
                            ├── Lee la caché de esquemas
                            ├── Valida cuotas de almacenamiento
                            │
                            │ Bukkit.getScheduler().runTask(plugin, ...)
                            ▼
┌────────────────────────────────────────────────────────┐
│               Hilo Principal de Minecraft              │
│   (Máquina de estados de Tortugas, Bloques, Hologramas)│
└────────────────────────────────────────────────────────┘
```

---

## 2. Servidor HTTP Embebido (`WebServerManager.java`)

MultiverseProgramming incorpora el servidor HTTP nativo del JDK `com.sun.net.httpserver.HttpServer`. Esto elimina dependencias pesadas de frameworks externos (Netty, Jetty, Spring) y logra tiempos de respuesta inferiores al milisegundo.

### A. Resolución Automática de Conflictos de Puertos
En servidores compartidos o redes con múltiples instancias, los puertos pueden estar ocupados. `WebServerManager.start()` implementa una búsqueda automática secuencial:
```java
int preferredPort = plugin.getConfigManager().getWebPortalPort(); // ej. 8080
int maxAttempts = 10;

for (int attempt = 0; attempt < maxAttempts; attempt++) {
    int candidatePort = preferredPort + attempt;
    try {
        createdServer = HttpServer.create(new InetSocketAddress(bindAddress, candidatePort), 0);
        boundPort = candidatePort;
        if (attempt > 0) {
            plugin.getLogger().warning("[WebPortal] ¡El puerto configurado " + preferredPort 
                + " estaba en uso! Se cambió automáticamente al puerto alternativo " + boundPort);
        }
        break;
    } catch (BindException e) {
        // Intenta el siguiente puerto disponible hasta candidatePort + 9
    }
}
```

### B. Modelo de Concurrencia y Hilos
- **Pool de Hilos**: Utiliza un `ThreadPoolExecutor` con **4 hilos daemon** nombrados `MultiverseWeb-Worker`.
- **Ejecución Asíncrona**: El procesamiento de peticiones web (decodificación Base64, descompresión Gzip, serialización JSON) ocurre fuera del ciclo de ticks de Minecraft, garantizando 20 TPS sin caídas de rendimiento.
- **Paso al Hilo Principal**: Cuando una petición necesita modificar el mundo (como ordenar construir a una Tortuga), se envía al hilo principal mediante `Bukkit.getScheduler().runTask(plugin, () -> turtle.startBuild(...))`.

### C. Seguridad CORS y Cabeceras
Todos los endpoints implementan cabeceras estándar para permitir peticiones desde cualquier origen (CORS):
- `Access-Control-Allow-Origin: *`
- `Access-Control-Allow-Methods: GET, POST, OPTIONS`
- `Access-Control-Allow-Headers: Content-Type, Authorization`
- Las peticiones preflight `OPTIONS` se responden inmediatamente con HTTP `204 No Content`.

---

## 3. Especificación Completa de la API REST

| Endpoint | Método | Descripción | Cuerpo de Petición | Cuerpo de Respuesta |
| :--- | :---: | :--- | :--- | :--- |
| `/` | `GET` | Sirve el HTML de la SPA del dashboard | Ninguno | Flujo HTML (`text/html`) |
| `/api/blueprints` | `GET` | Lista todos los esquemas registrados | Ninguno | Arreglo de metadatos de esquemas |
| `/api/blueprints/{id}` | `GET` | Obtiene un esquema con sus bloques | Ninguno | Objeto JSON del esquema |
| `/api/upload` | `POST` | Sube un archivo `.litematic` o `.nbt` | JSON con datos en Base64 | `{ ok: true, blueprint: {...} }` |
| `/api/turtles` | `GET` | Lista tortugas activas y su progreso | Ninguno | Arreglo de estados de Tortugas |
| `/api/build` | `POST` | Envía orden de construir a una Tortuga | Parámetros de construcción | `{ ok: true, message: "..." }` |
| `/api/pause` | `POST` | Pausa o reanuda una construcción | `{ turtleId: "T-001" }` | `{ ok: true, status: "..." }` |
| `/api/cancel` | `POST` | Cancela una tarea de construcción | `{ turtleId: "T-001" }` | `{ ok: true }` |
| `/api/quota` | `GET` | Consulta la cuota de un jugador | Parámetro URL: `?player=Nombre` | Desglose de bytes y MB |
| `/api/delete` | `POST` | Elimina un esquema del servidor | `{ blueprintId: "...", player: "..." }` | `{ ok: true }` |

---

## 4. Análisis Detallado de Endpoints Clave

### A. Pipeline de Subida de Esquemas (`POST /api/upload`)
```json
{
  "filename": "torre_castillo.litematic",
  "data": "H4sICDy...",
  "player": "Steve"
}
```
1. **Extracción**: Se analiza el cuerpo JSON y se decodifica la cadena Base64 a `byte[]` en memoria.
2. **Verificación de Cuotas**: `BlueprintManager.getPlayerUsageBytes(player)` verifica si el nuevo archivo supera la cuota máxima por jugador (`blueprint-player-quota-mb`, por defecto 10 MB).
3. **Análisis Binario**: `BlueprintParser.parse(filename, rawBytes)` identifica la cabecera, descomprime el flujo Gzip y construye la instancia inmutable [`Blueprint`](file:///c:/Users/danie/OneDrive/Documentos/Github/Personal/MultiverseProgramming/src/main/java/com/multiverse/programming/blueprint/Blueprint.java).
4. **Almacenamiento en Disco**: El archivo se guarda físicamente en `plugins/MultiverseProgramming/blueprints/` y se indexa en caché.

### B. Despacho Remoto a Tortugas (`POST /api/build`)
```json
{
  "blueprintId": "BP-7A9B",
  "turtleId": "T-001",
  "x": 0,
  "y": 0,
  "z": 0,
  "clear": true,
  "orientation": "EAST"
}
```
1. **Localización de la Tortuga**: Se obtiene la referencia de la tortuga desde `TurtleManager` mediante su identificador único.
2. **Cálculo de Coordenadas Relativas**: Por defecto (`relative: true`), las coordenadas `(x, y, z)` se calculan como desplazamiento relativo desde la posición actual de la tortuga (`targetX = turtleX + x`). Usar `(0, 0, 0)` ancla el plano directamente en la posición de la tortuga.
3. **Cálculo de Rotación**: Se normaliza la orientación recibida ("NORTH", "EAST", "SOUTH", "WEST" o grados) con `BlueprintRotator.normalizeRotation()`.
4. **Ejecución en el Hilo Principal**: Se programa la llamada a `turtle.startBuild()` en el programador de tareas de Bukkit.
5. **Confirmación**: Se retorna HTTP `200 OK` informando al cliente web que la construcción ha iniciado.

---

## 5. Arquitectura del Frontend Web (`dashboard.html`)

La interfaz es una Single-Page Application (SPA) moderna, sin dependencias pesadas de frameworks, empaquetada dentro del archivo JAR en `src/main/resources/dashboard.html`.

### A. Motor de Visualización 3D con Three.js (WebGL)
- **Renderizador**: `THREE.WebGLRenderer` con suavizado de bordes (antialiasing) y codificación sRGB.
- **Paleta de Voxeles**: Un diccionario asigna a cada identificador de bloque de Minecraft (ej. `minecraft:stone`, `minecraft:oak_planks`, `minecraft:water`) su color hexadecimal exacto.
- **Optimización de Geometría**: Para sostener 60 FPS estables con esquemas de gran tamaño, los bloques se renderizan mediante geometrías agrupadas en lugar de mallas individuales independientes.
- **Controles de Órbita**: Permite rotar la cámara arrastrando con el ratón, hacer zoom con la rueda y desplazarse con clic derecho.

### B. Visor de Capas en 2D (Slicer)
- Una barra deslizante vertical permite filtrar los bloques capa por capa ($Y = 0 \dots H$).
- Permite a los jugadores examinar conexiones de redstone, cableado interno o secciones estructurales antes de ordenar la construcción.

### C. Internacionalización Bilingüe (i18n)
- Incluye un botón selector de idioma en la barra superior (`EN / ES`).
- Todos los textos, alertas y botones se actualizan en el DOM al instante sin recargar la página.
