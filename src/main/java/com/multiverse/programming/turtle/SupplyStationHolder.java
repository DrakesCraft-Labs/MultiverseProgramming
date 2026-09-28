// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.turtle;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Dedicated InventoryHolder for Supply Station Terminal GUIs.
 * Prevents GUI cross-talk and event collision with other plugins like MultiverseNets.
 */
public final class SupplyStationHolder implements InventoryHolder {

    private final Turtle turtle;
    private Inventory inventory;

    public SupplyStationHolder(Turtle turtle) {
        this.turtle = turtle;
    }

    public Turtle getTurtle() {
        return turtle;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
