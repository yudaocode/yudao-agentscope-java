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
package io.agentscope.core.rag.integration.bailian;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Verifies the Bailian SDK's resolved crypto provider against the runtime JDK API.
 *
 * @author 芋道源码
 */
class BouncyCastleJava8RuntimeTest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void authenticatedEncryptionConsumesHeapAndDirectBuffers(boolean direct)
            throws GeneralSecurityException {
        byte[] aad = {1, 2, 3};
        byte[] plain = {4, 5, 6, 7};
        SecretKeySpec key = new SecretKeySpec(new byte[16], "AES");
        GCMParameterSpec parameters = new GCMParameterSpec(128, new byte[12]);
        Cipher encrypt = Cipher.getInstance("AES/GCM/NoPadding", new BouncyCastleProvider());
        encrypt.init(Cipher.ENCRYPT_MODE, key, parameters);
        ByteBuffer buffer =
                direct ? ByteBuffer.allocateDirect(aad.length) : ByteBuffer.allocate(aad.length);
        buffer.put(aad);
        buffer.flip();
        encrypt.updateAAD(buffer);
        assertEquals(buffer.limit(), buffer.position());
        byte[] encrypted = encrypt.doFinal(plain);

        Cipher decrypt = Cipher.getInstance("AES/GCM/NoPadding", new BouncyCastleProvider());
        decrypt.init(Cipher.DECRYPT_MODE, key, parameters);
        decrypt.updateAAD(aad);
        assertArrayEquals(plain, decrypt.doFinal(encrypted));
    }
}
