// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.peripheral;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.luaj.vm2.LuaError;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Thread-safe dispatcher that allows Lua scripts executing on async threads
 * to safely call Bukkit API methods on the server's primary thread.
 */
public final class SyncDispatcher {

    private static final long DEFAULT_TIMEOUT_MS = 1500L;

    private SyncDispatcher() {
    }

    /**
     * Executes a callable on the Bukkit main thread and returns its result.
     * If the current thread is already the primary thread, runs synchronously.
     */
    public static <T> T sync(JavaPlugin plugin, Callable<T> callable) {
        if (Bukkit.isPrimaryThread() || Bukkit.getScheduler() == null) {
            try {
                return callable.call();
            } catch (LuaError e) {
                throw e;
            } catch (Exception e) {
                throw new LuaError(e.getMessage());
            }
        }

        if (plugin == null || !plugin.isEnabled()) {
            throw new LuaError("Server plugin is not enabled");
        }

        Future<T> future = Bukkit.getScheduler().callSyncMethod(plugin, callable);
        try {
            return future.get(DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new LuaError("execution interrupted while waiting for server");
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new LuaError("peripheral timed out waiting for server tick");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof LuaError le) {
                throw le;
            }
            throw new LuaError(cause != null ? cause.getMessage() : e.getMessage());
        }
    }

    /**
     * Executes a runnable on the Bukkit main thread.
     */
    public static void syncVoid(JavaPlugin plugin, Runnable runnable) {
        sync(plugin, () -> {
            runnable.run();
            return null;
        });
    }
}
