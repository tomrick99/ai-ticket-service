package com.example.aiticketservice.client.qwen;

import com.example.aiticketservice.client.IntentResult;
import com.example.aiticketservice.client.IntentType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * 将模型 assistant 文本解析为 {@link IntentResult}；非法结构时返回 UNKNOWN，不抛给业务层。
 */
@Component
public class QwenResultParser {

    private static final String SYSTEM_JSON_INSTRUCTION = """
            你只输出一个 JSON 对象，不要 markdown，不要其它说明。键为：
            intent（字符串，取值之一：CREATE_TICKET, QUERY_TICKET, CLOSE_TICKET, UNKNOWN）、
            ticketId（数字或 null）、
            title（字符串或 null）、
            description（字符串或 null）。
            根据用户中文指令填写。查询/关闭工单时必须从用户话里提取 ticketId 数字。""";

    private final ObjectMapper objectMapper;

    public QwenResultParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public static String systemPrompt() {
        return SYSTEM_JSON_INSTRUCTION;
    }

    public IntentResult parseAssistantContent(String raw) {
        if (raw == null || raw.isBlank()) {
            return IntentResult.unknown();
        }
        String json = stripMarkdownFence(raw.trim());
        try {
            JsonNode node = objectMapper.readTree(json);
            if (!node.isObject()) {
                return IntentResult.unknown();
            }
            IntentType intent = parseIntent(node.path("intent").asText(null));
            Long ticketId = parseLong(node.get("ticketId"));
            String title = textOrNull(node.get("title"));
            String description = textOrNull(node.get("description"));
            return new IntentResult(intent, ticketId, title, description);
        } catch (Exception e) {
            return IntentResult.unknown();
        }
    }

    private static String stripMarkdownFence(String s) {
        String t = s;
        if (t.startsWith("```")) {
            int firstNl = t.indexOf('\n');
            if (firstNl > 0) {
                t = t.substring(firstNl + 1);
            }
            int end = t.lastIndexOf("```");
            if (end > 0) {
                t = t.substring(0, end);
            }
        }
        return t.trim();
    }

    private static IntentType parseIntent(String s) {
        if (s == null || s.isBlank()) {
            return IntentType.UNKNOWN;
        }
        try {
            return IntentType.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return IntentType.UNKNOWN;
        }
    }

    private static Long parseLong(JsonNode n) {
        if (n == null || n.isNull() || n.isMissingNode()) {
            return null;
        }
        if (n.isNumber()) {
            return n.longValue();
        }
        if (n.isTextual()) {
            try {
                return Long.parseLong(n.asText().trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static String textOrNull(JsonNode n) {
        if (n == null || n.isNull() || n.isMissingNode()) {
            return null;
        }
        String t = n.asText();
        return t == null || t.isBlank() ? null : t;
    }
}
