package com.example.aiticketservice.client;

import com.example.aiticketservice.client.config.AppProperties;
import com.example.aiticketservice.client.qwen.QwenChatRequest;
import com.example.aiticketservice.client.qwen.QwenChatResponse;
import com.example.aiticketservice.client.qwen.QwenResultParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 通义兼容模式 HTTP 调用；超时、重试、解析与非法 JSON 兜底均在本类及 {@link QwenResultParser} 完成。
 */
@Component
public class QwenIntentClient {

    private final WebClient qwenWebClient;
    private final AppProperties appProperties;
    private final QwenResultParser qwenResultParser;
    private final ObjectMapper objectMapper;

    public QwenIntentClient(WebClient qwenWebClient,
                            AppProperties appProperties,
                            QwenResultParser qwenResultParser,
                            ObjectMapper objectMapper) {
        this.qwenWebClient = qwenWebClient;
        this.appProperties = appProperties;
        this.qwenResultParser = qwenResultParser;
        this.objectMapper = objectMapper;
    }

    public IntentResult analyze(String userText) {
        String apiKey = appProperties.getQwen().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("未配置 app.qwen.api-key（可用环境变量 APP_QWEN_API_KEY）");
        }

        QwenChatRequest req = buildRequest(userText);
        int maxAttempts = Math.max(1, appProperties.getQwen().getRetry().getMaxAttempts());
        long delayMs = Math.max(0L, appProperties.getQwen().getRetry().getDelayMs());
        Duration timeout = appProperties.getQwen().getTimeout();

        Exception last = null;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                String rawJson = qwenWebClient.post()
                        .uri("/chat/completions")
                        .header("Authorization", "Bearer " + apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(req)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(timeout)
                        .onErrorResume(WebClientResponseException.class, ex ->
                                Mono.error(new IllegalStateException("Qwen HTTP " + ex.getStatusCode().value(), ex)))
                        .block();

                String assistantContent = extractAssistantContent(rawJson);
                return qwenResultParser.parseAssistantContent(assistantContent);
            } catch (Exception e) {
                last = e;
                if (attempt < maxAttempts - 1 && delayMs > 0) {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("重试被中断", ie);
                    }
                }
            }
        }
        throw new IllegalStateException("Qwen 调用在重试后仍失败", last);
    }

    private QwenChatRequest buildRequest(String userText) {
        QwenChatRequest req = new QwenChatRequest();
        req.setModel(appProperties.getQwen().getModel());
        req.getMessages().add(new QwenChatRequest.Message("system", QwenResultParser.systemPrompt()));
        req.getMessages().add(new QwenChatRequest.Message("user", userText == null ? "" : userText));
        return req;
    }

    private String extractAssistantContent(String rawJson) throws Exception {
        QwenChatResponse resp = objectMapper.readValue(rawJson, QwenChatResponse.class);
        if (resp.getChoices() == null || resp.getChoices().isEmpty()) {
            return "";
        }
        QwenChatResponse.Message msg = resp.getChoices().get(0).getMessage();
        return msg != null ? msg.getContent() : "";
    }
}
