// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming;

import com.multiverse.programming.peripheral.PeripheralManager;
import org.bukkit.Location;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.DebugLib;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class LuaRunner {

    public static final int DEFAULT_MAX_STREAM_LINES = 10_000;
    private static final int SINK_CAPACITY = 2_048;
    private static final int LINES_PER_FLUSH = 20;

    private static final ThreadPoolExecutor WORKER_POOL = new ThreadPoolExecutor(
            4,
            32,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(256),
            new ThreadFactory() {
                private final AtomicInteger count = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "MultiverseLua-Worker-" + count.getAndIncrement());
                    t.setDaemon(true);
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    private LuaRunner() {
    }

    public static void shutdownPool() {
        WORKER_POOL.shutdownNow();
    }

    public record Result(boolean ok, String output) {
    }

    /**
     * Subclass of Luaj's DebugLib that intercepts every bytecode instruction
     * to check if the thread was interrupted and perform cooperative yielding
     * so large algorithms don't freeze the CPU or starve Minecraft threads.
     */
    public static class InterruptHookDebugLib extends DebugLib {
        private int instructionCount = 0;
        private int instructionsSinceYield = 0;
        private final int maxSliceInstructions;
        private final boolean isAdvanced;

        public InterruptHookDebugLib() {
            this(1_000_000, true);
        }

        public InterruptHookDebugLib(int maxSliceInstructions, boolean isAdvanced) {
            this.maxSliceInstructions = maxSliceInstructions;
            this.isAdvanced = isAdvanced;
        }

        public void resetSlice() {
            instructionsSinceYield = 0;
        }

        @Override
        public void onInstruction(int pc, Varargs v, int top) {
            instructionCount++;
            instructionsSinceYield++;

            // Check interruption every 512 instructions
            if ((instructionCount & 0x1FF) == 0) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new LuaError("execution interrupted");
                }

                if (maxSliceInstructions > 0 && instructionsSinceYield > maxSliceInstructions) {
                    if (isAdvanced) {
                        try {
                            Thread.sleep(2);
                            instructionsSinceYield = 0;
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new LuaError("execution interrupted");
                        }
                    } else {
                        throw new LuaError("CPU limit reached (too long without yield or sleep)");
                    }
                }
            }
            super.onInstruction(pc, v, top);
        }
    }

    public static String validate(String code) {
        if (code == null || code.trim().isEmpty()) {
            return "The disk is empty.";
        }
        if (code.startsWith("\033") || code.startsWith("\u001b")) {
            return "Binary bytecode is not allowed.";
        }
        try {
            sandbox().load(code);
            return null;
        } catch (LuaError e) {
            return e.getMessage();
        }
    }

    public static Result execute(String code, long timeoutMs) {
        return execute(null, null, code, timeoutMs);
    }

    public static Result execute(MultiverseProgrammingPlugin plugin, Location computerLoc, String code, long timeoutMs) {
        if (code != null && (code.startsWith("\033") || code.startsWith("\u001b"))) {
            return new Result(false, "Binary bytecode is not allowed.");
        }
        Globals globals = sandbox(0, false);
        StringBuffer output = new StringBuffer();
        globals.set("print", newPrint(output));

        if (plugin != null) {
            PeripheralManager.bindAll(globals, plugin, computerLoc, false);
        }

        try {
            LuaValue chunk = globals.load(code);
            AtomicBoolean ok = new AtomicBoolean(true);
            Future<?> future = WORKER_POOL.submit(() -> {
                try {
                    chunk.call();
                } catch (LuaError e) {
                    ok.set(false);
                    output.append(e.getMessage());
                } catch (StackOverflowError e) {
                    ok.set(false);
                    output.append("stack exhausted (infinite recursion?)");
                } catch (Throwable t) {
                    ok.set(false);
                    output.append("unexpected error: ").append(t.getMessage());
                }
            });
            try {
                if (timeoutMs > 0) {
                    future.get(timeoutMs, TimeUnit.MILLISECONDS);
                } else {
                    future.get();
                }
            } catch (TimeoutException e) {
                future.cancel(true);
                ok.set(false);
                output.append("<execution interrupted: exceeds ").append(timeoutMs).append(" ms>");
            } catch (InterruptedException | ExecutionException e) {
                ok.set(false);
                output.append("error while running: ").append(e.getMessage());
            }
            return new Result(ok.get(), output.toString());
        } catch (LuaError e) {
            return new Result(false, e.getMessage());
        } catch (Throwable t) {
            return new Result(false, "error loading program: " + t.getMessage());
        }
    }

    public static LuaProgram runStreaming(String code, long timeoutMs, Consumer<String> onLine) {
        return runStreaming(null, null, true, code, timeoutMs, DEFAULT_MAX_STREAM_LINES, onLine);
    }

    public static LuaProgram runStreaming(String code, long timeoutMs, int maxLines, Consumer<String> onLine) {
        return runStreaming(null, null, true, code, timeoutMs, maxLines, onLine);
    }

    public static LuaProgram runStreaming(
            MultiverseProgrammingPlugin plugin,
            Location computerLoc,
            boolean isAdvanced,
            String code,
            long timeoutMs,
            int maxLines,
            Consumer<String> onLine
    ) {
        if (code != null && (code.startsWith("\033") || code.startsWith("\u001b"))) {
            onLine.accept("Binary bytecode is not allowed.");
            return new LuaProgram(null, null, null, new AtomicBoolean(true));
        }

        int maxSlice = plugin != null ? plugin.getConfigManager().getMaxInstructionsPerSlice() : 1_000_000;
        Globals globals = sandbox(maxSlice, isAdvanced);
        BufferedSink sink = new BufferedSink(onLine);
        globals.set("print", streamingPrint(sink, maxLines));

        if (plugin != null) {
            PeripheralManager.bindAll(globals, plugin, computerLoc, isAdvanced);
        }

        AtomicBoolean done = new AtomicBoolean(false);
        Future<?> taskFuture = WORKER_POOL.submit(() -> {
            try {
                try {
                    LuaValue chunk = globals.load(code);
                    chunk.call();
                } catch (LuaError e) {
                    sink.send(e.getMessage());
                } catch (StackOverflowError e) {
                    sink.send("stack exhausted (infinite recursion?)");
                } catch (Throwable t) {
                    sink.send("unexpected error: " + t.getMessage());
                }
            } finally {
                done.set(true);
                sink.flush();
            }
        });

        Timer timer = null;
        if (timeoutMs > 0) {
            timer = new Timer(timeoutMs, () -> {
                taskFuture.cancel(true);
                sink.send("<execution interrupted: exceeds " + timeoutMs + " ms>");
                sink.flush();
            });
            timer.start();
        }

        return new LuaProgram(taskFuture, timer, sink, done);
    }

    public static Globals sandbox() {
        return sandbox(1_000_000, true);
    }

    public static Globals sandbox(int maxSliceInstructions, boolean isAdvanced) {
        Globals globals = JsePlatform.standardGlobals();

        InterruptHookDebugLib debugLib = new InterruptHookDebugLib(maxSliceInstructions, isAdvanced);
        globals.load(debugLib);
        globals.debuglib = debugLib;

        // Sandbox dangerous libraries
        globals.set("io", LuaValue.NIL);
        globals.set("os", LuaValue.NIL);
        globals.set("luajava", LuaValue.NIL);
        globals.set("package", LuaValue.NIL);
        globals.set("dofile", LuaValue.NIL);
        globals.set("loadfile", LuaValue.NIL);
        globals.set("debug", LuaValue.NIL);

        return globals;
    }

    private static VarArgFunction newPrint(StringBuffer output) {
        return new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new LuaError("execution interrupted");
                }
                StringBuilder line = new StringBuilder();
                for (int i = 1; i <= args.narg(); i++) {
                    if (i > 1) {
                        line.append('\t');
                    }
                    line.append(args.arg(i).tojstring());
                }
                if (output.length() > 1_000_000) {
                    throw new LuaError("output limit reached");
                }
                output.append(line).append('\n');
                return LuaValue.NONE;
            }
        };
    }

    private static VarArgFunction streamingPrint(BufferedSink sink, int maxLines) {
        return new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new LuaError("execution interrupted");
                }
                if (sink.lineCount() > maxLines) {
                    throw new LuaError("output limit reached (" + maxLines + " lines)");
                }
                StringBuilder line = new StringBuilder();
                for (int i = 1; i <= args.narg(); i++) {
                    if (i > 1) {
                        line.append('\t');
                    }
                    line.append(args.arg(i).tojstring());
                }
                sink.send(line.toString());
                return LuaValue.NONE;
            }
        };
    }

    public static final class LuaProgram {

        private final Future<?> taskFuture;
        private final Timer timer;
        private final BufferedSink sink;
        private final AtomicBoolean done;

        private LuaProgram(Future<?> taskFuture, Timer timer, BufferedSink sink, AtomicBoolean done) {
            this.taskFuture = taskFuture;
            this.timer = timer;
            this.sink = sink;
            this.done = done;
        }

        public boolean isRunning() {
            return !done.get();
        }

        public void cancel() {
            if (taskFuture != null) {
                taskFuture.cancel(true);
            }
            if (timer != null) {
                timer.stop();
            }
            if (sink != null) {
                sink.close();
            }
        }
    }

    private static final class BufferedSink {

        private final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>(SINK_CAPACITY);
        private final AtomicInteger lineCount = new AtomicInteger();
        private final Consumer<String> onLine;
        private final ExecutorService flusher;
        private volatile boolean closed;

        private BufferedSink(Consumer<String> onLine) {
            this.onLine = onLine;
            this.flusher = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "luaj-output-sink");
                t.setDaemon(true);
                return t;
            });
            flusher.submit(this::flushLoop);
        }

        private void flushLoop() {
            while (true) {
                try {
                    drainTo(onLine, LINES_PER_FLUSH);
                    if (closed && queue.isEmpty()) {
                        return;
                    }
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        private void drainTo(Consumer<String> consumer, int maxLines) {
            StringBuilder batch = new StringBuilder();
            int flushed = 0;
            String line;
            while (flushed < maxLines && (line = queue.poll()) != null) {
                if (batch.length() > 0) {
                    batch.append('\n');
                }
                batch.append(line);
                flushed++;
            }
            if (batch.length() > 0) {
                consumer.accept(batch.toString());
            }
        }

        public void send(String line) {
            if (closed) {
                return;
            }
            lineCount.incrementAndGet();
            try {
                while (!queue.offer(line, 10, TimeUnit.MILLISECONDS)) {
                    if (closed) {
                        return;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        public int lineCount() {
            return lineCount.get();
        }

        public void flush() {
            drainTo(onLine, Integer.MAX_VALUE);
        }

        public void close() {
            closed = true;
            flush();
            flusher.shutdownNow();
        }
    }

    private static final class Timer {

        private final long timeoutMs;
        private final Runnable onTimeout;
        private volatile boolean cancelled;
        private volatile Thread thread;

        private Timer(long timeoutMs, Runnable onTimeout) {
            this.timeoutMs = timeoutMs;
            this.onTimeout = onTimeout;
        }

        public void start() {
            thread = new Thread(() -> {
                try {
                    Thread.sleep(timeoutMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                if (!cancelled) {
                    onTimeout.run();
                }
            }, "luaj-timeout");
            thread.setDaemon(true);
            thread.start();
        }

        public void stop() {
            cancelled = true;
            Thread t = thread;
            if (t != null) {
                t.interrupt();
            }
        }
    }
}