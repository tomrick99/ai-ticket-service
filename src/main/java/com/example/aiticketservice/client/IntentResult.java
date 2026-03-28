package com.example.aiticketservice.client;

import java.util.Objects;

/**
 * AI 适配层输出的结构化意图结果，供业务层消费（不含 HTTP 细节）。
 */
public final class IntentResult {

    private final IntentType intent;
    private final Long ticketId;
    private final String title;
    private final String description;

    public IntentResult(IntentType intent, Long ticketId, String title, String description) {
        this.intent = Objects.requireNonNull(intent);
        this.ticketId = ticketId;
        this.title = title;
        this.description = description;
    }

    public static IntentResult unknown() {
        return new IntentResult(IntentType.UNKNOWN, null, null, null);
    }

    public IntentType getIntent() {
        return intent;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
