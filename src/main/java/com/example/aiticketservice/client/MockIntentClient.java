package com.example.aiticketservice.client;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 本地关键词模拟 AI，无网络依赖；结构化输出与 Qwen 路径对齐。
 */
@Component
public class MockIntentClient implements IntentClient {

    private static final Pattern ID_PATTERN = Pattern.compile("(\\d+)");

    @Override
    public IntentResult analyze(String text) {
        String normalized = text == null ? "" : text.trim();
        IntentType intent;
        if (normalized.contains("创建") || normalized.contains("新建")) {
            intent = IntentType.CREATE_TICKET;
        } else if (normalized.contains("查询") || normalized.contains("查看")) {
            intent = IntentType.QUERY_TICKET;
        } else if (normalized.contains("关闭") || normalized.contains("结束")) {
            intent = IntentType.CLOSE_TICKET;
        } else {
            intent = IntentType.UNKNOWN;
        }

        Long ticketId = extractTicketId(normalized, intent);
        if (intent == IntentType.CREATE_TICKET) {
            return new IntentResult(intent, null, "AI创建工单", normalized.isEmpty() ? text : normalized);
        }
        return new IntentResult(intent, ticketId, null, null);
    }

    private Long extractTicketId(String normalized, IntentType intent) {
        if (intent != IntentType.QUERY_TICKET && intent != IntentType.CLOSE_TICKET) {
            return null;
        }
        Matcher matcher = ID_PATTERN.matcher(normalized);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        return null;
    }
}
