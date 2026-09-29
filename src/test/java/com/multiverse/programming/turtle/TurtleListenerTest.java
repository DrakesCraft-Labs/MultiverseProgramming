// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.turtle;

import com.multiverse.programming.BukkitMockHelper;
import com.multiverse.programming.ConfigManager;
import com.multiverse.programming.DiskManager;
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
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
        org.bukkit.Server server = BukkitMockHelper.setUpMockServer();
        doReturn(new ArrayList<Player>()).when(server).getOnlinePlayers();
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
        when(mockTurtle.isAccessAllowed(any())).thenReturn(true);

        Player player = mock(Player.class);
        when(player.getLocation()).thenReturn(loc);
        when(player.hasPermission(TurtleListener.PERMISSION_TURTLE)).thenReturn(true);

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
        when(mockTurtle.isAccessAllowed(any())).thenReturn(true);
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
        when(mockTurtle.isAccessAllowed(any())).thenReturn(true);
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
    @DisplayName("A regular player with the turtle permission opens the Turtle control panel")
    void testRegularPlayerOpensTurtleGUI() {
        Location loc = new Location(mockWorld, 5, 64, 5);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(loc);
        when(block.getType()).thenReturn(Material.DISPENSER);

        Turtle mockTurtle = mock(Turtle.class);
        when(mockTurtle.getId()).thenReturn("T-001");
        when(mockTurtle.getStatus()).thenReturn(Turtle.Status.IDLE);
        when(mockTurtle.getStatusMessage()).thenReturn("Idle");
        when(mockTurtle.isAccessAllowed(any())).thenReturn(true);
        when(mockTurtleManager.getTurtle(loc)).thenReturn(mockTurtle);

        Player player = mock(Player.class);
        when(player.hasPermission(TurtleListener.PERMISSION_TURTLE)).thenReturn(true);

        org.bukkit.event.player.PlayerInteractEvent event = new org.bukkit.event.player.PlayerInteractEvent(
                player,
                org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK,
                null,
                block,
                org.bukkit.block.BlockFace.UP,
                org.bukkit.inventory.EquipmentSlot.HAND
        );
        listener.onTurtleInteract(event);

        assertTrue(event.isCancelled());
        verify(player).openInventory(any(Inventory.class));
    }

    @Test
    @DisplayName("A player without the turtle permission is denied and no GUI opens")
    void testPlayerWithoutTurtlePermissionIsDenied() {
        Location loc = new Location(mockWorld, 5, 64, 5);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(loc);
        when(block.getType()).thenReturn(Material.DISPENSER);

        Turtle mockTurtle = mock(Turtle.class);
        when(mockTurtle.getId()).thenReturn("T-001");
        when(mockTurtleManager.getTurtle(loc)).thenReturn(mockTurtle);

        Player player = mock(Player.class);
        when(player.hasPermission(TurtleListener.PERMISSION_TURTLE)).thenReturn(false);

        org.bukkit.event.player.PlayerInteractEvent event = new org.bukkit.event.player.PlayerInteractEvent(
                player,
                org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK,
                null,
                block,
                org.bukkit.block.BlockFace.UP,
                org.bukkit.inventory.EquipmentSlot.HAND
        );
        listener.onTurtleInteract(event);

        assertTrue(event.isCancelled(), "Interaction must be swallowed so the turtle block is not used as a container");
        verify(player, never()).openInventory(any(Inventory.class));
        verify(player).sendMessage(contains("don't have permission to use Turtles"));
    }

    @Test
    @DisplayName("The Turtle owner can open the control panel")
    void testOwnerOpensTurtleGUI() {
        Location loc = new Location(mockWorld, 5, 64, 5);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(loc);
        when(block.getType()).thenReturn(Material.DISPENSER);

        UUID ownerUuid = UUID.randomUUID();
        Turtle mockTurtle = mock(Turtle.class);
        when(mockTurtle.getId()).thenReturn("T-001");
        when(mockTurtle.getOwner()).thenReturn(ownerUuid);
        when(mockTurtle.getStatus()).thenReturn(Turtle.Status.IDLE);
        when(mockTurtle.getStatusMessage()).thenReturn("Idle");
        when(mockTurtle.isAccessAllowed(ownerUuid)).thenReturn(true);
        when(mockTurtleManager.getTurtle(loc)).thenReturn(mockTurtle);

        Player owner = mock(Player.class);
        when(owner.getUniqueId()).thenReturn(ownerUuid);
        when(owner.hasPermission(TurtleListener.PERMISSION_TURTLE)).thenReturn(true);
        when(owner.hasPermission(TurtleListener.PERMISSION_ADMIN)).thenReturn(false);

        org.bukkit.event.player.PlayerInteractEvent event = new org.bukkit.event.player.PlayerInteractEvent(
                owner,
                org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK,
                null,
                block,
                org.bukkit.block.BlockFace.UP,
                org.bukkit.inventory.EquipmentSlot.HAND
        );
        listener.onTurtleInteract(event);

        assertTrue(event.isCancelled());
        verify(owner).openInventory(any(Inventory.class));
    }

    @Test
    @DisplayName("A player authorized by the owner can open the control panel")
    void testAuthorizedPlayerOpensTurtleGUI() {
        Location loc = new Location(mockWorld, 5, 64, 5);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(loc);
        when(block.getType()).thenReturn(Material.DISPENSER);

        UUID ownerUuid = UUID.randomUUID();
        UUID allyUuid = UUID.randomUUID();
        Turtle mockTurtle = mock(Turtle.class);
        when(mockTurtle.getId()).thenReturn("T-001");
        when(mockTurtle.getOwner()).thenReturn(ownerUuid);
        when(mockTurtle.getStatus()).thenReturn(Turtle.Status.IDLE);
        when(mockTurtle.getStatusMessage()).thenReturn("Idle");
        when(mockTurtle.isAccessAllowed(allyUuid)).thenReturn(true);
        when(mockTurtleManager.getTurtle(loc)).thenReturn(mockTurtle);

        Player ally = mock(Player.class);
        when(ally.getUniqueId()).thenReturn(allyUuid);
        when(ally.hasPermission(TurtleListener.PERMISSION_TURTLE)).thenReturn(true);
        when(ally.hasPermission(TurtleListener.PERMISSION_ADMIN)).thenReturn(false);

        org.bukkit.event.player.PlayerInteractEvent event = new org.bukkit.event.player.PlayerInteractEvent(
                ally,
                org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK,
                null,
                block,
                org.bukkit.block.BlockFace.UP,
                org.bukkit.inventory.EquipmentSlot.HAND
        );
        listener.onTurtleInteract(event);

        assertTrue(event.isCancelled());
        verify(ally).openInventory(any(Inventory.class));
    }

    @Test
    @DisplayName("A player who is neither owner nor authorized is denied")
    void testUnauthorizedPlayerDenied() {
        Location loc = new Location(mockWorld, 5, 64, 5);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(loc);
        when(block.getType()).thenReturn(Material.DISPENSER);

        UUID ownerUuid = UUID.randomUUID();
        UUID strangerUuid = UUID.randomUUID();
        Turtle mockTurtle = mock(Turtle.class);
        when(mockTurtle.getId()).thenReturn("T-001");
        when(mockTurtle.getOwner()).thenReturn(ownerUuid);
        when(mockTurtle.isAccessAllowed(strangerUuid)).thenReturn(false);
        when(mockTurtleManager.getTurtle(loc)).thenReturn(mockTurtle);

        Player stranger = mock(Player.class);
        when(stranger.getUniqueId()).thenReturn(strangerUuid);
        when(stranger.hasPermission(TurtleListener.PERMISSION_TURTLE)).thenReturn(true);
        when(stranger.hasPermission(TurtleListener.PERMISSION_ADMIN)).thenReturn(false);

        org.bukkit.event.player.PlayerInteractEvent event = new org.bukkit.event.player.PlayerInteractEvent(
                stranger,
                org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK,
                null,
                block,
                org.bukkit.block.BlockFace.UP,
                org.bukkit.inventory.EquipmentSlot.HAND
        );
        listener.onTurtleInteract(event);

        assertTrue(event.isCancelled());
        verify(stranger, never()).openInventory(any(Inventory.class));
        verify(stranger).sendMessage(contains("authorized by the owner"));
    }

    @Test
    @DisplayName("Owner clicking an online player in the access GUI authorizes them")
    void testOwnerAuthorizesPlayerViaAccessGUI() {
        UUID ownerUuid = UUID.randomUUID();
        UUID targetUuid = UUID.randomUUID();

        Turtle mockTurtle = mock(Turtle.class);
        when(mockTurtle.getId()).thenReturn("T-001");
        when(mockTurtle.getOwner()).thenReturn(ownerUuid);
        when(mockTurtle.getOwnerName()).thenReturn("Owner");
        when(mockTurtle.isOwner(ownerUuid)).thenReturn(true);
        when(mockTurtle.getAuthorizedPlayers()).thenReturn(new ArrayList<>());
        when(mockTurtle.addAuthorized(targetUuid)).thenReturn(true);

        TurtleAccessHolder holder = new TurtleAccessHolder(mockTurtle);
        Inventory mockInv = mock(Inventory.class);
        holder.setInventory(mockInv);
        when(mockInv.getHolder()).thenReturn(holder);

        Player owner = mock(Player.class);
        when(owner.getUniqueId()).thenReturn(ownerUuid);
        when(owner.hasPermission(TurtleListener.PERMISSION_ADMIN)).thenReturn(false);
        when(owner.getName()).thenReturn("Owner");

        ItemStack head = mock(ItemStack.class);
        when(head.getType()).thenReturn(Material.PLAYER_HEAD);
        ItemMeta headMeta = BukkitMockHelper.createMockItemMeta(Material.PLAYER_HEAD);
        headMeta.getPersistentDataContainer().set(TurtleAccessGUI.ACCESS_UUID_KEY, PersistentDataType.STRING, targetUuid.toString());
        when(head.getItemMeta()).thenReturn(headMeta);

        InventoryView view = mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(mockInv);
        when(view.getTitle()).thenReturn(TurtleAccessGUI.TITLE_PREFIX + "T-001");
        when(view.getPlayer()).thenReturn(owner);

        org.bukkit.event.inventory.InventoryClickEvent event = mock(org.bukkit.event.inventory.InventoryClickEvent.class);
        when(event.getView()).thenReturn(view);
        when(event.getWhoClicked()).thenReturn(owner);
        when(event.getRawSlot()).thenReturn(TurtleAccessGUI.FIRST_ONLINE_SLOT);
        when(event.getCurrentItem()).thenReturn(head);

        listener.onInventoryClick(event);

        verify(mockTurtle).addAuthorized(targetUuid);
        verify(owner).sendMessage(contains("can now open and operate this Turtle"));
    }

    @Test
    @DisplayName("Placing a Turtle without the turtle permission is denied")
    void testTurtlePlaceDeniedWithoutPermission() {
        org.bukkit.inventory.ItemStack turtleItem = DiskManager.createTurtle(Material.DISPENSER);
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.DISPENSER);

        Player player = mock(Player.class);
        when(player.getLocation()).thenReturn(new Location(mockWorld, 0, 64, 0));
        when(player.hasPermission(TurtleListener.PERMISSION_TURTLE)).thenReturn(false);

        org.bukkit.event.block.BlockPlaceEvent event = new org.bukkit.event.block.BlockPlaceEvent(
                block,
                mock(org.bukkit.block.BlockState.class),
                mock(Block.class),
                turtleItem,
                player,
                true,
                org.bukkit.inventory.EquipmentSlot.HAND
        );
        listener.onTurtlePlace(event);

        assertTrue(event.isCancelled());
        verify(mockTurtleManager, never()).createTurtle(any(), any(), any());
        verify(player).sendMessage(contains("don't have permission to place Turtles"));
    }

    @Test
    @DisplayName("Placing a Turtle with the turtle permission is allowed")
    void testTurtlePlaceAllowedWithPermission() {
        org.bukkit.inventory.ItemStack turtleItem = DiskManager.createTurtle(Material.DISPENSER);
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.DISPENSER);

        Turtle mockTurtle = mock(Turtle.class);
        when(mockTurtle.getId()).thenReturn("T-001");
        when(mockTurtleManager.createTurtle(any(), any(), any())).thenReturn(mockTurtle);

        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getLocation()).thenReturn(new Location(mockWorld, 0, 64, 0));
        when(player.hasPermission(TurtleListener.PERMISSION_TURTLE)).thenReturn(true);

        org.bukkit.event.block.BlockPlaceEvent event = new org.bukkit.event.block.BlockPlaceEvent(
                block,
                mock(org.bukkit.block.BlockState.class),
                mock(Block.class),
                turtleItem,
                player,
                true,
                org.bukkit.inventory.EquipmentSlot.HAND
        );
        listener.onTurtlePlace(event);

        assertFalse(event.isCancelled());
        verify(mockTurtleManager).createTurtle(any(), any(), any());
        verify(player).sendMessage(contains("placed"));
    }

    @Test
    @DisplayName("The owner can break and disassemble their own Turtle")
    void testOwnerBreaksTurtle() {
        Location loc = new Location(mockWorld, 10, 64, 10);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(loc);
        when(block.getType()).thenReturn(Material.DISPENSER);
        when(block.getWorld()).thenReturn(mockWorld);

        UUID ownerUuid = UUID.randomUUID();
        Turtle mockTurtle = turtleOwnedBy(ownerUuid, "T-001");
        when(mockTurtleManager.getTurtle(loc)).thenReturn(mockTurtle);

        Player owner = mock(Player.class);
        when(owner.getUniqueId()).thenReturn(ownerUuid);
        when(owner.hasPermission(TurtleListener.PERMISSION_ADMIN)).thenReturn(false);
        when(owner.isSneaking()).thenReturn(false);
        when(owner.getLocation()).thenReturn(loc);

        BlockBreakEvent event = new BlockBreakEvent(block, owner);
        listener.onBlockBreak(event);

        assertFalse(event.isCancelled());
        verify(mockTurtleManager).removeTurtle(any());
        verify(owner).sendMessage(contains("disassembled"));
    }

    @Test
    @DisplayName("A stranger cannot break someone else's Turtle")
    void testStrangerCannotBreakTurtle() {
        Location loc = new Location(mockWorld, 10, 64, 10);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(loc);
        when(block.getType()).thenReturn(Material.DISPENSER);

        Turtle mockTurtle = turtleOwnedBy(UUID.randomUUID(), "T-001");
        when(mockTurtleManager.getTurtle(loc)).thenReturn(mockTurtle);

        Player stranger = mock(Player.class);
        when(stranger.getUniqueId()).thenReturn(UUID.randomUUID());
        when(stranger.hasPermission(TurtleListener.PERMISSION_ADMIN)).thenReturn(false);
        when(stranger.isSneaking()).thenReturn(false);
        when(stranger.getLocation()).thenReturn(loc);

        BlockBreakEvent event = new BlockBreakEvent(block, stranger);
        listener.onBlockBreak(event);

        assertTrue(event.isCancelled());
        verify(mockTurtleManager, never()).removeTurtle(any());
        verify(stranger).sendMessage(contains("only its owner or an administrator"));
    }

    @Test
    @DisplayName("An unclaimed Turtle cannot be broken by a regular player")
    void testUnownedTurtleCannotBeBrokenByPlayer() {
        Location loc = new Location(mockWorld, 10, 64, 10);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(loc);
        when(block.getType()).thenReturn(Material.DISPENSER);

        Turtle mockTurtle = turtleOwnedBy(null, "T-001");
        when(mockTurtleManager.getTurtle(loc)).thenReturn(mockTurtle);

        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.hasPermission(TurtleListener.PERMISSION_ADMIN)).thenReturn(false);
        when(player.isSneaking()).thenReturn(false);
        when(player.getLocation()).thenReturn(loc);

        BlockBreakEvent event = new BlockBreakEvent(block, player);
        listener.onBlockBreak(event);

        assertTrue(event.isCancelled());
        verify(mockTurtleManager, never()).removeTurtle(any());
    }

    @Test
    @DisplayName("An administrator can break any Turtle")
    void testAdminCanBreakTurtle() {
        Location loc = new Location(mockWorld, 10, 64, 10);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(loc);
        when(block.getType()).thenReturn(Material.DISPENSER);
        when(block.getWorld()).thenReturn(mockWorld);

        Turtle mockTurtle = turtleOwnedBy(UUID.randomUUID(), "T-001");
        when(mockTurtleManager.getTurtle(loc)).thenReturn(mockTurtle);

        Player admin = mock(Player.class);
        when(admin.getUniqueId()).thenReturn(UUID.randomUUID());
        when(admin.hasPermission(TurtleListener.PERMISSION_ADMIN)).thenReturn(true);
        when(admin.isSneaking()).thenReturn(false);
        when(admin.getLocation()).thenReturn(loc);

        BlockBreakEvent event = new BlockBreakEvent(block, admin);
        listener.onBlockBreak(event);

        assertFalse(event.isCancelled());
        verify(mockTurtleManager).removeTurtle(any());
    }

    private Turtle turtleOwnedBy(UUID ownerUuid, String id) {
        Turtle turtle = mock(Turtle.class);
        when(turtle.getId()).thenReturn(id);
        when(turtle.getOwner()).thenReturn(ownerUuid);
        when(turtle.getDisk()).thenReturn(null);
        when(turtle.getInventory()).thenReturn(new ItemStack[16]);
        when(mockTurtleManager.checkBlockProtection(any())).thenReturn(null);
        return turtle;
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

    @Test
    @DisplayName("onTurtlePlace does not create turtle when placing vanilla dispenser")
    void testVanillaDispenserPlaceDoesNotCreateTurtle() {
        org.bukkit.inventory.ItemStack vanillaDispenser = new org.bukkit.inventory.ItemStack(Material.DISPENSER);
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.DISPENSER);

        Player player = mock(Player.class);
        when(player.getLocation()).thenReturn(new Location(mockWorld, 0, 64, 0));

        org.bukkit.event.block.BlockPlaceEvent event = new org.bukkit.event.block.BlockPlaceEvent(
                block,
                mock(org.bukkit.block.BlockState.class),
                mock(Block.class),
                vanillaDispenser,
                player,
                true,
                org.bukkit.inventory.EquipmentSlot.HAND
        );

        listener.onTurtlePlace(event);

        verify(mockTurtleManager, never()).createTurtle(any(), any(), any());
        verify(player, never()).sendMessage(contains("placed"));
    }

    @Test
    @DisplayName("onTurtlePlace does not create turtle when placing Slimefun dispenser")
    void testSlimefunDispenserPlaceDoesNotCreateTurtle() {
        org.bukkit.inventory.ItemStack sfDispenser = new org.bukkit.inventory.ItemStack(Material.DISPENSER);
        org.bukkit.inventory.meta.ItemMeta meta = mock(org.bukkit.inventory.meta.ItemMeta.class);
        org.bukkit.persistence.PersistentDataContainer pdc = BukkitMockHelper.createMockPDC();
        pdc.set(new org.bukkit.NamespacedKey("slimefun", "slimefun_item"), org.bukkit.persistence.PersistentDataType.STRING, "ELECTRIC_FURNACE");
        when(meta.getPersistentDataContainer()).thenReturn(pdc);
        when(sfDispenser.getItemMeta()).thenReturn(meta);

        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.DISPENSER);

        Player player = mock(Player.class);
        when(player.getLocation()).thenReturn(new Location(mockWorld, 0, 64, 0));

        org.bukkit.event.block.BlockPlaceEvent event = new org.bukkit.event.block.BlockPlaceEvent(
                block,
                mock(org.bukkit.block.BlockState.class),
                mock(Block.class),
                sfDispenser,
                player,
                true,
                org.bukkit.inventory.EquipmentSlot.HAND
        );

        listener.onTurtlePlace(event);

        verify(mockTurtleManager, never()).createTurtle(any(), any(), any());
        verify(player, never()).sendMessage(contains("placed"));
    }

    @Test
    @DisplayName("onTurtleInteract does not intercept Slimefun dispenser block")
    void testTurtleInteractIgnoresSlimefunDispenser() {
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.DISPENSER);
        org.bukkit.block.TileState state = mock(org.bukkit.block.TileState.class);
        org.bukkit.persistence.PersistentDataContainer pdc = BukkitMockHelper.createMockPDC();
        pdc.set(new org.bukkit.NamespacedKey("slimefun", "slimefun_item"), org.bukkit.persistence.PersistentDataType.STRING, "CARGO_NODE_INPUT");
        when(state.getPersistentDataContainer()).thenReturn(pdc);
        when(block.getState()).thenReturn(state);

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

        assertFalse(event.isCancelled(), "Event must NOT be cancelled for Slimefun machine block");
        verify(player, never()).openInventory(any(Inventory.class));
    }
}
