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

    @BeforeEach
    void setUp() throws IOException {
        BukkitMockHelper.setUpMockServer();
        mockPlugin = mock(MultiverseProgrammingPlugin.class);
        when(mockPlugin.getLogger()).thenReturn(Logger.getLogger("WebServerTest"));

        // Find available port
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }

        ConfigManager mockConfig = mock(ConfigManager.class);
        when(mockConfig.isWebPortalEnabled()).thenReturn(true);
        when(mockConfig.getWebPortalBindAddress()).thenReturn("127.0.0.1");
        when(mockConfig.getWebPortalPort()).thenReturn(port);
        when(mockConfig.getTurtleBuildDelayTicks()).thenReturn(2);
        when(mockConfig.isTurtleRequireMaterials()).thenReturn(false);
        when(mockPlugin.getConfigManager()).thenReturn(mockConfig);

        BlueprintManager mockBpManager = mock(BlueprintManager.class);
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
}
