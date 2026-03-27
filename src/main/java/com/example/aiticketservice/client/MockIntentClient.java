package com.example.aiticketservice.client;

import org.springframework.stereotype.Component;

/**
 * 模拟外部 AI 意图识别服务。
 * 这里只按关键词做最小可运行判断，后续可替换成真实大模型调用。
 */
@Component
public class MockIntentClient implements IntentClient {
    @Override
    public IntentType detectIntent(String text) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.contains("创建") || normalized.contains("新建")) {
            return IntentType.CREATE_TICKET;
        }
        if (normalized.contains("查询") || normalized.contains("查看")) {
            return IntentType.QUERY_TICKET;
        }
        if (normalized.contains("关闭") || normalized.contains("结束")) {
            return IntentType.CLOSE_TICKET;
        }
        return IntentType.UNKNOWN;
    }
}
