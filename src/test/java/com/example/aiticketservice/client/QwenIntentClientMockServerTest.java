package com.example.aiticketservice.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 使用 MockWebServer 模拟通义 HTTP，不访问外网。
 */
@SpringBootTest
class QwenIntentClientMockServerTest {

    static final MockWebServer server;

    static {
        MockWebServer s = new MockWebServer();
        try {
            s.start();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
        server = s;
    }

    @AfterAll
    static void shutdown() throws IOException {
        server.shutdown();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        final String base = server.url("/").toString().replaceAll("/$", "");
        registry.add("app.ai.provider", () -> "qwen");
        registry.add("app.ai.fallback", () -> "unknown");
        registry.add("app.qwen.base-url", () -> base);
        registry.add("app.qwen.api-key", () -> "test-api-key");
        registry.add("app.qwen.retry.max-attempts", () -> 1);
        registry.add("app.qwen.retry.delay-ms", () -> 0);
    }

    @Autowired
    private QwenIntentClient qwenIntentClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void qwenReturnsValidChatCompletion_parsesStructuredIntent() throws Exception {
        String assistantContent = "{\"intent\":\"CREATE_TICKET\",\"ticketId\":null,\"title\":\"标题\",\"description\":\"描述\"}";
        String body = buildChatCompletionBody(assistantContent);

        server.enqueue(new MockResponse()
                .setBody(body)
                .addHeader("Content-Type", "application/json"));

        IntentResult r = qwenIntentClient.analyze("用户输入");
        assertThat(r.getIntent().name()).isEqualTo("CREATE_TICKET");
        assertThat(r.getTitle()).isEqualTo("标题");
        assertThat(r.getDescription()).isEqualTo("描述");
    }

    @Test
    void qwenReturnsMalformedAssistantContent_parserYieldsUnknown() throws Exception {
        String body = buildChatCompletionBody("<<<not-json>>>");

        server.enqueue(new MockResponse()
                .setBody(body)
                .addHeader("Content-Type", "application/json"));

        IntentResult r = qwenIntentClient.analyze("x");
        assertThat(r.getIntent().name()).isEqualTo("UNKNOWN");
    }

    private String buildChatCompletionBody(String assistantContent) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode choices = root.putArray("choices");
        ObjectNode choice = choices.addObject();
        ObjectNode message = choice.putObject("message");
        message.put("content", assistantContent);
        return objectMapper.writeValueAsString(root);
    }
}
