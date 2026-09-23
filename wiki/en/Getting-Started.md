# Getting Started

## 1. Install the plugin

1. Put `MultiverseProgramming.jar` in your server's `plugins/` folder.
2. Restart the server (or run `multiverse-programming` reload if your administration allows it).
3. Check `plugins/MultiverseProgramming/config.yml` — it is generated on first start.
4. Verify the plugin loaded: look for `MultiverseProgramming activado` in the console/log.

## 2. Get a floppy disk

```
/pc disco
```

You receive a **Disquete** (floppy disk) — actually a *book & quill* with a small Lua template already on the first page.

## 3. Write a program

Open the disk and write your Lua program in its pages. Every page is part of the program (pages are joined with newlines).

A simple program:

```lua
-- my first program
local mensaje = "hello from Lua"
print(mensaje)
```

## 4. Create / open a computer

Place a **lectern** and **right‑click** it. The computer GUI opens.

> The block is configurable in `config.yml` (`computer-block`). If you change it, the new block type becomes the computer.

## 5. Validate

- Put the disk in **slot 0** (the leftmost empty slot).
- Click the green **`✔ Validar código`** button (the emerald, slot 8).

Two outcomes:

- **Syntax error** → the GUI closes and the Lua error (with line number) appears in red in your chat.
- **Valid code** → the game confirms the code is valid and tells you to run it.

## 6. Run

Hold the disk in your main hand and type:

```
/pc run
```

The program executes **in the sandbox**. All `print()` output is shown in your chat. If the program produces no output you will see `(sin salida)`.

## Common mistakes

- Places the disk in the wrong slot → only slot 0 accepts disks.
- Forgets to run: the validate button **only compiles**, it does not execute.
- Runs without holding a disk → the command tells you to hold a **Disquete**.
- Infinite loop → the program is interrupted after `execution-timeout-ms` (default 3000).