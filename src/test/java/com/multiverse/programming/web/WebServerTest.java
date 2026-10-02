// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.web;

import com.multiverse.programming.BukkitMockHelper;
import com.multiverse.programming.ConfigManager;
import com.multiverse.programming.MultiverseProgrammingPlugin;
import com.multiverse.programming.blueprint.Blueprint;
import com.multiverse.programming.blueprint.BlueprintManager;
import com.multiverse.programming.turtle.Turtle;
import com.multiverse.programming.turtle.TurtleManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebServerTest {

    private MultiverseProgrammingPlugin mockPlugin;
    private WebServerManager webServer;
    private int port;
    private ConfigManager mockConfig;
    private BlueprintManager mockBpManager;
    private Blueprint testBlueprint;

    @BeforeEach
    void setUp() throws IOException {
        BukkitMockHelper.setUpMockServer();
        mockPlugin = mock(MultiverseProgrammingPlugin.class);
        when(mockPlugin.getLogger()).thenReturn(Logger.getLogger("WebServerTest"));

        // Find available port
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }

        mockConfig = mock(ConfigManager.class);
        when(mockConfig.isWebPortalEnabled()).thenReturn(true);
        when(mockConfig.getWebPortalBindAddress()).thenReturn("127.0.0.1");
        when(mockConfig.getWebPortalPort()).thenReturn(port);
        when(mockConfig.getTurtleBuildDelayTicks()).thenReturn(2);
        when(mockConfig.isTurtleRequireMaterials()).thenReturn(false);
        when(mockPlugin.getConfigManager()).thenReturn(mockConfig);

        mockBpManager = mock(BlueprintManager.class);
        Blueprint testBp = new Blueprint(
                "BP-TEST",
                "Test Castle",
                "Tester",
                "LITEMATIC",
                10, 5, 10,
                50,
                Map.of("minecraft:stone", 50),
                List.of(),
                System.currentTimeMillis()
        );
        when(mockBpManager.getAllBlueprints()).thenReturn(List.of(testBp));
        when(mockBpManager.getBlueprint("BP-TEST")).thenReturn(testBp);
        this.testBlueprint = testBp;
        when(mockPlugin.getBlueprintManager()).thenReturn(mockBpManager);

        TurtleManager mockTurtleManager = mock(TurtleManager.class);
        World mockWorld = mock(World.class);
        when(mockWorld.getName()).thenReturn("world");
        Turtle testTurtle = new Turtle(mockPlugin, "T-001", new Location(mockWorld, 100, 64, 200), BlockFace.NORTH, null);
        when(mockTurtleManager.getAllTurtles()).thenReturn(List.of(testTurtle));
        when(mockTurtleManager.getTurtleById("T-001")).thenReturn(testTurtle);
        when(mockPlugin.getTurtleManager()).thenReturn(mockTurtleManager);

        webServer = new WebServerManager(mockPlugin);
        webServer.start();
    }

    @AfterEach
    void tearDown() {
        if (webServer != null) {
            webServer.stop();
        }
    }

    @Test
    @DisplayName("GET / serves the complete dashboard HTML")
    void testGetDashboard() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Multiverse Programming // Blueprint Nexus"));
        assertTrue(response.body().contains("TURTLE MATRIX"));
    }

    @Test
    @DisplayName("GET /api/blueprints returns registered blueprints in JSON")
    void testGetBlueprints() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/blueprints"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("BP-TEST"));
        assertTrue(response.body().contains("Test Castle"));
    }

    @Test
    @DisplayName("POST /api/build is rejected with 403 while remote dispatch is disabled")
    void testBuildDispatchDisabledByDefault() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        String body = "{\"blueprintId\":\"BP-TEST\",\"turtleId\":\"T-001\",\"x\":0,\"y\":0,\"z\":0}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/build"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, response.statusCode());
        assertTrue(response.body().contains("disabled"), "Error body should explain the endpoint is disabled");
    }

    @Test
    @DisplayName("GET /api/turtles returns active online turtles in JSON")
    void testGetTurtles() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/turtles"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("T-001"));
        assertTrue(response.body().contains("world"));
    }

    @Test
    @DisplayName("POST /api/delete never honours a client-supplied Admin/Server identity")
    void testDeleteIgnoresClaimedAdminIdentity() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        String body = "{\"blueprintId\":\"BP-TEST\",\"player\":\"Admin\"}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/delete"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        client.send(request, HttpResponse.BodyHandlers.ofString());

        // The privileged "Admin" identity must be downgraded to a non-privileged guest, so a remote
        // client cannot delete blueprints it does not own by claiming to be an administrator.
        verify(mockBpManager).deleteBlueprint("BP-TEST", "WebGuest");
        verify(mockBpManager, never()).deleteBlueprint(eq("BP-TEST"), eq("Admin"));
    }

    @Test
    @DisplayName("Write endpoints require the access token when one is configured")
    void testUploadRequiresTokenWhenConfigured() throws IOException, InterruptedException {
        when(mockConfig.getWebPortalAccessToken()).thenReturn("s3cret");
        when(mockBpManager.register(anyString(), any(byte[].class), anyString())).thenReturn(testBlueprint);
        HttpClient client = HttpClient.newHttpClient();
        String body = "{\"filename\":\"a.litematic\",\"data\":\"QUJD\",\"player\":\"Bob\"}";

        // No token -> rejected with 401.
        HttpResponse<String> noToken = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + port + "/api/upload"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(401, noToken.statusCode());
        verify(mockBpManager, never()).register(anyString(), any(byte[].class), anyString());

        // Correct token -> passes authorization (no longer 401/403).
        HttpResponse<String> withToken = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + port + "/api/upload"))
                        .header("Content-Type", "application/json")
                        .header("X-MVP-Token", "s3cret")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertNotEquals(401, withToken.statusCode());
        assertNotEquals(403, withToken.statusCode());
    }
}
