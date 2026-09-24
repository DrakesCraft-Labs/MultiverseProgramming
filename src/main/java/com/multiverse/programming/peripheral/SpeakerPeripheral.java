// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.peripheral;

import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Note;
import org.bukkit.SoundCategory;
import org.bukkit.plugin.java.JavaPlugin;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.Locale;

/**
 * Peripheral that allows playing musical notes, tones, and Minecraft sound effects.
 */
public final class SpeakerPeripheral implements Peripheral {

    private final JavaPlugin plugin;
    private final Location location;

    public SpeakerPeripheral(JavaPlugin plugin, Location location) {
        this.plugin = plugin;
        this.location = location;
    }

    @Override
    public String getType() {
        return "speaker";
    }

    @Override
    public Location getLocation() {
        return location;
    }

    private static Instrument parseInstrument(String name) {
        if (name == null) {
            return Instrument.PIANO;
        }
        return switch (name.trim().toLowerCase(Locale.ROOT)) {
            case "piano", "harp" -> Instrument.PIANO;
            case "bass", "bass_drum", "bassdrum" -> Instrument.BASS_DRUM;
            case "snare", "snare_drum" -> Instrument.SNARE_DRUM;
            case "sticks", "hat", "click" -> Instrument.STICKS;
            case "bass_guitar", "bassguitar" -> Instrument.BASS_GUITAR;
            case "flute" -> Instrument.FLUTE;
            case "bell" -> Instrument.BELL;
            case "guitar" -> Instrument.GUITAR;
            case "chime" -> Instrument.CHIME;
            case "xylophone" -> Instrument.XYLOPHONE;
            case "iron_xylophone", "iron" -> Instrument.IRON_XYLOPHONE;
            case "cow_bell", "cowbell" -> Instrument.COW_BELL;
            case "didgeridoo" -> Instrument.DIDGERIDOO;
            case "bit" -> Instrument.BIT;
            case "banjo" -> Instrument.BANJO;
            case "pling" -> Instrument.PLING;
            default -> {
                try {
                    yield Instrument.valueOf(name.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    yield Instrument.PIANO;
                }
            }
        };
    }

    @Override
    public LuaValue toLuaTable() {
        LuaTable table = new LuaTable();

        // speaker.playNote(instrument, note) -> note is integer 0-24
        table.set("playNote", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {
                Instrument inst = parseInstrument(arg1.tojstring());
                int noteVal = Math.max(0, Math.min(24, arg2.checkint()));
                SyncDispatcher.syncVoid(plugin, () -> {
                    if (location.getWorld() != null) {
                        location.getWorld().playNote(location, inst, new Note(noteVal));
                    }
                });
                return LuaValue.NONE;
            }
        });

        // speaker.playSound(soundName, [volume], [pitch])
        table.set("playSound", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String soundName = args.checkjstring(1);
                float volume = args.narg() >= 2 ? (float) args.optdouble(2, 1.0) : 1.0f;
                float pitch = args.narg() >= 3 ? (float) args.optdouble(3, 1.0) : 1.0f;

                SyncDispatcher.syncVoid(plugin, () -> {
                    if (location.getWorld() != null) {
                        location.getWorld().playSound(location, soundName, SoundCategory.BLOCKS, volume, pitch);
                    }
                });
                return LuaValue.NONE;
            }
        });

        // speaker.playTone(frequencyHz, [durationMs]) -> converts Hz to note pitch
        table.set("playTone", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                double freq = args.checkdouble(1);
                // Standard pitch conversion: 440 Hz (A4) corresponds to pitch 1.0
                float pitch = (float) Math.max(0.5, Math.min(2.0, freq / 440.0));
                SyncDispatcher.syncVoid(plugin, () -> {
                    if (location.getWorld() != null) {
                        location.getWorld().playSound(location, "block.note_block.pling", SoundCategory.BLOCKS, 1.0f, pitch);
                    }
                });
                return LuaValue.NONE;
            }
        });

        return table;
    }
}
