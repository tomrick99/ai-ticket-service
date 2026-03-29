package com.example.aiticketservice.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Ai ai = new Ai();
    private Qwen qwen = new Qwen();
    private Security security = new Security();
    private Audit audit = new Audit();

    public Ai getAi() {
        return ai;
    }

    public void setAi(Ai ai) {
        this.ai = ai;
    }

    public Qwen getQwen() {
        return qwen;
    }

    public void setQwen(Qwen qwen) {
        this.qwen = qwen;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public Audit getAudit() {
        return audit;
    }

    public void setAudit(Audit audit) {
        this.audit = audit;
    }

    public static class Ai {
        /**
         * mock：仅本地关键词；qwen：调用通义兼容接口。
         */
        private String provider = "mock";
        /**
         * mock：失败后走 Mock；unknown：返回 UNKNOWN；none：抛出异常。
         */
        private String fallback = "mock";

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getFallback() {
            return fallback;
        }

        public void setFallback(String fallback) {
            this.fallback = fallback;
        }
    }

    public static class Qwen {
        private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        private String apiKey = "";
        private String model = "qwen-plus";
        private Duration timeout;
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration readTimeout = Duration.ofSeconds(60);
        private Retry retry = new Retry();

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }

        public Duration resolveConnectTimeout() {
            return connectTimeout != null ? connectTimeout : Duration.ofSeconds(5);
        }

        public Duration resolveReadTimeout() {
            if (readTimeout != null) {
                return readTimeout;
            }
            if (timeout != null) {
                return timeout;
            }
            return Duration.ofSeconds(60);
        }

        public Retry getRetry() {
            return retry;
        }

        public void setRetry(Retry retry) {
            this.retry = retry;
        }

        public static class Retry {
            private int maxAttempts = 3;
            private long delayMs = 500L;

            public int getMaxAttempts() {
                return maxAttempts;
            }

            public void setMaxAttempts(int maxAttempts) {
                this.maxAttempts = maxAttempts;
            }

            public long getDelayMs() {
                return delayMs;
            }

            public void setDelayMs(long delayMs) {
                this.delayMs = delayMs;
            }
        }
    }

    public static class Security {
        private boolean enabled = true;
        private String apiKey = "";
        private String headerName = "X-API-Key";
        private String clientIdHeader = "X-Client-Id";
        private List<String> protectedPaths = new ArrayList<>(List.of(
                "/tickets/**",
                "/ai/tickets/**",
                "/actuator/**"
        ));
        private List<String> publicPaths = new ArrayList<>(List.of(
                "/error",
                "/actuator/health",
                "/actuator/health/**"
        ));

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getHeaderName() {
            return headerName;
        }

        public void setHeaderName(String headerName) {
            this.headerName = headerName;
        }

        public String getClientIdHeader() {
            return clientIdHeader;
        }

        public void setClientIdHeader(String clientIdHeader) {
            this.clientIdHeader = clientIdHeader;
        }

        public List<String> getProtectedPaths() {
            return protectedPaths;
        }

        public void setProtectedPaths(List<String> protectedPaths) {
            this.protectedPaths = protectedPaths;
        }

        public List<String> getPublicPaths() {
            return publicPaths;
        }

        public void setPublicPaths(List<String> publicPaths) {
            this.publicPaths = publicPaths;
        }
    }

    public static class Audit {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
