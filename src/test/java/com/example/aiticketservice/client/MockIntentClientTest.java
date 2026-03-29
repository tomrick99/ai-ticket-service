package com.example.aiticketservice.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockIntentClientTest {

    private final MockIntentClient client = new MockIntentClient();

    @Test
    void recognizeChineseCreateIntent() {
        IntentResult result = client.analyze("请帮我创建一个测试工单");
        assertThat(result.getIntent()).isEqualTo(IntentType.CREATE_TICKET);
    }

    @Test
    void recognizeChineseQueryIntentAndExtractTicketId() {
        IntentResult result = client.analyze("请帮我查询工单 123");
        assertThat(result.getIntent()).isEqualTo(IntentType.QUERY_TICKET);
        assertThat(result.getTicketId()).isEqualTo(123L);
    }

    @Test
    void recognizeChineseCloseIntentAndExtractTicketId() {
        IntentResult result = client.analyze("请帮我关闭工单 456");
        assertThat(result.getIntent()).isEqualTo(IntentType.CLOSE_TICKET);
        assertThat(result.getTicketId()).isEqualTo(456L);
    }
}
