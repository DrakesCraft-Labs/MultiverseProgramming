// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public final class MultiverseProgrammingPlugin extends JavaPlugin {

    private long timeoutMs;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        Material bloque = Material.matchMaterial(getConfig().getString("computer-block", "LECTERN"));
        if (bloque == null) {
            bloque = Material.LECTERN;
        }
        timeoutMs = getConfig().getLong("execution-timeout-ms", 3000L);

        getServer().getPluginManager().registerEvents(new ComputerListener(this, bloque), this);
        getCommand("pc").setExecutor(new ComputerCommand(this));
        getCommand("pc").setTabCompleter(new ComputerCommand(this));

        registrarRecetaDisco();

        getLogger().info("MultiverseProgramming enabled (block: " + bloque.name() + ").");
    }

    @Override
    public void onDisable() {
        getLogger().info("MultiverseProgramming disabled.");
    }

    public String getPrefijo() {
        return "§8[§bComputer§8]";
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    private void registrarRecetaDisco() {
        NamespacedKey key = new NamespacedKey(this, "floppy_disk");
        ShapedRecipe receta = new ShapedRecipe(key, DiskManager.crearDisquete());
        receta.shape(" P ", "PIP", " P ");
        receta.setIngredient('P', Material.PAPER);
        receta.setIngredient('I', Material.IRON_INGOT);
        getServer().addRecipe(receta);
    }
}