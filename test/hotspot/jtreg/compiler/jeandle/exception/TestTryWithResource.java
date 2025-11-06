/*
 * Copyright (c) 2025, the Jeandle-JDK Authors. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

/**
 * @test
 * @requires os.arch=="amd64" | os.arch=="x86_64"
 * @library /test/lib /
 * @run main/othervm -XX:CompileCommand=compileonly,compiler.jeandle.exception.TestTryWithResource::testTry
 *      -Xcomp -XX:-TieredCompilation -XX:+JeandleDumpIR -XX:+UseJeandleCompiler compiler.jeandle.exception.TestTryWithResource
 */

package compiler.jeandle.exception;

import java.io.InputStream;
import java.io.IOException;

import jdk.test.lib.Asserts;
import compiler.jeandle.fileCheck.FileCheck;

public class TestTryWithResource {

    public static void main(String[] args) throws Exception {
        TrackingInputStream tis = new TrackingInputStream(
                TestTryWithResource.class.getResourceAsStream("TestTryWithResource.class"));
        Asserts.assertTrue(testTry(tis));

        String currentDir = System.getProperty("user.dir");
        java.lang.reflect.Method m = TestTryWithResource.class.getDeclaredMethod("testTry", TrackingInputStream.class);
        FileCheck fileCheck = new FileCheck(currentDir, m, false);
        fileCheck.check("invoke hotspotcc void @\"compiler_jeandle_exception_TestTryWithResource$TrackingInputStream_close_()V\"");
        fileCheck.checkNext("to label");

        fileCheck.check("invoke hotspotcc void @\"java_lang_Throwable_addSuppressed_(Ljava_lang_Throwable;)V\"");
        fileCheck.checkNext("to label");

    }

    static final class TrackingInputStream extends InputStream {
        private final InputStream delegate;
        private volatile boolean closed;

        TrackingInputStream(InputStream delegate) {
            this.delegate = delegate;
        }

        public boolean isClosed() {
            return closed;
        }

        @Override
        public int read() throws IOException {
            return delegate.read();
        }

        @Override
        public void close() throws IOException {
            closed = true;
            delegate.close();
        }
    }

    static boolean testTry(TrackingInputStream tis) {
        try (tis;) {
            System.out.println(tis.isClosed());
            while (tis.skip(100) == 0) {
                Asserts.assertFalse(tis.isClosed());
                justThrow();
            }
        } catch (Exception io) {
            Asserts.assertTrue(testTry(tis));
            System.out.println(tis.isClosed());
        }
        return tis.isClosed();
    }

    static void justThrow() throws Exception {
        throw new Exception("Expected Exception");
    }
}
