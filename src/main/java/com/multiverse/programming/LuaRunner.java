// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class LuaRunner {

    private LuaRunner() {
    }

    public record Result(boolean ok, String output) {
    }

    public static String validate(String code) {
        if (code == null || code.trim().isEmpty()) {
            return "The disk is empty.";
        }
        try {
            sandbox().load(code);
            return null;
        } catch (LuaError e) {
            return e.getMessage();
        }
    }

    public static Result execute(String code, long timeoutMs) {
        Globals globals = sandbox();
        StringBuilder output = new StringBuilder();
        globals.set("print", newPrint(output));

        try {
            LuaValue chunk = globals.load(code);
            AtomicBoolean ok = new AtomicBoolean(true);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<?> future = executor.submit(() -> {
                try {
                    chunk.call();
                } catch (LuaError e) {
                    ok.set(false);
                    output.append(e.getMessage());
                } catch (StackOverflowError e) {
                    ok.set(false);
                    output.append("stack exhausted (infinite recursion?)");
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
            } finally {
                executor.shutdownNow();
            }
            return new Result(ok.get(), output.toString());
        } catch (LuaError e) {
            return new Result(false, e.getMessage());
        }
    }

    public static LuaProgram runStreaming(String code, long timeoutMs, Consumer<String> onLine) {
        Globals globals = sandbox();
        BufferedSink sink = new BufferedSink(onLine);
        globals.set("print", streamingPrint(sink));

        ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "luaj-run"));
        AtomicBoolean done = new AtomicBoolean(false);

        executor.submit(() -> {
            try {
                try {
                    LuaValue chunk = globals.load(code);
                    chunk.call();
                } catch (LuaError e) {
                    sink.send(e.getMessage());
                } catch (StackOverflowError e) {
                    sink.send("stack exhausted (infinite recursion?)");
                }
            } finally {
                done.set(true);
                sink.flush();
            }
        });

        Timer timer = null;
        if (timeoutMs > 0) {
            timer = new Timer(timeoutMs, () -> {
                executor.shutdownNow();
                sink.send("<execution interrupted: exceeds " + timeoutMs + " ms>");
                sink.flush();
            });
            timer.start();
        }

        return new LuaProgram(executor, timer, sink, done);
    }

    private static Globals sandbox() {
        Globals globals = JsePlatform.standardGlobals();
        globals.set("io", LuaValue.NIL);
        globals.set("os", LuaValue.NIL);
        globals.set("luajava", LuaValue.NIL);
        globals.set("package", LuaValue.NIL);
        globals.set("dofile", LuaValue.NIL);
        globals.set("loadfile", LuaValue.NIL);
        return globals;
    }

    private static VarArgFunction newPrint(StringBuilder output) {
        return new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                StringBuilder line = new StringBuilder();
                for (int i = 1; i <= args.narg(); i++) {
                    if (i > 1) {
                        line.append('\t');
                    }
                    line.append(args.arg(i).tojstring());
                }
                output.append(line).append('\n');
                return LuaValue.NONE;
            }
        };
    }

    private static VarArgFunction streamingPrint(BufferedSink sink) {
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
                sink.send(line.toString());
                return LuaValue.NONE;
            }
        };
    }

    public static final class LuaProgram {

        private final ExecutorService executor;
        private final Timer timer;
        private final BufferedSink sink;
        private final AtomicBoolean done;

        private LuaProgram(ExecutorService executor, Timer timer, BufferedSink sink, AtomicBoolean done) {
            this.executor = executor;
            this.timer = timer;
            this.sink = sink;
            this.done = done;
        }

        public boolean isRunning() {
            return !done.get();
        }

        public void cancel() {
            executor.shutdownNow();
            if (timer != null) {
                timer.stop();
            }
            sink.close();
        }
    }

    private static final class BufferedSink {

        private final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
        private final Consumer<String> onLine;
        private final ExecutorService flusher;
        private volatile boolean closed;

        private BufferedSink(Consumer<String> onLine) {
            this.onLine = onLine;
            this.flusher = Executors.newSingleThreadExecutor(r -> new Thread(r, "luaj-output-sink"));
            flusher.submit(this::flushLoop);
        }

        private void flushLoop() {
            while (true) {
                try {
                    drainTo(onLine);
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

        private void drainTo(Consumer<String> consumer) {
            String line;
            while ((line = queue.poll()) != null) {
                consumer.accept(line);
            }
        }

        public void send(String line) {
            queue.add(line);
        }

        public void flush() {
            drainTo(onLine);
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