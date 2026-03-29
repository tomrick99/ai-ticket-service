package com.example.aiticketservice.client;

import com.example.aiticketservice.client.config.AppProperties;
import com.example.aiticketservice.client.qwen.QwenChatRequest;
import com.example.aiticketservice.client.qwen.QwenChatResponse;
import com.example.aiticketservice.client.qwen.QwenResultParser;
import com.example.aiticketservice.config.SensitiveLogSanitizer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Component
public class QwenIntentClient implements IntentClient {

    private static final Logger log = LoggerFactory.getLogger(QwenIntentClient.class);
    private static final int MAX_ERROR_SNIPPET_LENGTH = 240;

    private final WebClient qwenWebClient;
    private final AppProperties appProperties;
    private final QwenResultParser qwenResultParser;
    private final MeterRegistry meterRegistry;

    public QwenIntentClient(WebClient qwenWebClient,
                            AppProperties appProperties,
                            QwenResultParser qwenResultParser,
                            MeterRegistry meterRegistry) {
        this.qwenWebClient = qwenWebClient;
        this.appProperties = appProperties;
        this.qwenResultParser = qwenResultParser;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public IntentResult analyze(String userText) {
        AppProperties.Qwen qwen = appProperties.getQwen();
        validateConfig(qwen);

        String apiKey = qwen.getApiKey();
        QwenChatRequest request = buildRequest(userText);
        int maxAttempts = Math.max(1, qwen.getRetry().getMaxAttempts());
        long delayMs = Math.max(0L, qwen.getRetry().getDelayMs());
        Duration readTimeout = qwen.resolveReadTimeout();

        Exception last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            long startNanos = System.nanoTime();
            try {
                QwenChatResponse response = Objects.requireNonNull(qwenWebClient.post()
                        .uri("/chat/completions")
                        .header("Authorization", "Bearer " + apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(QwenChatResponse.class)
                        .block(readTimeout), "Qwen returned empty response");

                recordLatency("success", startNanos);
                log.info("Qwen request succeeded attempt={}/{} durationMs={} model={} baseUrl={}",
                        attempt,
                        maxAttempts,
                        elapsedMillis(startNanos),
                        qwen.getModel(),
                        qwen.getBaseUrl());
                return qwenResultParser.parseAssistantContent(extractAssistantContent(response));
            } catch (WebClientResponseException ex) {
                last = ex;
                recordLatency("http_error", startNanos);
                log.warn("Qwen request failed status={} attempt={}/{} durationMs={} model={} baseUrl={} body={} key={}",
                        ex.getStatusCode().value(),
                        attempt,
                        maxAttempts,
                        elapsedMillis(startNanos),
                        qwen.getModel(),
                        qwen.getBaseUrl(),
                        sanitizeProviderResponse(ex.getResponseBodyAsString()),
                        SensitiveLogSanitizer.maskSecret(apiKey));
            } catch (WebClientRequestException ex) {
                last = ex;
                recordLatency("request_error", startNanos);
                log.warn("Qwen request transport error attempt={}/{} durationMs={} model={} baseUrl={} message={} key={}",
                        attempt,
                        maxAttempts,
                        elapsedMillis(startNanos),
                        qwen.getModel(),
                        qwen.getBaseUrl(),
                        SensitiveLogSanitizer.sanitize(ex.getMessage()),
                        SensitiveLogSanitizer.maskSecret(apiKey));
            } catch (Exception ex) {
                last = ex;
                recordLatency("unexpected_error", startNanos);
                log.warn("Qwen request unexpected failure attempt={}/{} durationMs={} model={} baseUrl={} message={}",
                        attempt,
                        maxAttempts,
                        elapsedMillis(startNanos),
                        qwen.getModel(),
                        qwen.getBaseUrl(),
                        SensitiveLogSanitizer.sanitize(ex.getMessage()));
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

        throw new IllegalStateException("Qwen request failed after retries", last);
    }

    private QwenChatRequest buildRequest(String userText) {
        QwenChatRequest request = new QwenChatRequest();
        request.setModel(appProperties.getQwen().getModel());
        request.getMessages().add(new QwenChatRequest.Message("system", QwenResultParser.systemPrompt()));
        request.getMessages().add(new QwenChatRequest.Message("user", userText == null ? "" : userText));
        return request;
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

    private String extractAssistantContent(QwenChatResponse response) {
        if (response.getChoices() == null || response.getChoices().isEmpty()) {
            return "";
        }
        QwenChatResponse.Message message = response.getChoices().get(0).getMessage();
        return message != null ? message.getContent() : "";
    }

    private String sanitizeProviderResponse(String responseBody) {
        if (isBlank(responseBody)) {
            return "";
        }
        String sanitized = SensitiveLogSanitizer.sanitize(responseBody).replaceAll("\\s+", " ").trim();
        if (sanitized.length() <= MAX_ERROR_SNIPPET_LENGTH) {
            return sanitized;
        }
        return sanitized.substring(0, MAX_ERROR_SNIPPET_LENGTH) + "...";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private long elapsedMillis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    private void recordLatency(String outcome, long startNanos) {
        Timer.builder("app.provider.qwen.latency")
                .tag("provider", "qwen")
                .tag("outcome", outcome)
                .register(meterRegistry)
                .record(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
    }
}
