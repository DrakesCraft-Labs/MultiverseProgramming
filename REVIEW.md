# Revisión completa — MultiverseProgramming

**Versión revisada:** `1.1.7` · **Rama:** `claude/festive-darwin-du5ypv` · **Fecha:** 2026-10-02
**Alcance:** todo el código fuente (`src/main`, `src/test`), configuración, portal web, workflows de CI y wiki.
**Estado de pruebas:** la suite JUnit pasa localmente (los tests se ejecutan contra mocks de Bukkit; la compilación con Maven en este entorno falla sólo porque el proxy bloquea `repo.papermc.io` con 403, no por el código).

---

## 1. Resumen ejecutivo

El proyecto está **bien estructurado y maduro**: separación clara por paquetes (`blueprint`, `peripheral`, `turtle`, `web`, `protection`), buena cobertura de tests (28 clases de test), parsers NBT/Litematica propios endurecidos contra zip-bombs, y una capa de protección por reflexión que evita dependencias duras con WorldGuard/ProtectionStones/BentoBox. La calidad general del código es alta.

Hay, sin embargo, **tres problemas de seguridad que conviene atender** antes de una publicación amplia (Modrinth), todos en la superficie de red/entrada no confiable, más algunos defectos de corrección/rendimiento de menor gravedad.

| # | Severidad | Área | Resumen |
|---|-----------|------|---------|
| H1 | **Alta** | Blueprints / comandos | SSRF: cualquier jugador puede hacer que el servidor descargue una URL arbitraria |
| H2 | **Alta** | Portal web | Control de acceso roto: `player` auto-declarado permite borrar blueprints ajenos y saltarse la cuota |
| M1 | Media | Sandbox Lua | `load`/`string.dump`/`require` siguen expuestos; el guardia de bytecode es incompleto |
| M2 | Media | Protección | `checkBuildArea` recorre *todos* los bloques con reflexión → posible lag/DoS en el dispatch |
| L1 | Baja | Configuración | `max-concurrent-programs` se lee pero nunca se aplica (knob muerto) |
| L2 | Baja | Portal web | Mensajes de error concatenados a JSON a mano (`PauseHandler`/`CancelHandler`) |
| L3 | Info | Portal web | `docs/index.html` (GitHub Pages) difiere de `dashboard.html` empaquetado |

---

## 2. Hallazgos de seguridad

### H1 — SSRF vía `/mvprog get <url>` (Alta)

`ComputerCommand.handleGetCommand` (línea ~752) pasa el argumento del jugador sin filtrar a
`BlueprintManager.getOrDownloadBlueprint`, que acepta directamente cualquier `http://` o `https://`:

```java
// BlueprintManager.getOrDownloadBlueprint
if (target.startsWith("http://") || target.startsWith("https://")) {
    candidateUrls.add(target);           // ← URL arbitraria, sin allowlist
}
```

El comando `get` requiere sólo `multiverseprogramming.use`, que es **`default: true`** (todos los jugadores). Un jugador puede forzar al servidor a emitir peticiones GET a destinos internos (p. ej. endpoints de metadatos de la nube, servicios en `127.0.0.1`/`10.x`, escaneo de puertos por *timing*). Como la respuesta se parsea como NBT, la exfiltración directa es limitada, pero es un **SSRF ciego** explotable. Además se permite `http://` en texto plano hacia servicios internos.

**Recomendación:**
- Validar el host contra una *allowlist* (p. ej. `bytebin.lucko.me`, `raw.githubusercontent.com`) o rechazar IPs privadas/loopback/link-local/metadatos resolviendo el host antes de conectar.
- Limitar `get` directo por URL a `multiverseprogramming.admin`, dejando a los jugadores sólo claves de pastebin / IDs `BP-*`.

### H2 — Control de acceso roto en el portal web (Alta)

`/api/upload`, `/api/delete` y `/api/quota` **no tienen autenticación** y confían en un campo `player` enviado por el cliente. El *bind* por defecto es `0.0.0.0:8080`.

- **Borrado de blueprints ajenos:** `DeleteHandler` llama a `deleteBlueprint(blueprintId, player)` donde `player` viene del JSON. En `BlueprintManager.deleteBlueprint`:
  ```java
  boolean isAdmin = playerOrAdmin.equalsIgnoreCase("Server") || playerOrAdmin.equalsIgnoreCase("Admin");
  ```
  Cualquiera que alcance el puerto puede `POST /api/delete {"blueprintId":"BP-XXXX","player":"Admin"}` y borrar **cualquier** blueprint.
- **Evasión de cuota / suplantación:** en `UploadHandler`, enviar `"player":"Server"` (o `"Admin"`) salta por completo el límite de 15 MB (`register` exime a `Server`/`Admin`), y se puede subir contenido imputado a cualquier nombre de jugador.

Nota positiva: los endpoints que *controlan tortugas* (`/api/build`, `/api/pause`, `/api/cancel`) **sí** están protegidos por `web-portal-remote-dispatch: false` por defecto — buena decisión. El problema es que upload/delete/quota no tienen equivalente.

**Recomendación:**
- No tratar `player` del cliente como identidad. Emitir un token por jugador (p. ej. `/mvprog web` entrega un enlace con token de un solo uso) y validar propiedad/cuota contra ese token en el servidor.
- Como mínimo, documentar que `delete`/`upload` deben restringirse a `127.0.0.1` y nunca confiar en el valor `Admin`/`Server` recibido por red; ignorar esos valores especiales cuando provienen de HTTP.

### M1 — Endurecimiento del sandbox Lua incompleto (Media)

`LuaRunner.sandbox` anula `io`, `os`, `luajava`, `package`, `dofile`, `loadfile`, `debug`, lo cual es correcto. Pero quedan accesibles:

- `load` y `string.dump`: permiten cargar *bytecode* arbitrario desde una cadena. El guardia `validate()`/`execute()` sólo rechaza programas que **empiezan** con el byte ESC (`\033`), no bytecode cargado dinámicamente vía `load(string.dump(...))` dentro del programa. El verificador de bytecode de luaj es débil y se conoce que puede provocar fallos de la VM.
- `require`: aunque `luajava` esté a NIL, dejar `require` abierto amplía la superficie.

No pude demostrar RCE completo en este entorno (el `standardGlobals` de luaj no enlaza `luajava` por sí mismo), pero es un endurecimiento recomendable para no depender de ese detalle.

**Recomendación:** anular o envolver `load`/`loadstring` para forzar modo texto (`"t"`, nunca binario) y considerar anular `require`. Alternativamente, migrar a un `load` propio que rechace fuentes que no sean texto imprimible.

---

## 3. Corrección y rendimiento

### M2 — `ProtectionManager.checkBuildArea` recorre todos los bloques (Media)

```java
for (Blueprint.PlacementBlock block : blueprint.blocks()) {   // hasta 250k–500k
    Location checkLoc = origin.clone().add(block.x(), block.y(), block.z());
    if (!canBuildAt(playerUuid, checkLoc)) { ... }
}
```

`canBuildAt` hace llamadas por reflexión a hasta tres plugins **por bloque**. Para un blueprint grande son cientos de miles de llamadas reflexivas, ejecutadas en el hilo que despacha el *build*. Es un vector de lag/DoS (basta despachar un blueprint grande dentro de una región protegida).

**Recomendación:** muestrear la caja envolvente como ya hace `checkRegionArea` (8 esquinas + centro), o cachear por *chunk*/región el resultado de `canBuildAt`.

### L1 — `max-concurrent-programs` nunca se aplica (Baja)

`ConfigManager.getMaxConcurrentPrograms()` no se invoca en ningún punto del código (`grep` confirma 0 usos fuera del propio getter). El pool de `LuaRunner` está fijo en código (core 4, máx 32) con `CallerRunsPolicy`. El ajuste de `config.yml` es engañoso: no hace nada.

**Recomendación:** aplicar el límite con un `Semaphore` al enviar al pool, o eliminar la opción de `config.yml` y la documentación asociada.

### L2 — JSON construido a mano con mensajes de excepción (Baja)

En `PauseHandler` y `CancelHandler`:
```java
sendJsonResponse(exchange, 400, "{\"ok\":false,\"error\":\"" + e.getMessage() + "\"}");
```
Si `e.getMessage()` contiene comillas o saltos de línea, rompe el JSON (o permite inyección en el campo). El resto del código usa `gson` correctamente; conviene unificar aquí también.

### L3 — `docs/index.html` divergente (Info)

`docs/index.html` (portal público en GitHub Pages) **difiere** de `src/main/resources/dashboard.html` (el servido por el plugin). Riesgo de que el portal público quede desincronizado con el del servidor. Conviene generar `docs/index.html` desde la misma fuente en CI, o documentar la divergencia intencional.

---

## 4. Aspectos positivos

- **Parsers NBT/Litematica propios** con mitigación de zip-bomb (`BoundedInputStream`, `MAX_UNCOMPRESSED_BYTES`, `validateArrayLength`) — muy sólido.
- **`BlueprintSecurityValidator`** con lista de bloques peligrosos (command blocks, bedrock, spawners, vaults…) y tope absoluto de bloques independiente del config.
- **Dispatch remoto de tortugas desactivado por defecto**, con aviso de seguridad claro en `config.yml`.
- **Dashboard web**: los sinks de `innerHTML` relevantes (nombre y materiales del blueprint) pasan por `escapeHtml`; el autor se renderiza con `textContent`. No encontré XSS explotable con los datos actuales.
- **Deduplicación SHA-256**, cuotas por jugador y limpieza por retención.
- **Sandbox Lua** con *hook* de interrupción cooperativa y límite de instrucciones — buena base.
- Buena **cobertura de tests** y commits descriptivos.

---

## 5. Prioridad sugerida

1. **H2** (borrado/cuota sin auth) y **H1** (SSRF) — antes de exponer el portal o publicar en Modrinth.
2. **M2** (lag por protección) — afecta a la jugabilidad en servidores con regiones.
3. **M1** (sandbox) — endurecimiento preventivo.
4. **L1–L3** — limpieza y consistencia.

---

## 6. Correcciones aplicadas (en esta rama)

Todos los hallazgos anteriores se han corregido:

- **H1 (SSRF):** `BlueprintManager.validateUrlAllowed` rechaza URLs no http(s) y destinos no públicos (loopback, link-local, RFC1918, CGNAT `100.64/10`, ULA IPv6, metadatos de nube) resolviendo el host antes de conectar; se valida cada URL candidata antes de descargar. Además, `/mvprog get <url>` por URL directa queda restringido a `multiverseprogramming.admin` (los jugadores siguen pudiendo usar códigos de pastebin e IDs `BP-*`).
- **H2 (control de acceso web):** el portal nunca confía en una identidad `Server`/`Admin` recibida por HTTP (`sanitizeWebIdentity` la degrada a `WebGuest`), cerrando el borrado de blueprints ajenos y la evasión de cuota. Nuevo `web-portal-access-token` opcional protege `/api/upload` y `/api/delete` (cabecera `X-MVP-Token` o campo `token`), con comparación en tiempo constante.
- **M1 (sandbox Lua):** `require` se anula; `load`/`loadstring` se envuelven para rechazar *bytecode* precompilado (sólo fuente de texto).
- **M2 (rendimiento protección):** `checkBuildArea` ahora muestrea la caja envolvente (8 esquinas + centro) en vez de recorrer todos los bloques.
- **L1 (concurrencia):** `max-concurrent-programs` se aplica con un `Semaphore` en `LuaRunner` (configurado al habilitar y recargar); al superar el límite el programa se rechaza con un mensaje claro.
- **L2 (JSON):** `PauseHandler`/`CancelHandler` usan Gson para los errores (mensajes correctamente escapados).
- **L3 (portal público):** `docs/index.html` sincronizado con la lista de bloques peligrosos de `dashboard.html`.

Se añadieron pruebas: guard SSRF (`BlueprintManagerTest`), `require`/`load`/concurrencia (`LuaRunnerTest`) y neutralización de identidad + token (`WebServerTest`).

> **Nota de verificación:** en este entorno sandbox la compilación completa con Maven no puede ejecutarse porque la política de egreso bloquea `repo.papermc.io` (403), de donde proviene `paper-api`. La lógica nueva de `LuaRunner` (API de luaj) y del guard SSRF se validó con compilaciones aisladas; el resto se valida en CI (GitHub Actions / workflow de Modrinth).

## 7. Segunda pasada — hallazgos y correcciones adicionales

- **Acceso asíncrono al mundo en `PeripheralManager.findPeripherals` (Media):** los programas Lua de los computadores *estándar* se ejecutan en un hilo asíncrono (`ComputerGUI.pressButton` → `runTaskAsynchronously` → `LuaRunner.execute` → `bindAll` → `findPeripherals`), y el descubrimiento de periféricos leía tipos/estados de bloques adyacentes **fuera del hilo principal**, lo que en Paper es acceso asíncrono inseguro al mundo. *Corregido:* `findPeripherals` ahora envuelve el descubrimiento en `SyncDispatcher.sync` (se ejecuta en línea si ya está en el hilo principal, como en los computadores avanzados y las tortugas). Las operaciones de los periféricos ya usaban `SyncDispatcher`, así que sólo faltaba el descubrimiento.
- **Amplificación de memoria en `NbtReader` (Baja/endurecimiento):** una `TAG_List` que declarara tipo de elemento `TAG_End` (que no consume bytes por entrada) con una longitud grande podía asignar millones de objetos sin disparar el límite de bytes descomprimidos. *Corregido:* se rechaza una lista de `TAG_End` con longitud distinta de cero (una lista de `End` sólo es válida vacía).

Verificado: ambos cambios compilan de forma aislada; el guard de `NbtReader` lanza con longitud>0 y acepta la lista vacía. Pruebas añadidas en `NbtReaderTest`.

Áreas revisadas sin incidencias: ciclo de vida del plugin (`onDisable` cancela web, tortugas, programas, recetas y el pool), cancelación de programas (`cancelAll`/`stopProgramAt`/`stopProgramsByPlayer` cancelan programa + tarea de limpieza), `getProgressPercentage` (protegido contra división por cero), parseo de IDs en `TurtleManager` (con `try/catch`) y la propagación de errores de `SyncDispatcher`.

## 8. Tercera pasada — tortuga (construcción, movimiento, combustible)

- **Pérdida de materiales en el loop de construcción (Media):** en `Turtle.startBuild`, el material se consumía *antes* de comprobar que la Y estuviera dentro del mundo y que el chunk estuviera cargado. En el camino "chunk no cargado" se hacía `return` sin incrementar el índice, de modo que el mismo bloque se reintentaba cada tick **consumiendo material repetidamente**; y en el "Y fuera de rango" se saltaba el bloque con el material ya gastado. *Corregido:* el consumo se hace ahora tras pasar ambas comprobaciones, justo antes de colocar (el bloque está garantizado a colocarse ese tick). Nota menor pendiente: si el bloque ya coincide (reanudación), se consume igualmente 1 ítem; es un desperdicio menor preexistente, no una pérdida en bucle.
- **Combustible no consumido en movimiento/dig normal (Media, funcional):** `turtle-fuel-required` y las reservas de combustible sólo se usaban en el motor de cantera (*quarry*); `forward/back/up/down` y `dig` nunca gastaban ni verificaban combustible, pese a lo que prometen el README y `config.yml`. *Corregido:* con `turtle-fuel-required: true`, cada movimiento y cada excavación consumen 1 de combustible y se rechazan si la tortuga está sin combustible (el motor de cantera mantiene su consumo 1.2× aparte). Tests añadidos en `TurtleTest`.

Áreas de la tortuga revisadas sin incidencias: `dig`/`place` no duplican ítems (usan `getDrops()` + `setType` sin drop natural), el consumo de combustible del *quarry* (1.2×) es correcto, el refuelado (incluido `LAVA_BUCKET` → `BUCKET`) cuadra, y `/mvprog build` valida las coordenadas (`isCoordinateToken`) antes de parsearlas.
