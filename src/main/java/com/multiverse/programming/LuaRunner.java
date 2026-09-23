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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LuaRunner {

    private LuaRunner() {
    }

    public record Resultado(boolean ok, String salida) {
    }

    public static String validar(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            return "El disquete está vacío.";
        }
        try {
            sandbox().load(codigo);
            return null;
        } catch (LuaError e) {
            return e.getMessage();
        }
    }

    public static Resultado ejecutar(String codigo, long timeoutMs) {
        Globals globals = sandbox();
        StringBuilder salida = new StringBuilder();
        globals.set("print", nuevoPrint(salida));

        try {
            LuaValue chunk = globals.load(codigo);
            AtomicBoolean ok = new AtomicBoolean(true);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<?> future = executor.submit(() -> {
                try {
                    chunk.call();
                } catch (LuaError e) {
                    ok.set(false);
                    salida.append(e.getMessage());
                } catch (StackOverflowError e) {
                    ok.set(false);
                    salida.append("pila agotada (¿recursión infinita?)");
                }
            });
            try {
                future.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                ok.set(false);
                salida.append("<ejecución interrumpida: supera ").append(timeoutMs).append(" ms>");
            } catch (InterruptedException | ExecutionException e) {
                ok.set(false);
                salida.append("error al ejecutar: ").append(e.getMessage());
            } finally {
                executor.shutdownNow();
            }
            return new Resultado(ok.get(), salida.toString());
        } catch (LuaError e) {
            return new Resultado(false, e.getMessage());
        }
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

    private static VarArgFunction nuevoPrint(StringBuilder salida) {
        return new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                StringBuilder linea = new StringBuilder();
                for (int i = 1; i <= args.narg(); i++) {
                    if (i > 1) {
                        linea.append('\t');
                    }
                    linea.append(args.arg(i).tojstring());
                }
                salida.append(linea).append('\n');
                return LuaValue.NONE;
            }
        };
    }
}