# Computer and Disks

## Standard Computer Block

By default, the computer is a **lectern**. Right-clicking it opens the computer GUI and cancels the vanilla lectern behavior.

To change the block material, modify `config.yml`:

```yaml
computer-block: LECTERN
enable-computer: true
enable-recipe-computer: true
```

## Advanced Computer Block

Default block: **enchanting table**. Get one with `/mvprog give advancedcomputer` or craft it (see [Recipes](Recipes.md)).

Key features:
- **Long-Running & Looping Execution**: Unrestricted runtime (`advanced-execution-timeout-ms`, default `0` for unlimited).
- **Persistent Storage**: Retains the inserted floppy disk even after closing the inventory.
- **Dynamic Run / Stop Status Indicator**:
  - **Slot 8 (Idle)**: Displays a green emerald titled `▶ Run Program`. Clicking it validates the disk and launches the program.
  - **Slot 8 (Running)**: Dynamically transforms into a glowing Redstone Block titled `⏹ Stop Program`. Clicking this instantly halts program execution.
- **Detached Stop Capability**: You can safely take out the floppy disk while the program is running, and click `⏹ Stop Program` at any time to kill the running task.

### GUI Layout

The GUI consists of a 9-slot interface:

```
[0] [1] [2] [3] [4] [5] [6] [7] [8]
```

- **Slot 0 — Disk Drive Slot**: Insert floppy disks (books) here. Other items are rejected.
- **Slots 1–7 — Status Panes**: Protected decorative border panes.
- **Slot 8 — Action Button**:
  - Standard Computer: Green emerald `✔ Validate & Run`.
  - Advanced Computer: Green emerald `▶ Run Program` or Redstone Block `⏹ Stop Program`.

---

## Floppy Disks

Floppy disks are crafted on a crafting table (see [Recipes](Recipes.md)). Disks are represented in-game as custom **Book & Quill** items preloaded with template code.

- **Storage**: The sequence of pages represents your Lua program.
- **Source of Truth**: The program is read directly from disk pages upon execution.
- **Custom Disks**: Both unsigned (`WRITABLE_BOOK`) and signed (`WRITTEN_BOOK`) books are supported.