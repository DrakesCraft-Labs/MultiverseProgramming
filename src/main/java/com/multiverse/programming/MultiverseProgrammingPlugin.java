// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming;

import org.bukkit.Material;
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

        getLogger().info("MultiverseProgramming activado (bloque: " + bloque.name() + ").");
    }

    @Override
    public void onDisable() {
        getLogger().info("MultiverseProgramming desactivado.");
    }

    public String getPrefijo() {
        return "§8[§bComputadora§8]";
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }
}