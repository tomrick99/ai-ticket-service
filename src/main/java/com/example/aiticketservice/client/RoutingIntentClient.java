package com.example.aiticketservice.client;

import com.example.aiticketservice.client.config.AppProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * 根据 app.ai.provider 选择 Mock 或 Qwen，并在 Qwen 失败时按 app.ai.fallback 降级。
 */
@Service
@Primary
public class RoutingIntentClient implements IntentClient {

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
            return mockIntentClient.analyze(text);
        }
        try {
            return qwenIntentClient.analyze(text);
        } catch (Exception e) {
            return handleFallback(text, e);
        }
    }

    private boolean isMockProvider() {
        String p = appProperties.getAi().getProvider();
        return p == null || p.isBlank() || "mock".equalsIgnoreCase(p);
    }

    private IntentResult handleFallback(String text, Exception e) {
        String fb = appProperties.getAi().getFallback();
        if (fb == null || fb.isBlank() || "mock".equalsIgnoreCase(fb)) {
            return mockIntentClient.analyze(text);
        }
        if ("unknown".equalsIgnoreCase(fb)) {
            return IntentResult.unknown();
        }
        if ("none".equalsIgnoreCase(fb)) {
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException(e.getMessage(), e);
        }
        return mockIntentClient.analyze(text);
    }
}
