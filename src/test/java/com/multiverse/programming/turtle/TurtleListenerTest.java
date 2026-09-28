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
import org.bukkit.inventory.Inventory;
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
                .thenReturn(new Turtle.BlockProtectionCheck(true, "it is part of an active construction by Turtle T-001."));

        BlockBreakEvent event = new BlockBreakEvent(block, player);
        listener.onBlockBreak(event);

        assertTrue(event.isCancelled(), "Event must be cancelled when non-sneaking player breaks a protected block");
        verify(player).sendMessage(contains("You cannot break this block because it is part of an active construction"));
        verify(player).sendMessage(contains("sneak (crouch)"));
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
                .thenReturn(new Turtle.BlockProtectionCheck(true, "it is part of an active construction by Turtle T-001."));

        BlockBreakEvent event = new BlockBreakEvent(block, player);
        listener.onBlockBreak(event);

        assertFalse(event.isCancelled(), "Event must NOT be cancelled when sneaking player forces destruction");
        verify(player, never()).sendMessage(contains("You cannot break this block"));
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

    @Test
    @DisplayName("Right clicking Lodestone terminal opens SupplyStationGUI")
    void testInteractLodestoneOpensSupplyStationGUI() {
        Location loc = new Location(mockWorld, 15, 64, 15);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(loc);
        when(block.getType()).thenReturn(Material.LODESTONE);

        Turtle mockTurtle = mock(Turtle.class);
        when(mockTurtle.getId()).thenReturn("T-001");
        when(mockTurtleManager.getTurtleByTerminal(loc)).thenReturn(mockTurtle);

        Player player = mock(Player.class);
        when(player.getLocation()).thenReturn(loc);

        org.bukkit.event.player.PlayerInteractEvent event = new org.bukkit.event.player.PlayerInteractEvent(
                player,
                org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK,
                null,
                block,
                org.bukkit.block.BlockFace.UP
        );
        listener.onTurtleInteract(event);

        assertTrue(event.isCancelled());
        verify(player).openInventory(any(Inventory.class));
    }

    @Test
    @DisplayName("Clicking Re-validate & Resume button in SupplyStationGUI calls revalidateAndResume")
    void testSupplyStationGUIResumeClick() {
        Turtle mockTurtle = mock(Turtle.class);
        when(mockTurtle.getId()).thenReturn("T-001");
        when(mockTurtle.revalidateAndResume()).thenReturn(true);
        when(mockTurtleManager.getTurtleById("T-001")).thenReturn(mockTurtle);

        Player player = mock(Player.class);
        org.bukkit.inventory.InventoryView view = mock(org.bukkit.inventory.InventoryView.class);
        when(view.getTitle()).thenReturn(SupplyStationGUI.TITLE_PREFIX + "T-001");
        Inventory mockInv = mock(Inventory.class);
        when(view.getTopInventory()).thenReturn(mockInv);
        when(view.getPlayer()).thenReturn(player);

        org.bukkit.event.inventory.InventoryClickEvent event = new org.bukkit.event.inventory.InventoryClickEvent(
                view,
                org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER,
                SupplyStationGUI.SLOT_RESUME,
                org.bukkit.event.inventory.ClickType.LEFT,
                org.bukkit.event.inventory.InventoryAction.PICKUP_ALL
        );

        listener.onInventoryClick(event);

        assertTrue(event.isCancelled());
        verify(mockTurtle).revalidateAndResume();
        verify(player).sendMessage(contains("re-validated inventories and resumed"));
    }

    @Test
    @DisplayName("Clicking Stop button in SupplyStationGUI calls stopAnyWork")
    void testSupplyStationGUIStopClick() {
        Turtle mockTurtle = mock(Turtle.class);
        when(mockTurtle.getId()).thenReturn("T-001");
        when(mockTurtle.stopAnyWork()).thenReturn(true);
        when(mockTurtleManager.getTurtleById("T-001")).thenReturn(mockTurtle);

        Player player = mock(Player.class);
        org.bukkit.inventory.InventoryView view = mock(org.bukkit.inventory.InventoryView.class);
        when(view.getTitle()).thenReturn(SupplyStationGUI.TITLE_PREFIX + "T-001");
        Inventory mockInv = mock(Inventory.class);
        when(view.getTopInventory()).thenReturn(mockInv);
        when(view.getPlayer()).thenReturn(player);

        org.bukkit.event.inventory.InventoryClickEvent event = new org.bukkit.event.inventory.InventoryClickEvent(
                view,
                org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER,
                SupplyStationGUI.SLOT_STOP,
                org.bukkit.event.inventory.ClickType.LEFT,
                org.bukkit.event.inventory.InventoryAction.PICKUP_ALL
        );

        listener.onInventoryClick(event);

        assertTrue(event.isCancelled());
        verify(mockTurtle).stopAnyWork();
        verify(player).sendMessage(contains("Stopped active task for Turtle T-001"));
    }

    @Test
    @DisplayName("onTurtleInteract ignores MultiverseNets blocks such as Lodestone controller")
    void testTurtleInteractIgnoresMultiverseNetsLodestone() {
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.LODESTONE);
        when(block.getX()).thenReturn(20);
        when(block.getY()).thenReturn(64);
        when(block.getZ()).thenReturn(20);

        org.bukkit.Chunk chunk = mock(org.bukkit.Chunk.class);
        when(chunk.isLoaded()).thenReturn(true);
        org.bukkit.persistence.PersistentDataContainer pdc = BukkitMockHelper.createMockPDC();
        pdc.set(new org.bukkit.NamespacedKey("multiversenets", "t20_64_20"), org.bukkit.persistence.PersistentDataType.STRING, "MVN_CONTROLLER");
        when(chunk.getPersistentDataContainer()).thenReturn(pdc);
        when(block.getChunk()).thenReturn(chunk);

        Player player = mock(Player.class);
        org.bukkit.event.player.PlayerInteractEvent event = new org.bukkit.event.player.PlayerInteractEvent(
                player,
                org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK,
                null,
                block,
                org.bukkit.block.BlockFace.UP,
                org.bukkit.inventory.EquipmentSlot.HAND
        );

        listener.onTurtleInteract(event);

        assertFalse(event.isCancelled(), "Event must not be cancelled for MultiverseNets controller block");
        verify(mockTurtleManager, never()).getTurtleByTerminal(any());
        verify(player, never()).openInventory(any(Inventory.class));
    }
}
