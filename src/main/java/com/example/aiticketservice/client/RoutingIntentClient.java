package com.example.aiticketservice.client;

import com.example.aiticketservice.client.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * 根据 app.ai.provider 选择 Mock 或 Qwen，并在 Qwen 失败时按 app.ai.fallback 降级。
 */
@Service
@Primary
public class RoutingIntentClient implements IntentClient {

    private static final Logger log = LoggerFactory.getLogger(RoutingIntentClient.class);

    private final MockIntentClient mockIntentClient;
    private final QwenIntentClient qwenIntentClient;
    private final AppProperties appProperties;

    public RoutingIntentClient(MockIntentClient mockIntentClient,
                               QwenIntentClient qwenIntentClient,
                               AppProperties appProperties) {
        this.mockIntentClient = mockIntentClient;
        this.qwenIntentClient = qwenIntentClient;
        this.appProperties = appProperties;
    }

    @Override
    public IntentResult analyze(String text) {
        if (isMockProvider()) {
            log.debug("Using mock intent provider");
            return mockIntentClient.analyze(text);
        }
        try {
            log.debug("Using qwen intent provider");
            return qwenIntentClient.analyze(text);
        } catch (Exception e) {
            return handleFallback(text, e);
        }
    }

    private boolean isMockProvider() {
        String p = appProperties.getAi().getProvider();
        if (p == null || p.isBlank()) {
            return true;
        }
        if ("mock".equalsIgnoreCase(p)) {
            return true;
        }
        if (!"qwen".equalsIgnoreCase(p)) {
            log.warn("Unknown app.ai.provider={}, fallback to mock", p);
            return true;
        }
        return false;
    }

    private IntentResult handleFallback(String text, Exception e) {
        String fb = appProperties.getAi().getFallback();
        if (fb == null || fb.isBlank() || "mock".equalsIgnoreCase(fb)) {
            log.warn("Qwen provider failed, fallback to mock: {}", e.getMessage());
            return mockIntentClient.analyze(text);
        }
        if ("unknown".equalsIgnoreCase(fb)) {
            log.warn("Qwen provider failed, fallback to UNKNOWN: {}", e.getMessage());
            return IntentResult.unknown();
        }
        if ("none".equalsIgnoreCase(fb)) {
            log.error("Qwen provider failed and fallback is disabled", e);
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException(e.getMessage(), e);
        }
        log.warn("Unknown app.ai.fallback={}, fallback to mock", fb);
        return mockIntentClient.analyze(text);
    }
}
