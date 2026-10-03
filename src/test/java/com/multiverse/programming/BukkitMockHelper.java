// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.UnsafeValues;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public final class BukkitMockHelper {

    private BukkitMockHelper() {
    }

    public static Server setUpMockServer() {
        Server server = mock(Server.class);
        ItemFactory itemFactory = mock(ItemFactory.class);
        UnsafeValues unsafe = mock(UnsafeValues.class);

        stubIfPresent(unsafe, "createEmptyStack", mock(ItemStack.class));
        when(server.getUnsafe()).thenReturn(unsafe);

        when(itemFactory.createItemStack(any())).thenAnswer(inv -> {
            ItemStack stack = mock(ItemStack.class);
            ItemMeta meta = createMockItemMeta(Material.STONE);
            when(stack.getItemMeta()).thenReturn(meta);
            return stack;
        });

        when(itemFactory.getItemMeta(any())).thenAnswer(inv -> {
            Material mat = inv.getArgument(0);
            return createMockItemMeta(mat);
        });

        when(server.getItemFactory()).thenReturn(itemFactory);

        when(server.createBlockData(anyString())).thenAnswer(inv -> {
            String str = inv.getArgument(0);
            org.bukkit.block.data.BlockData bd = mock(org.bukkit.block.data.BlockData.class);
            String clean = str;
            if (clean.contains("[")) clean = clean.substring(0, clean.indexOf('['));
            if (clean.startsWith("minecraft:")) clean = clean.substring("minecraft:".length());
            Material m = Material.matchMaterial(clean.toUpperCase());
            when(bd.getMaterial()).thenReturn(m != null ? m : Material.STONE);
            when(bd.getAsString()).thenReturn(str);
            return bd;
        });

        when(server.createBlockData(any(Material.class))).thenAnswer(inv -> {
            Material m = inv.getArgument(0);
            org.bukkit.block.data.BlockData bd = mock(org.bukkit.block.data.BlockData.class);
            when(bd.getMaterial()).thenReturn(m != null ? m : Material.STONE);
            when(bd.getAsString()).thenReturn("minecraft:" + (m != null ? m.name().toLowerCase() : "stone"));
            return bd;
        });

        when(server.createInventory(any(), anyInt(), anyString())).thenAnswer(inv -> mock(org.bukkit.inventory.Inventory.class));

        try {
            Field serverField = Bukkit.class.getDeclaredField("server");
            serverField.setAccessible(true);
            serverField.set(null, server);
        } catch (Exception e) {
            try {
                Bukkit.setServer(server);
            } catch (Throwable ignored) {
            }
        }

        return server;
    }

    /**
     * Stubs a no-arg method only when the running Paper API still declares it. The suite is
     * compiled against 1.21.11, 26.1 and 26.2 (see the api-26.x profiles), and some internals such
     * as {@code UnsafeValues#createEmptyStack()} no longer exist in the newer APIs.
     */
    private static void stubIfPresent(Object mock, String methodName, Object result) {
        try {
            Method method = mock.getClass().getMethod(methodName);
            when(method.invoke(mock)).thenReturn(result);
        } catch (NoSuchMethodException ignored) {
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not stub " + methodName, e);
        }
    }

    /**
     * An empty (AIR) stack. Since Paper 26.2, {@code new ItemStack(Material.AIR)} is built through
     * the server-side {@code InternalAPIBridge}, which only exists on a running server.
     */
    public static ItemStack emptyStack() {
        ItemStack stack = mock(ItemStack.class);
        when(stack.getType()).thenReturn(Material.AIR);
        when(stack.isEmpty()).thenReturn(true);
        return stack;
    }

    public static ItemMeta createMockItemMeta(Material mat) {
        if (mat == Material.WRITABLE_BOOK || mat == Material.WRITTEN_BOOK) {
            BookMeta bookMeta = mock(BookMeta.class);
            String[] nameHolder = new String[]{DiskManager.NAME};
            when(bookMeta.getDisplayName()).thenAnswer(i -> nameHolder[0]);
            when(bookMeta.hasDisplayName()).thenAnswer(i -> nameHolder[0] != null);
            doAnswer(i -> {
                nameHolder[0] = i.getArgument(0);
                return null;
            }).when(bookMeta).setDisplayName(anyString());

            List<String> pages = new ArrayList<>();
            when(bookMeta.hasPages()).thenAnswer(i -> !pages.isEmpty());
            when(bookMeta.getPages()).thenAnswer(i -> new ArrayList<>(pages));
            when(bookMeta.getPage(anyInt())).thenAnswer(i -> {
                int pageNum = i.getArgument(0);
                return (pageNum > 0 && pageNum <= pages.size()) ? pages.get(pageNum - 1) : "";
            });

            doAnswer(i -> {
                List<?> newPages = i.getArgument(0);
                pages.clear();
                if (newPages != null) {
                    for (Object p : newPages) pages.add(String.valueOf(p));
                }
                return null;
            }).when(bookMeta).setPages(anyList());

            doAnswer(i -> {
                pages.clear();
                for (Object arg : i.getArguments()) {
                    if (arg instanceof String[] arr) {
                        pages.addAll(Arrays.asList(arr));
                    } else if (arg != null) {
                        pages.add(String.valueOf(arg));
                    }
                }
                return null;
            }).when(bookMeta).setPages(any(String[].class));

            org.bukkit.persistence.PersistentDataContainer pdc = createMockPDC();
            when(bookMeta.getPersistentDataContainer()).thenReturn(pdc);

            return bookMeta;
        }

        ItemMeta meta = mock(ItemMeta.class);
        String[] nameHolder = new String[]{mat == Material.ENCHANTING_TABLE ? DiskManager.ADVANCED_COMPUTER_NAME : "Computer"};
        when(meta.getDisplayName()).thenAnswer(i -> nameHolder[0]);
        when(meta.hasDisplayName()).thenAnswer(i -> nameHolder[0] != null);
        doAnswer(i -> {
            nameHolder[0] = i.getArgument(0);
            return null;
        }).when(meta).setDisplayName(anyString());
        org.bukkit.persistence.PersistentDataContainer pdc = createMockPDC();
        when(meta.getPersistentDataContainer()).thenReturn(pdc);
        return meta;
    }

    public static org.bukkit.persistence.PersistentDataContainer createMockPDC() {
        org.bukkit.persistence.PersistentDataContainer pdc = mock(org.bukkit.persistence.PersistentDataContainer.class);
        java.util.Map<org.bukkit.NamespacedKey, Object> store = new java.util.HashMap<>();
        doAnswer(inv -> {
            org.bukkit.NamespacedKey k = inv.getArgument(0);
            Object v = inv.getArgument(2);
            store.put(k, v);
            return null;
        }).when(pdc).set(any(), any(), any());
        doAnswer(inv -> {
            org.bukkit.NamespacedKey k = inv.getArgument(0);
            return store.get(k);
        }).when(pdc).get(any(), any());
        doAnswer(inv -> {
            org.bukkit.NamespacedKey k = inv.getArgument(0);
            return store.containsKey(k);
        }).when(pdc).has(any(), any());
        doAnswer(inv -> {
            org.bukkit.NamespacedKey k = inv.getArgument(0);
            store.remove(k);
            return null;
        }).when(pdc).remove(any());
        when(pdc.getKeys()).thenAnswer(inv -> new java.util.HashSet<>(store.keySet()));
        return pdc;
    }

    public static void tearDownMockServer() {
        try {
            Field serverField = Bukkit.class.getDeclaredField("server");
            serverField.setAccessible(true);
            serverField.set(null, null);
        } catch (Exception ignored) {
        }
    }
}
