package com.example.aiticketservice.client;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MockIntentClient implements IntentClient {

    private static final Pattern ID_PATTERN = Pattern.compile("(\\d+)");

    @Override
    public IntentResult analyze(String text) {
        String normalized = text == null ? "" : text.trim();
        IntentType intent;
        if (containsAny(normalized,
                "\u521b\u5efa", "\u65b0\u5efa", "\u9352\u6d9a\u7f13", "\u93c2\u677f\u7f13", "create")) {
            intent = IntentType.CREATE_TICKET;
        } else if (containsAny(normalized,
                "\u67e5\u8be2", "\u67e5\u770b", "\u67e5\u4e0b", "\u93cc\u30e8\ue1d7", "\u93cc\u30e7\u6e45", "query")) {
            intent = IntentType.QUERY_TICKET;
        } else if (containsAny(normalized,
                "\u5173\u95ed", "\u7ed3\u675f", "\u5173\u6389", "\u9358\u5fd4\u5a0a\u59ab", "\u7f02\u4f79\u631b\u6f7c", "close")) {
            intent = IntentType.CLOSE_TICKET;
        } else {
            intent = IntentType.UNKNOWN;
        }

        Long ticketId = extractTicketId(normalized, intent);
        if (intent == IntentType.CREATE_TICKET) {
            return new IntentResult(intent, null, "AI\u521b\u5efa\u5de5\u5355", normalized.isEmpty() ? text : normalized);
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

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}