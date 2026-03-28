package com.example.aiticketservice.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Qwen 请求失败时按 fallback=mock 回到关键词逻辑（不访问外网）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RoutingIntentFallbackIntegrationTest {

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
        registry.add("app.ai.fallback", () -> "mock");
        registry.add("app.qwen.base-url", () -> base);
        registry.add("app.qwen.api-key", () -> "test-key");
        registry.add("app.qwen.retry.max-attempts", () -> 1);
        registry.add("app.qwen.retry.delay-ms", () -> 0);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void whenQwenReturns500_fallbackMockStillRecognizesCreate() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("error"));

        mockMvc.perform(post("/ai/tickets/handle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "text", "帮我创建一个工单"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.intent").value("CREATE_TICKET"));
    }
}
