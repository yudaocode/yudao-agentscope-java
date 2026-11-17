/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.core.compat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.junit.jupiter.api.Test;

/**
 * Verifies the Java8 implementations used by sandbox archive transfer and session IDs.
 *
 * @author 芋道源码
 */
class Java8PlatformTest {
    @Test
    void formatsOnlyTheRequestedByteRange() {
        byte[] bytes = {0, 15, (byte) 255, 1};
        assertEquals("0fff", HexFormat.of().formatHex(bytes, 1, 3));
        assertEquals("", HexFormat.of().formatHex(bytes, 2, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> HexFormat.of().formatHex(bytes, -1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> HexFormat.of().formatHex(bytes, 3, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> HexFormat.of().formatHex(bytes, 0, 5));
    }

    @Test
    void transfersAllBytesAndReturnsTheLongCount() throws IOException {
        byte[] bytes = new byte[20003];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) i;
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertEquals(
                bytes.length, Java8Platform.transferTo(new ByteArrayInputStream(bytes), output));
        assertArrayEquals(bytes, output.toByteArray());
        assertThrows(
                NullPointerException.class,
                () -> Java8Platform.transferTo(new ByteArrayInputStream(bytes), null));
    }

    @Test
    void emptyInputHonorsBoundsAndClosedState() throws IOException {
        InputStream input = Java8Platform.nullInputStream();
        assertEquals(-1, input.read());
        assertEquals(0, input.read(new byte[0]));
        assertEquals(-1, input.read(new byte[2]));
        assertEquals(0, input.skip(10));
        assertEquals(0, input.available());
        assertThrows(IndexOutOfBoundsException.class, () -> input.read(new byte[1], 1, 1));
        input.close();
        assertThrows(IOException.class, input::read);
        assertThrows(IOException.class, () -> input.read(new byte[0]));
        assertThrows(IOException.class, () -> input.skip(0));
        assertThrows(IOException.class, input::available);
    }

    @Test
    void discardOutputHonorsBoundsAndClosedState() throws IOException {
        OutputStream output = Java8Platform.nullOutputStream();
        output.write(1);
        output.write(new byte[] {1, 2}, 0, 2);
        assertThrows(IndexOutOfBoundsException.class, () -> output.write(new byte[1], 1, 1));
        output.close();
        assertThrows(IOException.class, () -> output.write(1));
        assertThrows(IOException.class, () -> output.write(new byte[0]));
    }
}
