// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.peripheral;

import org.bukkit.Location;
import org.luaj.vm2.LuaValue;

/**
 * Common interface for all peripherals that can be attached to a computer.
 */
public interface Peripheral {

    /**
     * Type identifier of the peripheral (e.g. "monitor", "crafter", "transposer", "speaker").
     */
    String getType();

    /**
     * In-game location of the peripheral block.
     */
    Location getLocation();

    /**
     * Converts the peripheral into an operable Lua table with methods.
     */
    LuaValue toLuaTable();
}
