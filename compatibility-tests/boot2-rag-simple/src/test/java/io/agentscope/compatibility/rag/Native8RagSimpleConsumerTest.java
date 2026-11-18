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
package io.agentscope.compatibility.rag;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.agentscope.core.rag.reader.PDFReader;
import io.agentscope.core.rag.reader.ReaderInput;
import io.agentscope.core.rag.reader.TikaReader;
import io.agentscope.core.rag.store.ElasticsearchStore;
import io.agentscope.core.rag.store.dto.SearchDocumentDto;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifies document parsing through the installed RAG extension on Java 8 and Boot 2.7.
 *
 * @author 芋道源码
 */
@Timeout(30)
class Native8RagSimpleConsumerTest {

    @TempDir
    Path temp;

    @Test
    void readsGeneratedPdfWithBothReaderEntryPoints() throws Exception {
        assertThat(System.getProperty("java.specification.version")).isEqualTo("1.8");
        String text = "AgentScope Java 8 PDF fixture";
        Path file = temp.resolve("portable.pdf");
        try (PDDocument pdf = new PDDocument()) {
            PDPage page = new PDPage();
            pdf.addPage(page);
            try (PDPageContentStream output = new PDPageContentStream(pdf, page)) {
                output.beginText();
                output.setFont(PDType1Font.HELVETICA, 12);
                output.newLineAtOffset(35, 700);
                output.showText(text);
                output.endText();
            }
            pdf.save(file.toFile());
        }
        ReaderInput input = ReaderInput.fromPath(file);
        Duration timeout = Duration.ofSeconds(10);
        assertThat(new PDFReader().read(input).block(timeout))
                .anySatisfy(doc -> assertThat(doc.getMetadata().getContentText()).contains(text));
        assertThat(new TikaReader().read(input).block(timeout))
                .anySatisfy(doc -> assertThat(doc.getMetadata().getContentText()).contains(text));
    }
    @Test
    void searchesThroughTheInstalledElasticsearchClient() throws Exception {
        AtomicReference<JsonNode> request = new AtomicReference<>();
        ObjectMapper mapper = new ObjectMapper();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/fixture-index",
                exchange -> {
                    try {
                        exchange.getResponseHeaders().set("X-Elastic-Product", "Elasticsearch");
                        if ("HEAD".equals(exchange.getRequestMethod())) {
                            exchange.sendResponseHeaders(200, -1);
                            return;
                        }
                        request.set(mapper.readTree(exchange.getRequestBody()));
                        byte[] body =
                                ("{\"took\":1,\"timed_out\":false,"
                                                + "\"_shards\":{\"total\":1,\"successful\":1,\"skipped\":0,\"failed\":0},"
                                                + "\"hits\":{\"max_score\":null,\"hits\":[]}}")
                                        .getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "application/json");
                        exchange.sendResponseHeaders(200, body.length);
                        try (OutputStream output = exchange.getResponseBody()) {
                            output.write(body);
                        }
                    } finally {
                        exchange.close();
                    }
                });
        server.start();
        try (ElasticsearchStore store =
                ElasticsearchStore.builder()
                        .url("http://127.0.0.1:" + server.getAddress().getPort())
                        .indexName("fixture-index")
                        .dimensions(2)
                        .build()) {
            SearchDocumentDto query =
                    SearchDocumentDto.builder()
                            .queryEmbedding(new double[] {0.2, 0.8})
                            .limit(1)
                            .build();
            assertThat(store.search(query).block(Duration.ofSeconds(10))).isEmpty();
            assertThat(request.get()).isNotNull();
            JsonNode knn = request.get().path("knn");
            assertThat(knn.isArray()).isTrue();
            assertThat(knn.size()).isEqualTo(1);
            assertThat(knn.get(0).path("k").asInt()).isEqualTo(1);
        } finally {
            server.stop(0);
        }
    }

}
