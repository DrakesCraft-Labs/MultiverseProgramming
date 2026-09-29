// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.turtle;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Dedicated InventoryHolder for the Turtle Access Control GUI.
 * Prevents GUI cross-talk and event collision with other plugin inventories.
 */
public final class TurtleAccessHolder implements InventoryHolder {

    private final Turtle turtle;
    private Inventory inventory;

    public TurtleAccessHolder(Turtle turtle) {
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
