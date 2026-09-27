// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.turtle;

import com.multiverse.programming.BukkitMockHelper;
import com.multiverse.programming.ConfigManager;
import com.multiverse.programming.MultiverseProgrammingPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TurtleListenerTest {

    private MultiverseProgrammingPlugin mockPlugin;
    private TurtleManager mockTurtleManager;
    private ConfigManager mockConfigManager;
    private TurtleListener listener;
    private World mockWorld;

    @BeforeEach
    void setUp() {
        BukkitMockHelper.setUpMockServer();
        mockPlugin = mock(MultiverseProgrammingPlugin.class);
        when(mockPlugin.getPrefix()).thenReturn("§8[§bMVP§8]§r");

        mockTurtleManager = mock(TurtleManager.class);
        when(mockPlugin.getTurtleManager()).thenReturn(mockTurtleManager);

        mockConfigManager = mock(ConfigManager.class);
        when(mockConfigManager.getTurtleBlock()).thenReturn(Material.DISPENSER);
        when(mockConfigManager.isEnableTurtle()).thenReturn(true);
        when(mockPlugin.getConfigManager()).thenReturn(mockConfigManager);

        mockWorld = mock(World.class);

        listener = new TurtleListener(mockPlugin);
    }

    @AfterEach
    void tearDown() {
        BukkitMockHelper.tearDownMockServer();
    }

    @Test
    @DisplayName("Non-sneaking player is prevented from breaking protected block with explanation message")
    void testBlockBreakProtectedNonSneaking() {
        Location loc = new Location(mockWorld, 10, 64, 10);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(loc);
        when(block.getType()).thenReturn(Material.STONE);

        Player player = mock(Player.class);
        when(player.isSneaking()).thenReturn(false);
        when(player.getLocation()).thenReturn(loc);

        when(mockTurtleManager.checkBlockProtection(loc))
                .thenReturn(new Turtle.BlockProtectionCheck(true, "forma parte de la construcción activa de la Turtle T-001."));

        BlockBreakEvent event = new BlockBreakEvent(block, player);
        listener.onBlockBreak(event);

        assertTrue(event.isCancelled(), "Event must be cancelled when non-sneaking player breaks a protected block");
        verify(player).sendMessage(contains("No puedes destruir este bloque porque forma parte de la construcción activa"));
        verify(player).sendMessage(contains("Shift"));
    }

    @Test
    @DisplayName("Sneaking player can break protected block")
    void testBlockBreakProtectedSneaking() {
        Location loc = new Location(mockWorld, 10, 64, 10);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(loc);
        when(block.getType()).thenReturn(Material.STONE);

        Player player = mock(Player.class);
        when(player.isSneaking()).thenReturn(true);
        when(player.getLocation()).thenReturn(loc);

        when(mockTurtleManager.checkBlockProtection(loc))
                .thenReturn(new Turtle.BlockProtectionCheck(true, "forma parte de la construcción activa de la Turtle T-001."));

        BlockBreakEvent event = new BlockBreakEvent(block, player);
        listener.onBlockBreak(event);

        assertFalse(event.isCancelled(), "Event must NOT be cancelled when sneaking player forces destruction");
        verify(player, never()).sendMessage(contains("No puedes destruir"));
    }

    @Test
    @DisplayName("Block is broken normally without sneaking when no active protection exists")
    void testBlockBreakUnprotectedNormal() {
        Location loc = new Location(mockWorld, 10, 64, 10);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(loc);
        when(block.getType()).thenReturn(Material.STONE);

        Player player = mock(Player.class);
        when(player.isSneaking()).thenReturn(false);
        when(player.getLocation()).thenReturn(loc);

        when(mockTurtleManager.checkBlockProtection(loc)).thenReturn(null);

        BlockBreakEvent event = new BlockBreakEvent(block, player);
        listener.onBlockBreak(event);

        assertFalse(event.isCancelled(), "Unprotected blocks should be broken normally without sneaking");
    }

    @Test
    @DisplayName("Explosions filter out protected blocks")
    void testExplosionsFilterProtectedBlocks() {
        Location protLoc = new Location(mockWorld, 10, 64, 10);
        Block protBlock = mock(Block.class);
        when(protBlock.getLocation()).thenReturn(protLoc);

        Location freeLoc = new Location(mockWorld, 20, 64, 20);
        Block freeBlock = mock(Block.class);
        when(freeBlock.getLocation()).thenReturn(freeLoc);

        when(mockTurtleManager.checkBlockProtection(protLoc))
                .thenReturn(new Turtle.BlockProtectionCheck(true, "en uso"));
        when(mockTurtleManager.checkBlockProtection(freeLoc))
                .thenReturn(null);

        // EntityExplodeEvent
        List<Block> entityBlocks = new ArrayList<>(List.of(protBlock, freeBlock));
        EntityExplodeEvent entityExplode = mock(EntityExplodeEvent.class);
        when(entityExplode.blockList()).thenReturn(entityBlocks);
        listener.onEntityExplode(entityExplode);

        assertEquals(1, entityBlocks.size());
        assertEquals(freeBlock, entityBlocks.get(0));

        // BlockExplodeEvent
        List<Block> blockBlocks = new ArrayList<>(List.of(protBlock, freeBlock));
        BlockExplodeEvent blockExplode = mock(BlockExplodeEvent.class);
        when(blockExplode.blockList()).thenReturn(blockBlocks);
        listener.onBlockExplode(blockExplode);

        assertEquals(1, blockBlocks.size());
        assertEquals(freeBlock, blockBlocks.get(0));
    }
}
