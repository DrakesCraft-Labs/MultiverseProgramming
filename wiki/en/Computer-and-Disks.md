# Computer and Disks

## The computer block

By default the computer is a **lectern**. Right‑clicking it opens the computer GUI and cancels the normal lectern behavior.

To use another block as the computer, edit `config.yml`:

```yaml
computer-block: LECTERN
```

Any block `Material` name works (e.g. `BARREL`, `DISPENSER`, `JUKEBOX`). Remember to restart for config changes to take effect.

### GUI layout

The GUI is a single row of 9 slots:

```
[0] [1] [2] [3] [4] [5] [6] [7] [8]
```

- **Slot 0 — disk slot.** Only floppy disks (books) can be inserted here. Any other item is rejected with a chat message.
- **Slots 1–7 — decoration.** Black glass panes; they cannot be moved or replaced.
- **Slot 8 — validate button.** A green emerald labeled `✔ Validate Code`. Clicking it validates the disk's code.

When the inventory is closed (error or success), the disk returns to the player's inventory automatically.

## Floppy disks

Floppy disks are crafted on a crafting table (see [Recipes](Recipes.md) for the exact grid).

The item is a **book & quill** renamed to "Floppy Disk". Both unsigned and signed books are accepted by the computer.

- The program is the **sequence of pages** (pages are joined with `\n`).
- The disk is the **source of truth**: there is no "save to computer" state. If you change the disk, you change the program.
- You can have several programs by keeping several disks in your inventory.

### Recognizing a disk

Any `WRITABLE_BOOK` or `WRITTEN_BOOK` in your hand is treated as a disk for `/pc run`. Other item types are rejected.

## Lifecycle of a program

```
write code on a disk → insert disk → validate (button) → hold disk → /pc run → output in chat
```

Validation only **compiles** the code (syntax check). Execution happens only with `/pc run`.