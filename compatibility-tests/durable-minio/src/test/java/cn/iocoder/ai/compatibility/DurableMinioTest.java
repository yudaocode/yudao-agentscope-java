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

package cn.iocoder.ai.compatibility;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.agentscope.durable.DurableException;
import io.agentscope.durable.DurableJson;
import io.agentscope.durable.store.ObjectStore.Reference;
import io.agentscope.extensions.durable.objects.S3ObjectStore;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveBucketArgs;
import io.minio.RemoveObjectArgs;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Verifies the installed Durable S3 extension against a MinIO server.
 *
 * @author 芋道源码
 */
class DurableMinioTest {
    private static final String SCOPE = DurableJson.hash("minio-fixture");
    private static final byte[] CONTENT = "芋道 Durable 对象".getBytes(StandardCharsets.UTF_8);

    @Test
    void readsTheSameObjectAfterRecreatingTheClient() throws Exception {
        withBucket(
                (client, bucket) -> {
                    S3ObjectStore store = new S3ObjectStore("fixture", bucket, client);
                    Reference reference = store.put(SCOPE, CONTENT);
                    try {
                        assertEquals(reference, store.put(SCOPE, CONTENT));
                        try (MinioClient secondClient = client()) {
                            assertArrayEquals(
                                    CONTENT,
                                    new S3ObjectStore("fixture", bucket, secondClient)
                                            .read(reference));
                        }
                    } finally {
                        remove(client, bucket, reference);
                    }
                });
    }

    @Test
    void rejectsCorruptionAndNeverOverwritesAnExistingKey() throws Exception {
        withBucket(
                (client, bucket) -> {
                    S3ObjectStore store = new S3ObjectStore("fixture", bucket, client);
                    Reference reference = store.put(SCOPE, CONTENT);
                    try {
                        // Write through the fixture SDK to simulate corruption outside the
                        // application.
                        byte[] corrupt = new byte[] {1, 2, 3};
                        client.putObject(
                                PutObjectArgs.builder()
                                        .bucket(bucket)
                                        .object(reference.key())
                                        .stream(
                                                new ByteArrayInputStream(corrupt),
                                                corrupt.length,
                                                -1)
                                        .build());
                        assertThrows(DurableException.class, () -> store.read(reference));
                        assertThrows(DurableException.class, () -> store.put(SCOPE, CONTENT));
                        assertThrows(DurableException.class, () -> store.read(reference));
                    } finally {
                        remove(client, bucket, reference);
                    }
                });
    }

    private static void withBucket(BucketTest test) throws Exception {
        try (ConfigurableApplicationContext boot =
                        new SpringApplicationBuilder(Fixture.class)
                                .web(WebApplicationType.NONE)
                                .properties(
                                        "spring.main.banner-mode=off", "logging.level.root=warn")
                                .run();
                MinioClient client = client()) {
            assertEquals(required("durable.minio.boot"), SpringBootVersion.getVersion());
            assertEquals(
                    required("durable.minio.jdk"),
                    System.getProperty("java.specification.version"));
            String bucket = "durable-" + UUID.randomUUID().toString();
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            try {
                test.run(client, bucket);
            } finally {
                client.removeBucket(RemoveBucketArgs.builder().bucket(bucket).build());
            }
        }
    }

    private static MinioClient client() {
        return MinioClient.builder()
                .endpoint(required("durable.minio.endpoint"))
                .credentials(
                        required("durable.minio.access-key"), required("durable.minio.secret-key"))
                .build();
    }

    private static void remove(MinioClient client, String bucket, Reference reference)
            throws Exception {
        client.removeObject(
                RemoveObjectArgs.builder().bucket(bucket).object(reference.key()).build());
    }

    private static String required(String property) {
        String value = System.getProperty(property);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("Missing fixture property " + property);
        }
        return value;
    }

    @FunctionalInterface
    private interface BucketTest {
        void run(MinioClient client, String bucket) throws Exception;
    }

    /**
     * @author 芋道源码
     */
    @Configuration(proxyBeanMethods = false)
    public static class Fixture {}
}
