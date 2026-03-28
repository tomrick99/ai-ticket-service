package com.example.aiticketservice.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Ai ai = new Ai();
    private Qwen qwen = new Qwen();

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
        private String model = "qwen-turbo";
        private Duration timeout = Duration.ofSeconds(60);
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
}
