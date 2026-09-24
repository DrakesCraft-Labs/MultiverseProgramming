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

        when(unsafe.createEmptyStack()).thenReturn(mock(ItemStack.class));
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
        return meta;
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
