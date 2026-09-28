// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Dedicated InventoryHolder for Computer and Advanced Computer GUIs.
 * Prevents GUI cross-talk and event collision with other plugins like MultiverseNets.
 */
public final class ComputerHolder implements InventoryHolder {

    private final boolean advanced;
    private final Location location;
    private Inventory inventory;

    public ComputerHolder(boolean advanced) {
        this(advanced, null);
    }

    public ComputerHolder(boolean advanced, Location location) {
        this.advanced = advanced;
        this.location = location;
    }

    public boolean isAdvanced() {
        return advanced;
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
