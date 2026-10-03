// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.web;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.multiverse.programming.MultiverseProgrammingPlugin;
import com.multiverse.programming.blueprint.Blueprint;
import com.multiverse.programming.blueprint.BlueprintParser;
import com.multiverse.programming.blueprint.BlueprintRotator;
import com.multiverse.programming.turtle.Turtle;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;

/**
 * Embedded HTTP server providing the web portal for uploading .litematic and .nbt blueprints,
 * visual previewing, and live dispatch of construction jobs to in-game Turtles.
 */
public final class WebServerManager {

    private final MultiverseProgrammingPlugin plugin;
    private final Gson gson = new Gson();
    private HttpServer server;
    private ThreadPoolExecutor executor;
    private int activePort = -1;

    public WebServerManager(MultiverseProgrammingPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
    }

    public synchronized boolean isRunning() {
        return server != null;
    }

    public synchronized int getActivePort() {
        return activePort;
    }

    public synchronized void start() {
        if (!plugin.getConfigManager().isWebPortalEnabled()) {
            plugin.getLogger().info("[WebPortal] Web portal is disabled in configuration.");
            return;
        }

        String bindAddress = plugin.getConfigManager().getWebPortalBindAddress();
        int preferredPort = plugin.getConfigManager().getWebPortalPort();
        int maxAttempts = 10;
        HttpServer createdServer = null;
        int boundPort = -1;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int candidatePort = preferredPort + attempt;
            try {
                createdServer = HttpServer.create(new InetSocketAddress(bindAddress, candidatePort), 0);
                boundPort = candidatePort;
                if (attempt > 0) {
                    plugin.getLogger().warning("[WebPortal] Configured port " + preferredPort
                            + " was already in use! Automatically switched to fallback port " + boundPort
                            + ". (You can change 'web-portal-port' in config.yml)");
                }
                break;
            } catch (BindException e) {
                if (attempt == maxAttempts - 1) {
                    plugin.getLogger().severe("[WebPortal] Failed to start HTTP server: Ports "
                            + preferredPort + " to " + candidatePort
                            + " are already in use by another application or server instance. "
                            + "Please configure a free port under 'web-portal-port' in config.yml.");
                    return;
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "[WebPortal] Failed to initialize HTTP server on port " + candidatePort, e);
                return;
            }
        }

        if (createdServer == null) {
            return;
        }

        this.server = createdServer;
        this.activePort = boundPort;

        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "MultiverseWeb-Worker");
            t.setDaemon(true);
            return t;
        });
        server.setExecutor(executor);

        // Register Route Handlers
        server.createContext("/", new DashboardHandler());
        server.createContext("/api/blueprints", new BlueprintsHandler());
        server.createContext("/api/upload", new UploadHandler());
        server.createContext("/api/turtles", new TurtlesHandler());
        server.createContext("/api/build", new BuildHandler());
        server.createContext("/api/pause", new PauseHandler());
        server.createContext("/api/cancel", new CancelHandler());
        server.createContext("/api/quota", new QuotaHandler());
        server.createContext("/api/delete", new DeleteHandler());

        server.start();
        plugin.getLogger().info("[WebPortal] Web server successfully running on " + bindAddress + ":" + activePort);
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        activePort = -1;
        plugin.getLogger().info("[WebPortal] Web server stopped.");
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String contentType, byte[] data) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        exchange.sendResponseHeaders(statusCode, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
        exchange.close();
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        sendResponse(exchange, statusCode, "application/json; charset=utf-8", json.getBytes(StandardCharsets.UTF_8));
    }

    /** Builds a safe {@code {"ok":false,"error":...}} payload with the message properly JSON-escaped. */
    private String errorJson(Exception e) {
        JsonObject err = new JsonObject();
        err.addProperty("ok", false);
        err.addProperty("error", e.getMessage() != null ? e.getMessage() : e.toString());
        return gson.toJson(err);
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Normalises a player identity supplied by an untrusted web client. The reserved privileged
     * identities ("Server"/"Admin") can never be asserted over HTTP, so a remote client cannot
     * escalate to delete other players' blueprints or bypass storage quotas. Blank names collapse
     * to a single shared, non-privileged guest identity.
     */
    private static String sanitizeWebIdentity(String raw) {
        if (raw == null || raw.isBlank()) {
            return "WebGuest";
        }
        String trimmed = raw.trim();
        if (trimmed.equalsIgnoreCase("Server") || trimmed.equalsIgnoreCase("Admin")) {
            return "WebGuest";
        }
        return trimmed;
    }

    /**
     * Enforces the optional write token. When {@code web-portal-access-token} is set, upload and
     * delete requests must present it via the {@code X-MVP-Token} header or a {@code token} JSON
     * field. Returns true when the request may proceed; otherwise writes a 401 and returns false.
     */
    private boolean authorizeWrite(HttpExchange exchange, JsonObject body) throws IOException {
        String required = plugin.getConfigManager().getWebPortalAccessToken();
        if (required == null || required.isBlank()) {
            return true;
        }
        String provided = exchange.getRequestHeaders().getFirst("X-MVP-Token");
        if ((provided == null || provided.isBlank()) && body != null && body.has("token")) {
            provided = body.get("token").getAsString();
        }
        if (provided != null && constantTimeEquals(provided.trim(), required)) {
            return true;
        }
        sendJsonResponse(exchange, 401,
                "{\"ok\":false,\"error\":\"Missing or invalid access token for this operation.\"}");
        return false;
    }

    private static boolean constantTimeEquals(String a, String b) {
        return java.security.MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    // =========================================================================
    // Handlers
    // =========================================================================

    private static class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 204, "text/plain", new byte[0]);
                return;
            }
            byte[] htmlBytes = WebDashboardHtml.DASHBOARD_HTML.getBytes(StandardCharsets.UTF_8);
            sendResponse(exchange, 200, "text/html; charset=utf-8", htmlBytes);
        }
    }

    private class BlueprintsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 204, "text/plain", new byte[0]);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            // Path can be /api/blueprints or /api/blueprints/{id}
            if (path.length() > "/api/blueprints/".length()) {
                String bpId = path.substring("/api/blueprints/".length()).trim();
                Blueprint bp = plugin.getBlueprintManager().getBlueprint(bpId);
                if (bp == null) {
                    sendJsonResponse(exchange, 404, "{\"error\":\"Blueprint not found\"}");
                    return;
                }
                sendJsonResponse(exchange, 200, gson.toJson(bp));
                return;
            }

            // List all blueprints
            JsonArray arr = new JsonArray();
            for (Blueprint bp : plugin.getBlueprintManager().getAllBlueprints()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", bp.id());
                obj.addProperty("name", bp.name());
                obj.addProperty("author", bp.author() != null ? bp.author() : "Unknown");
                obj.addProperty("format", bp.format());
                obj.addProperty("sizeX", bp.sizeX());
                obj.addProperty("sizeY", bp.sizeY());
                obj.addProperty("sizeZ", bp.sizeZ());
                obj.addProperty("totalBlocks", bp.totalBlocks());

                JsonObject mats = new JsonObject();
                for (Map.Entry<String, Integer> e : bp.materialCounts().entrySet()) {
                    mats.addProperty(e.getKey(), e.getValue());
                }
                obj.add("materialCounts", mats);
                obj.addProperty("owner", plugin.getBlueprintManager().getOwner(bp.id()));
                arr.add(obj);
            }
            sendJsonResponse(exchange, 200, gson.toJson(arr));
        }
    }

    private class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 204, "text/plain", new byte[0]);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                String body = readBody(exchange);
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                if (!authorizeWrite(exchange, json)) {
                    return;
                }
                String filename = json.get("filename").getAsString();
                String base64Data = json.get("data").getAsString();
                String owner = sanitizeWebIdentity(json.has("player") ? json.get("player").getAsString() : null);

                byte[] rawBytes = Base64.getDecoder().decode(base64Data);
                Blueprint bp = plugin.getBlueprintManager().register(filename, rawBytes, owner);

                JsonObject resp = new JsonObject();
                resp.addProperty("ok", true);
                resp.add("blueprint", gson.toJsonTree(bp));

                sendJsonResponse(exchange, 200, gson.toJson(resp));
                plugin.getLogger().info("[WebPortal] New blueprint uploaded by " + owner + ": " + bp.name() + " (" + bp.id() + ")");
            } catch (Exception e) {
                JsonObject err = new JsonObject();
                err.addProperty("ok", false);
                err.addProperty("error", e.getMessage() != null ? e.getMessage() : e.toString());
                sendJsonResponse(exchange, 400, gson.toJson(err));
            }
        }
    }

    private class TurtlesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 204, "text/plain", new byte[0]);
                return;
            }

            JsonArray arr = new JsonArray();
            try {
                if (plugin.getTurtleManager() != null) {
                    for (Turtle t : plugin.getTurtleManager().getAllTurtles()) {
                        JsonObject obj = new JsonObject();
                        obj.addProperty("id", t.getId());
                        Location loc = t.getLocation();
                        obj.addProperty("world", loc.getWorld() != null ? loc.getWorld().getName() : "unknown");
                        obj.addProperty("x", loc.getBlockX());
                        obj.addProperty("y", loc.getBlockY());
                        obj.addProperty("z", loc.getBlockZ());
                        obj.addProperty("facing", t.getFacing().name());
                        obj.addProperty("fuel", t.getFuel());
                        obj.addProperty("status", t.getStatus().name());
                        obj.addProperty("statusMessage", t.getStatusMessage());

                        JsonObject prog = new JsonObject();
                        prog.addProperty("active", t.getStatus() == Turtle.Status.BUILDING || t.getStatus() == Turtle.Status.PAUSED);
                        prog.addProperty("blueprintId", t.getActiveBlueprintId() != null ? t.getActiveBlueprintId() : "");
                        prog.addProperty("current", t.getCurrentBlockIndex());
                        prog.addProperty("total", t.getTotalBlocks());
                        prog.addProperty("percentage", t.getProgressPercentage());
                        obj.add("progress", prog);

                        arr.add(obj);
                    }
                }
            } catch (RuntimeException e) {
                // Without this the HttpServer silently drops the connection and the portal only
                // sees an empty reply; answer with the cause instead.
                plugin.getLogger().log(Level.WARNING, "[WebPortal] Failed to list turtles", e);
                sendJsonResponse(exchange, 500, errorJson(e));
                return;
            }
            sendJsonResponse(exchange, 200, gson.toJson(arr));
        }
    }

    /** Response returned when remote dispatch is disabled (the secure default). */
    private static final String REMOTE_DISPATCH_DISABLED_JSON =
            "{\"ok\":false,\"error\":\"Remote build dispatch is disabled on this server. "
                    + "Set 'web-portal-remote-dispatch: true' in config.yml to enable it.\"}";

    private class BuildHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 204, "text/plain", new byte[0]);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            if (!plugin.getConfigManager().isWebPortalRemoteDispatch()) {
                sendJsonResponse(exchange, 403, REMOTE_DISPATCH_DISABLED_JSON);
                return;
            }

            try {
                String body = readBody(exchange);
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                String bpId = json.get("blueprintId").getAsString();
                String turtleId = json.get("turtleId").getAsString();
                int x = json.get("x").getAsInt();
                int y = json.get("y").getAsInt();
                int z = json.get("z").getAsInt();

                Blueprint bp = plugin.getBlueprintManager().getBlueprint(bpId);
                if (bp == null) {
                    sendJsonResponse(exchange, 404, "{\"ok\":false,\"error\":\"Blueprint not found\"}");
                    return;
                }

                Turtle turtle = plugin.getTurtleManager().getTurtleById(turtleId);
                if (turtle == null) {
                    sendJsonResponse(exchange, 404, "{\"ok\":false,\"error\":\"Turtle not found: " + turtleId + "\"}");
                    return;
                }

                World world = turtle.getLocation().getWorld();
                if (world == null) {
                    sendJsonResponse(exchange, 400, "{\"ok\":false,\"error\":\"Turtle world is not loaded\"}");
                    return;
                }

                boolean isRelative = !json.has("relative") || json.get("relative").getAsBoolean();
                int targetX = isRelative ? turtle.getLocation().getBlockX() + x : x;
                int targetY = isRelative ? turtle.getLocation().getBlockY() + y : y;
                int targetZ = isRelative ? turtle.getLocation().getBlockZ() + z : z;

                Location targetOrigin = new Location(world, targetX, targetY, targetZ);
                int delay = plugin.getConfigManager().getTurtleBuildDelayTicks();
                boolean requireMaterials = plugin.getConfigManager().isTurtleRequireMaterials();

                boolean clear = json.has("clear") && json.get("clear").getAsBoolean();
                int rotationDegrees = 0;
                if (json.has("orientation")) {
                    rotationDegrees = BlueprintRotator.normalizeRotation(json.get("orientation").getAsString());
                } else if (json.has("rotation")) {
                    rotationDegrees = json.get("rotation").getAsInt();
                } else if (json.has("facing")) {
                    rotationDegrees = BlueprintRotator.normalizeRotation(json.get("facing").getAsString());
                }

                final int finalRotation = rotationDegrees;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    turtle.startBuild(bp, targetOrigin, delay, requireMaterials, clear, finalRotation,
                            () -> plugin.getLogger().info("[Turtle " + turtle.getId() + "] Build finished for " + bp.name()),
                            err -> plugin.getLogger().warning("[Turtle " + turtle.getId() + "] Build error: " + err)
                    );
                });

                JsonObject resp = new JsonObject();
                resp.addProperty("ok", true);
                resp.addProperty("message", "Build dispatched to " + turtle.getId() + " at " + targetX + "," + targetY + "," + targetZ + " (relative: " + x + "," + y + "," + z + ")");
                sendJsonResponse(exchange, 200, gson.toJson(resp));
            } catch (Exception e) {
                JsonObject err = new JsonObject();
                err.addProperty("ok", false);
                err.addProperty("error", e.getMessage() != null ? e.getMessage() : e.toString());
                sendJsonResponse(exchange, 400, gson.toJson(err));
            }
        }
    }

    private class PauseHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 204, "text/plain", new byte[0]);
                return;
            }

            try {
                String body = readBody(exchange);
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                if (!plugin.getConfigManager().isWebPortalRemoteDispatch()) {
                    sendJsonResponse(exchange, 403, REMOTE_DISPATCH_DISABLED_JSON);
                    return;
                }

                String turtleId = json.get("turtleId").getAsString();
                Turtle turtle = plugin.getTurtleManager().getTurtleById(turtleId);
                if (turtle == null) {
                    sendJsonResponse(exchange, 404, "{\"ok\":false,\"error\":\"Turtle not found\"}");
                    return;
                }

                if (turtle.getStatus() == Turtle.Status.BUILDING) {
                    turtle.pauseBuild();
                } else if (turtle.getStatus() == Turtle.Status.PAUSED) {
                    turtle.resumeBuild();
                }

                JsonObject resp = new JsonObject();
                resp.addProperty("ok", true);
                resp.addProperty("status", turtle.getStatus().name());
                sendJsonResponse(exchange, 200, gson.toJson(resp));
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, errorJson(e));
            }
        }
    }

    private class CancelHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 204, "text/plain", new byte[0]);
                return;
            }

            try {
                String body = readBody(exchange);
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                if (!plugin.getConfigManager().isWebPortalRemoteDispatch()) {
                    sendJsonResponse(exchange, 403, REMOTE_DISPATCH_DISABLED_JSON);
                    return;
                }

                String turtleId = json.get("turtleId").getAsString();
                Turtle turtle = plugin.getTurtleManager().getTurtleById(turtleId);
                if (turtle == null) {
                    sendJsonResponse(exchange, 404, "{\"ok\":false,\"error\":\"Turtle not found\"}");
                    return;
                }

                turtle.cancelBuild();
                JsonObject resp = new JsonObject();
                resp.addProperty("ok", true);
                sendJsonResponse(exchange, 200, gson.toJson(resp));
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, errorJson(e));
            }
        }
    }

    private class QuotaHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 204, "text/plain", new byte[0]);
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            String player = null;
            if (query != null && query.contains("player=")) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length == 2 && "player".equalsIgnoreCase(pair[0])) {
                        player = pair[1].trim();
                        break;
                    }
                }
            }
            player = sanitizeWebIdentity(player);

            long usedBytes = plugin.getBlueprintManager().getPlayerUsageBytes(player);
            double quotaMb = plugin.getConfigManager().getBlueprintPlayerQuotaMb();
            long quotaBytes = (long) (quotaMb * 1024 * 1024);

            JsonObject resp = new JsonObject();
            resp.addProperty("player", player);
            resp.addProperty("usedBytes", usedBytes);
            resp.addProperty("usedMb", Math.round((usedBytes / (1024.0 * 1024.0)) * 100.0) / 100.0);
            resp.addProperty("quotaMb", quotaMb);
            resp.addProperty("remainingBytes", Math.max(0, quotaBytes - usedBytes));
            resp.addProperty("remainingMb", Math.max(0, Math.round(((quotaBytes - usedBytes) / (1024.0 * 1024.0)) * 100.0) / 100.0));

            sendJsonResponse(exchange, 200, gson.toJson(resp));
        }
    }

    private class DeleteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 204, "text/plain", new byte[0]);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                String body = readBody(exchange);
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                if (!authorizeWrite(exchange, json)) {
                    return;
                }
                String blueprintId = json.get("blueprintId").getAsString();
                String player = sanitizeWebIdentity(json.has("player") ? json.get("player").getAsString() : null);

                boolean deleted = plugin.getBlueprintManager().deleteBlueprint(blueprintId, player);
                JsonObject resp = new JsonObject();
                resp.addProperty("ok", deleted);
                sendJsonResponse(exchange, deleted ? 200 : 404, gson.toJson(resp));
            } catch (Exception e) {
                JsonObject err = new JsonObject();
                err.addProperty("ok", false);
                err.addProperty("error", e.getMessage());
                sendJsonResponse(exchange, 400, gson.toJson(err));
            }
        }
    }
}
