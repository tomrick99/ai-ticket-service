package com.example.aiticketservice.client;

import com.example.aiticketservice.client.config.AppProperties;
import com.example.aiticketservice.client.qwen.QwenChatRequest;
import com.example.aiticketservice.client.qwen.QwenChatResponse;
import com.example.aiticketservice.client.qwen.QwenResultParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Objects;

/**
 * 通义兼容模式 HTTP 调用；超时、重试、解析与非法 JSON 兜底均在本类及 {@link QwenResultParser} 完成。
 */
@Component
public class QwenIntentClient implements IntentClient {

    private static final Logger log = LoggerFactory.getLogger(QwenIntentClient.class);

    private final WebClient qwenWebClient;
    private final AppProperties appProperties;
    private final QwenResultParser qwenResultParser;

    public QwenIntentClient(WebClient qwenWebClient,
                            AppProperties appProperties,
                            QwenResultParser qwenResultParser) {
        this.qwenWebClient = qwenWebClient;
        this.appProperties = appProperties;
        this.qwenResultParser = qwenResultParser;
    }

    @Override
    public IntentResult analyze(String userText) {
        AppProperties.Qwen qwen = appProperties.getQwen();
        validateConfig(qwen);
        String apiKey = qwen.getApiKey();
        /*
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("未配置 app.qwen.api-key（可用环境变量 APP_QWEN_API_KEY）");
        }

        */
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing app.qwen.api-key or DASHSCOPE_API_KEY");
        }
        QwenChatRequest req = buildRequest(userText);
        int maxAttempts = Math.max(1, qwen.getRetry().getMaxAttempts());
        long delayMs = Math.max(0L, qwen.getRetry().getDelayMs());
        Duration readTimeout = qwen.resolveReadTimeout();

        Exception last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                QwenChatResponse response = Objects.requireNonNull(qwenWebClient.post()
                        .uri("/chat/completions")
                        .header("Authorization", "Bearer " + apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(req)
                        .retrieve()
                        .bodyToMono(QwenChatResponse.class)
                        .block(readTimeout));

                String assistantContent = extractAssistantContent(response);
                return qwenResultParser.parseAssistantContent(assistantContent);
            } catch (WebClientResponseException ex) {
                last = ex;
                /*
                if (attempt < maxAttempts - 1 && delayMs > 0) {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("重试被中断", ie);
                    }
                }
                */
                log.warn("Qwen request failed with status={} attempt={}/{} model={} baseUrl={} key={}",
                        ex.getStatusCode().value(),
                        attempt,
                        maxAttempts,
                        qwen.getModel(),
                        qwen.getBaseUrl(),
                        maskApiKey(apiKey));
            } catch (WebClientRequestException ex) {
                last = ex;
                log.warn("Qwen request error attempt={}/{} model={} baseUrl={} message={} key={}",
                        attempt,
                        maxAttempts,
                        qwen.getModel(),
                        qwen.getBaseUrl(),
                        ex.getMessage(),
                        maskApiKey(apiKey));
            } catch (Exception ex) {
                last = ex;
                log.warn("Qwen parsing or timeout failure attempt={}/{} model={} baseUrl={} message={}",
                        attempt,
                        maxAttempts,
                        qwen.getModel(),
                        qwen.getBaseUrl(),
                        ex.getMessage());
            }

            if (attempt < maxAttempts && delayMs > 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Qwen retry interrupted", interruptedException);
                }
            }
        }
        /*
        throw new IllegalStateException("Qwen 调用在重试后仍失败", last);
        */
        throw new IllegalStateException("Qwen request failed after retries", last);
    }

    private QwenChatRequest buildRequest(String userText) {
        QwenChatRequest req = new QwenChatRequest();
        req.setModel(appProperties.getQwen().getModel());
        req.getMessages().add(new QwenChatRequest.Message("system", QwenResultParser.systemPrompt()));
        req.getMessages().add(new QwenChatRequest.Message("user", userText == null ? "" : userText));
        return req;
    }

    private void validateConfig(AppProperties.Qwen qwen) {
        if (isBlank(qwen.getApiKey())) {
            throw new IllegalStateException("Missing app.qwen.api-key or DASHSCOPE_API_KEY");
        }
        if (isBlank(qwen.getBaseUrl())) {
            throw new IllegalStateException("Missing app.qwen.base-url or QWEN_BASE_URL");
        }
        if (isBlank(qwen.getModel())) {
            throw new IllegalStateException("Missing app.qwen.model or QWEN_MODEL");
        }
    }

    private String extractAssistantContent(QwenChatResponse resp) {
        if (resp.getChoices() == null || resp.getChoices().isEmpty()) {
            return "";
        }
        QwenChatResponse.Message msg = resp.getChoices().get(0).getMessage();
        return msg != null ? msg.getContent() : "";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return "<empty>";
        }
        if (apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
