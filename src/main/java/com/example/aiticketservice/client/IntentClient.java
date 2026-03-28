package com.example.aiticketservice.client;

public interface IntentClient {

    /**
     * 分析用户自然语言，返回结构化意图（由 Mock 或 Qwen 适配层实现）。
     */
    IntentResult analyze(String text);

    /**
     * 兼容旧调用方：等价于 {@link #analyze(String)} 的意图枚举。
     */
    default IntentType detectIntent(String text) {
        return analyze(text).getIntent();
    }
}
