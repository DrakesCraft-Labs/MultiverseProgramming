// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming;

import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

public final class MultiverseProgrammingPlugin extends JavaPlugin {

    private long timeoutMs;
    private long advancedTimeoutMs;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        Material block = readBlock("computer-block", Material.LECTERN);
        Material advancedBlock = readBlock("advanced-computer-block", Material.ENCHANTING_TABLE);
        timeoutMs = getConfig().getLong("execution-timeout-ms", 3000L);
        advancedTimeoutMs = getConfig().getLong("advanced-execution-timeout-ms", 0L);

        getServer().getPluginManager().registerEvents(new ComputerListener(this, block, advancedBlock), this);
        getCommand("pc").setExecutor(new ComputerCommand(this));
        getCommand("pc").setTabCompleter(new ComputerCommand(this));

        new RecipeManager(this).registerAll(block);

        getLogger().info("MultiverseProgramming enabled (computer: " + block.name()
                + ", advanced: " + advancedBlock.name() + ").");
    }

    @Override
    public void onDisable() {
        ComputerListener.cancelAll();
        getLogger().info("MultiverseProgramming disabled.");
    }

    public String getPrefix() {
        return "§8[§bComputer§8]";
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public long getAdvancedTimeoutMs() {
        return advancedTimeoutMs;
    }

    private Material readBlock(String key, Material defaultValue) {
        Material block = Material.matchMaterial(getConfig().getString(key, defaultValue.name()));
        return block != null ? block : defaultValue;
    }
}