// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.block.BlockType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;

public class MockRegistryAccess implements RegistryAccess {

    private static final Map<NamespacedKey, ItemType> itemCache = new HashMap<>();
    private static final Map<NamespacedKey, BlockType> blockCache = new HashMap<>();

    private static boolean isNotAnItem(Material mat) {
        if (mat == Material.AIR || mat == Material.WATER || mat == Material.LAVA
                || mat == Material.CAVE_AIR || mat == Material.VOID_AIR) {
            return true;
        }
        String name = mat.name();
        return name.contains("PORTAL") || name.contains("FIRE")
                || name.contains("PISTON_HEAD") || name.contains("MOVING_PISTON")
                || name.startsWith("POTTED_");
    }

    private static boolean isNotABlock(Material mat) {
        if (mat == Material.DIAMOND || mat == Material.EMERALD || mat == Material.STICK
                || mat == Material.PAPER || mat == Material.BOOK
                || mat == Material.WRITABLE_BOOK || mat == Material.WRITTEN_BOOK
                || mat == Material.FEATHER || mat == Material.LEATHER
                || mat == Material.STRING || mat == Material.GUNPOWDER
                || mat == Material.BOWL || mat == Material.FLINT
                || mat == Material.COAL || mat == Material.CHARCOAL) {
            return true;
        }
        String name = mat.name();
        return name.endsWith("_INGOT") || name.endsWith("_SWORD")
                || name.endsWith("_PICKAXE") || name.endsWith("_AXE")
                || name.endsWith("_SHOVEL") || name.endsWith("_HOE")
                || name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")
                || name.endsWith("_NUGGET");
    }

    private static ItemType findItemType(NamespacedKey key) {
        String name = key.getKey().toUpperCase();
        Material mat = Material.matchMaterial(name);
        if (mat == null) {
            try {
                mat = Material.valueOf(name);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        if (isNotAnItem(mat)) {
            return null;
        }
        Material finalMat = mat;
        return itemCache.computeIfAbsent(key, k -> (ItemType) Proxy.newProxyInstance(
                ItemType.class.getClassLoader(),
                new Class<?>[]{ItemType.class},
                (proxy, method, args) -> {
                    String methodName = method.getName();
                    if ("asMaterial".equals(methodName)) return finalMat;
                    if ("getKey".equals(methodName)) return k;
                    if ("createItemStack".equals(methodName)) {
                        ItemStack is = mock(ItemStack.class);
                        when(is.getType()).thenReturn(finalMat);
                        ItemMeta[] metaHolder = new ItemMeta[]{BukkitMockHelper.createMockItemMeta(finalMat)};
                        when(is.getItemMeta()).thenAnswer(i -> metaHolder[0]);
                        when(is.hasItemMeta()).thenReturn(true);
                        doAnswer(i -> {
                            metaHolder[0] = i.getArgument(0);
                            return null;
                        }).when(is).setItemMeta(any());
                        return is;
                    }
                    if ("equals".equals(methodName)) return proxy == args[0];
                    if ("hashCode".equals(methodName)) return k.hashCode();
                    if ("toString".equals(methodName)) return "ItemType[" + k + "]";
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) return false;
                    if (returnType == int.class || returnType == short.class || returnType == float.class || returnType == double.class) return 0;
                    return null;
                }
        ));
    }

    private static BlockType findBlockType(NamespacedKey key) {
        String name = key.getKey().toUpperCase();
        Material mat = Material.matchMaterial(name);
        if (mat == null) {
            try {
                mat = Material.valueOf(name);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        if (isNotABlock(mat)) {
            return null;
        }
        Material finalMat = mat;
        return blockCache.computeIfAbsent(key, k -> (BlockType) Proxy.newProxyInstance(
                BlockType.class.getClassLoader(),
                new Class<?>[]{BlockType.class},
                (proxy, method, args) -> {
                    String methodName = method.getName();
                    if ("asMaterial".equals(methodName)) return finalMat;
                    if ("getKey".equals(methodName)) return k;
                    if ("isAir".equals(methodName)) return finalMat == Material.AIR || finalMat == Material.CAVE_AIR || finalMat == Material.VOID_AIR;
                    if ("equals".equals(methodName)) return proxy == args[0];
                    if ("hashCode".equals(methodName)) return k.hashCode();
                    if ("toString".equals(methodName)) return "BlockType[" + k + "]";
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) return false;
                    if (returnType == int.class || returnType == float.class) return 0;
                    return null;
                }
        ));
    }

    @SuppressWarnings("unchecked")
    private static <T extends Keyed> Registry<T> createMockRegistry(Class<T> type, Function<NamespacedKey, T> lookup) {
        return (Registry<T>) Proxy.newProxyInstance(
                Registry.class.getClassLoader(),
                new Class<?>[]{Registry.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("get".equals(name) && args != null && args.length == 1) {
                        if (args[0] instanceof NamespacedKey key) return lookup.apply(key);
                        if (args[0] instanceof net.kyori.adventure.key.Key key) return lookup.apply(new NamespacedKey(key.namespace(), key.value()));
                    }
                    if ("getOrThrow".equals(name) && args != null && args.length == 1) {
                        T val = null;
                        if (args[0] instanceof NamespacedKey key) val = lookup.apply(key);
                        else if (args[0] instanceof net.kyori.adventure.key.Key key) val = lookup.apply(new NamespacedKey(key.namespace(), key.value()));
                        if (val == null) {
                            NamespacedKey finalKey = (args[0] instanceof NamespacedKey k)
                                    ? k
                                    : new NamespacedKey(((net.kyori.adventure.key.Key) args[0]).namespace(), ((net.kyori.adventure.key.Key) args[0]).value());
                            Class<?>[] ifaces = (type != null && type.isInterface() && type != Keyed.class)
                                    ? new Class<?>[]{Keyed.class, type}
                                    : new Class<?>[]{Keyed.class};
                            return (T) Proxy.newProxyInstance(
                                    Keyed.class.getClassLoader(),
                                    ifaces,
                                    (p, m, a) -> {
                                        String mName = m.getName();
                                        if ("getKey".equals(mName)) return finalKey;
                                        if ("key".equals(mName)) return finalKey;
                                        if ("name".equals(mName)) return finalKey.getKey().toUpperCase(Locale.ROOT);
                                        if ("ordinal".equals(mName)) return 0;
                                        if ("compareTo".equals(mName)) return 0;
                                        if ("equals".equals(mName)) return p == a[0];
                                        if ("hashCode".equals(mName)) return finalKey.hashCode();
                                        if ("toString".equals(mName)) return type.getSimpleName() + "[" + finalKey + "]";
                                        Class<?> ret = m.getReturnType();
                                        if (ret == boolean.class) return false;
                                        if (ret == int.class || ret == float.class || ret == double.class) return 0;
                                        return null;
                                    }
                            );
                        }
                        return val;
                    }
                    if ("stream".equals(name)) return Stream.empty();
                    if ("keyStream".equals(name)) return Stream.empty();
                    if ("iterator".equals(name)) return Collections.emptyIterator();
                    if ("size".equals(name)) return 0;
                    if ("getTags".equals(name)) return Collections.emptyList();
                    if ("hasTag".equals(name)) return false;
                    if ("equals".equals(name)) return proxy == args[0];
                    if ("hashCode".equals(name)) return 42;
                    if ("toString".equals(name)) return "MockRegistry";
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) return false;
                    if (returnType == int.class || returnType == short.class || returnType == float.class || returnType == double.class) return 0;
                    return null;
                }
        );
    }

    private static final Registry<ItemType> ITEM_REGISTRY = createMockRegistry(ItemType.class, MockRegistryAccess::findItemType);
    private static final Registry<BlockType> BLOCK_REGISTRY = createMockRegistry(BlockType.class, MockRegistryAccess::findBlockType);

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Keyed> @NotNull Registry<T> getRegistry(@NotNull Class<T> type) {
        if (ItemType.class.isAssignableFrom(type)) {
            return (Registry<T>) ITEM_REGISTRY;
        }
        if (BlockType.class.isAssignableFrom(type)) {
            return (Registry<T>) BLOCK_REGISTRY;
        }
        return createMockRegistry(type, k -> null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Keyed> @NotNull Registry<T> getRegistry(@NotNull RegistryKey<T> registryKey) {
        if (registryKey == RegistryKey.ITEM) {
            return (Registry<T>) ITEM_REGISTRY;
        }
        if (registryKey == RegistryKey.BLOCK) {
            return (Registry<T>) BLOCK_REGISTRY;
        }
        if (registryKey == RegistryKey.SOUND_EVENT) {
            return (Registry<T>) createMockRegistry(Sound.class, k -> null);
        }
        if (registryKey == RegistryKey.MENU || "menu".equals(registryKey.key().value())) {
            return (Registry<T>) createMockRegistry(org.bukkit.inventory.MenuType.Typed.class, k -> null);
        }
        return (Registry<T>) createMockRegistry(Keyed.class, k -> null);
    }
}
