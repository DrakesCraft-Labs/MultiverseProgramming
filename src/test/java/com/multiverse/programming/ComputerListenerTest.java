// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

class ComputerListenerTest {

    private MultiverseProgrammingPlugin plugin;
    private ConfigManager configManager;
    private ComputerListener listener;

    @BeforeEach
    void setUp() {
        BukkitMockHelper.setUpMockServer();
        plugin = mock(MultiverseProgrammingPlugin.class);
        configManager = mock(ConfigManager.class);
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(plugin.getPrefix()).thenReturn("§8[§bComputer§8]");
        when(configManager.getComputerBlock()).thenReturn(Material.LECTERN);
        when(configManager.getAdvancedComputerBlock()).thenReturn(Material.ENCHANTING_TABLE);
        when(configManager.isDropDisksOnBreak()).thenReturn(true);
        when(configManager.getPreventSpamDelayMs()).thenReturn(500L);
        when(configManager.isEnableComputer()).thenReturn(true);
        when(configManager.isEnableAdvancedComputer()).thenReturn(true);

        listener = new ComputerListener(plugin, Material.LECTERN, Material.ENCHANTING_TABLE);
    }

    @AfterEach
    void tearDown() {
        ComputerListener.cancelAll();
        BukkitMockHelper.tearDownMockServer();
    }

    @Test
    @DisplayName("onBlockUse checks multiverseprogramming.use permission")
    void testBlockUsePermissionDenied() {
        Player player = mock(Player.class);
        when(player.hasPermission("multiverseprogramming.use")).thenReturn(false);

        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.LECTERN);

        PlayerInteractEvent event = new PlayerInteractEvent(
                player,
                Action.RIGHT_CLICK_BLOCK,
                null,
                block,
                org.bukkit.block.BlockFace.UP,
                EquipmentSlot.HAND
        );

        listener.onBlockUse(event);
        assertTrue(event.isCancelled());
        verify(player).sendMessage(contains("permission"));
        verify(player, never()).openInventory(any(Inventory.class));
    }

    @Test
    @DisplayName("Breaking advanced computer drops stored floppy disk naturally")
    void testBlockBreakDropsDisk() {
        Block block = mock(Block.class);
        World world = mock(World.class);
        Location loc = new Location(world, 10, 64, 10);
        when(block.getLocation()).thenReturn(loc);
        when(block.getWorld()).thenReturn(world);
        when(block.getType()).thenReturn(Material.ENCHANTING_TABLE);

        ItemStack disk = mock(ItemStack.class);
        when(disk.getType()).thenReturn(Material.WRITABLE_BOOK);

        ComputerListener.getAdvancedDisks().put(loc, disk);

        Player player = mock(Player.class);
        BlockBreakEvent event = new BlockBreakEvent(block, player);

        listener.onBlockBreak(event);

        // Verify disk was dropped in the world
        verify(world).dropItemNaturally(loc, disk);
        // Verify disk was removed from map to prevent memory leak
        assertFalse(ComputerListener.getAdvancedDisks().containsKey(loc));
    }

    @Test
    @DisplayName("Piston extending into computer block is cancelled")
    void testPistonExtendCancelled() {
        Block computerBlock = mock(Block.class);
        when(computerBlock.getType()).thenReturn(Material.LECTERN);

        Block piston = mock(Block.class);
        BlockPistonExtendEvent event = new BlockPistonExtendEvent(piston, List.of(computerBlock), org.bukkit.block.BlockFace.NORTH);

        listener.onPistonExtend(event);
        assertTrue(event.isCancelled());
    }

    @Test
    @DisplayName("WorldUnload cleans up world locations to prevent memory leaks")
    void testWorldUnloadCleanup() {
        World world = mock(World.class);
        Location loc = new Location(world, 0, 60, 0);

        ItemStack disk = mock(ItemStack.class);
        when(disk.getType()).thenReturn(Material.WRITABLE_BOOK);
        ComputerListener.getAdvancedDisks().put(loc, disk);

        WorldUnloadEvent event = new WorldUnloadEvent(world);
        listener.onWorldUnload(event);

        assertFalse(ComputerListener.getAdvancedDisks().containsKey(loc));
    }

    @Test
    @DisplayName("SWAP_OFFHAND click is always cancelled in Computer GUI")
    void testSwapOffhandCancelled() {
        Player player = mock(Player.class);
        InventoryView view = mock(InventoryView.class);
        when(view.getPlayer()).thenReturn(player);
        when(view.getTitle()).thenReturn(ComputerGUI.TITLE);

        InventoryClickEvent event = new InventoryClickEvent(
                view,
                org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER,
                0,
                ClickType.SWAP_OFFHAND,
                org.bukkit.event.inventory.InventoryAction.HOTBAR_SWAP
        );

        listener.onInventoryClick(event);
        assertTrue(event.isCancelled());
    }

    @Test
    @DisplayName("Vanilla interactions with NPC entity are cancelled")
    void testNpcVanillaInteractionsCancelled() {
        org.bukkit.entity.Entity npcEntity = mock(org.bukkit.entity.Entity.class);
        when(npcEntity.getScoreboardTags()).thenReturn(java.util.Set.of(com.multiverse.programming.peripheral.NpcPeripheral.NPC_TAG));

        org.bukkit.event.player.PlayerInteractEntityEvent interactEvent = mock(org.bukkit.event.player.PlayerInteractEntityEvent.class);
        when(interactEvent.getRightClicked()).thenReturn(npcEntity);
        listener.onPlayerInteractNpc(interactEvent);
        verify(interactEvent).setCancelled(true);

        org.bukkit.event.player.PlayerInteractAtEntityEvent interactAtEvent = mock(org.bukkit.event.player.PlayerInteractAtEntityEvent.class);
        when(interactAtEvent.getRightClicked()).thenReturn(npcEntity);
        listener.onPlayerInteractAtNpc(interactAtEvent);
        verify(interactAtEvent).setCancelled(true);

        org.bukkit.event.entity.EntityDamageEvent dmgEvent = mock(org.bukkit.event.entity.EntityDamageEvent.class);
        when(dmgEvent.getEntity()).thenReturn(npcEntity);
        listener.onNpcDamage(dmgEvent);
        verify(dmgEvent).setCancelled(true);

        org.bukkit.event.entity.EntityTransformEvent transEvent = mock(org.bukkit.event.entity.EntityTransformEvent.class);
        when(transEvent.getEntity()).thenReturn(npcEntity);
        listener.onNpcTransform(transEvent);
        verify(transEvent).setCancelled(true);
    }

    @Test
    @DisplayName("Sculk bloom is cancelled on NPC block adjacent to computer")
    void testSculkBloomCancelledOnNpcBlock() {
        when(configManager.getNpcBlock()).thenReturn(Material.SCULK_CATALYST);

        Block catalystBlock = mock(Block.class);
        when(catalystBlock.getType()).thenReturn(Material.SCULK_CATALYST);

        Block computer = mock(Block.class);
        when(computer.getType()).thenReturn(Material.LECTERN);
        when(catalystBlock.getRelative(org.bukkit.block.BlockFace.NORTH)).thenReturn(computer);

        org.bukkit.event.block.SculkBloomEvent bloomEvent = mock(org.bukkit.event.block.SculkBloomEvent.class);
        when(bloomEvent.getBlock()).thenReturn(catalystBlock);
        listener.onSculkBloom(bloomEvent);
        verify(bloomEvent).setCancelled(true);
    }
}
