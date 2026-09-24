// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class LuaRunnerTest {

    @Test
    @DisplayName("Validate returns null for valid syntax")
    void testValidateValidCode() {
        String code = "local x = 10\nlocal y = 20\nprint(x + y)";
        assertNull(LuaRunner.validate(code));
    }

    @Test
    @DisplayName("Validate returns error for syntax errors")
    void testValidateSyntaxError() {
        String code = "for i = 1, 10 do\nprint(i)"; // Missing 'end'
        String error = LuaRunner.validate(code);
        assertNotNull(error);
        assertTrue(error.contains("'end' expected"));
    }

    @Test
    @DisplayName("Validate flags empty disk")
    void testValidateEmpty() {
        assertEquals("The disk is empty.", LuaRunner.validate(""));
        assertEquals("The disk is empty.", LuaRunner.validate("   \n\t  "));
        assertEquals("The disk is empty.", LuaRunner.validate(null));
    }

    @Test
    @DisplayName("Validate rejects binary bytecode")
    void testValidateBinaryBytecode() {
        String binary = "\033Lua\082\000\001\004\008\004\008\000";
        assertEquals("Binary bytecode is not allowed.", LuaRunner.validate(binary));
    }

    @Test
    @DisplayName("Execute runs valid code and captures print output")
    void testExecuteSuccess() {
        String code = "print('Hello from Lua')\nprint(1 + 2)";
        LuaRunner.Result result = LuaRunner.execute(code, 3000);
        assertTrue(result.ok());
        assertEquals("Hello from Lua\n3\n", result.output());
    }

    @Test
    @DisplayName("Execute handles runtime errors cleanly without throwing unhandled exceptions")
    void testExecuteRuntimeError() {
        String code = "local a = nil\na:foo()";
        LuaRunner.Result result = LuaRunner.execute(code, 3000);
        assertFalse(result.ok());
        assertTrue(result.output().contains("attempt to index") || result.output().contains("nil"));
    }

    @Test
    @DisplayName("Execute catches StackOverflowError from infinite recursion")
    void testExecuteStackOverflow() {
        String code = "function rec() rec() end\nrec()";
        LuaRunner.Result result = LuaRunner.execute(code, 3000);
        assertFalse(result.ok());
        assertTrue(result.output().contains("stack exhausted"));
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Execute halts infinite loop without print via instruction interrupt hook")
    void testExecuteInfiniteLoopWithoutPrint() {
        // A pure CPU loop with no function calls:
        String code = "while true do end";
        long start = System.currentTimeMillis();
        LuaRunner.Result result = LuaRunner.execute(code, 300);
        long elapsed = System.currentTimeMillis() - start;

        assertFalse(result.ok());
        assertTrue(result.output().contains("<execution interrupted: exceeds 300 ms>"));
        // Ensure it finished around the 300ms mark and didn't hang the test
        assertTrue(elapsed < 2000, "Execution took too long: " + elapsed + "ms");
    }

    @Test
    @DisplayName("Sandbox isolates dangerous globals (io, os, luajava, package, debug)")
    void testSandboxIsolation() {
        Globals globals = LuaRunner.sandbox();
        assertTrue(globals.get("io").isnil(), "io should be nil");
        assertTrue(globals.get("os").isnil(), "os should be nil");
        assertTrue(globals.get("luajava").isnil(), "luajava should be nil");
        assertTrue(globals.get("package").isnil(), "package should be nil");
        assertTrue(globals.get("dofile").isnil(), "dofile should be nil");
        assertTrue(globals.get("loadfile").isnil(), "loadfile should be nil");
        assertTrue(globals.get("debug").isnil(), "debug should be nil");

        // Safe standard libraries must remain available
        assertTrue(globals.get("string").istable(), "string library should be available");
        assertTrue(globals.get("table").istable(), "table library should be available");
        assertTrue(globals.get("math").istable(), "math library should be available");
    }

    @Test
    @DisplayName("runStreaming streams output to consumer")
    void testRunStreaming() throws InterruptedException {
        String code = "for i = 1, 3 do print('Line ' .. i) end";
        List<String> received = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        LuaRunner.LuaProgram program = LuaRunner.runStreaming(code, 3000, 1000, line -> {
            received.add(line);
            if (received.size() >= 1) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertNotNull(program);
        program.cancel();
        assertFalse(received.isEmpty());
    }

    @Test
    @DisplayName("Large computational loops run smoothly without crashing or hanging")
    void testLargeComputationalLoop() {
        String code = "local sum = 0\nfor i = 1, 50000 do sum = sum + i end\nprint('sum=' .. sum)";
        LuaRunner.Result result = LuaRunner.execute(code, 3000);
        assertTrue(result.ok());
        assertTrue(result.output().contains("sum=1250025000"));
    }

    @Test
    @DisplayName("Cooperative sleep pauses execution and resets instruction quota")
    void testSleepCooperativeYield() {
        Globals globals = LuaRunner.sandbox(1000, true);
        com.multiverse.programming.peripheral.PeripheralManager.bindAll(globals, null, null, true);

        long start = System.currentTimeMillis();
        globals.load("sleep(0.05)").call(); // 50ms
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed >= 35, "Sleep should have paused for at least ~40ms, took: " + elapsed);
    }
}
