package com.example.aiticketservice.client.qwen;

import com.example.aiticketservice.client.IntentResult;
import com.example.aiticketservice.client.IntentType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QwenResultParserTest {

    private final QwenResultParser parser = new QwenResultParser(new ObjectMapper());

    @Test
    void parseAssistantContent_invalidJson_returnsUnknown() {
        IntentResult r = parser.parseAssistantContent("this is not json");
        assertThat(r.getIntent()).isEqualTo(IntentType.UNKNOWN);
    }

    @Test
    void parseAssistantContent_validJson_mapsFields() {
        String json = """
                {"intent":"QUERY_TICKET","ticketId":7,"title":null,"description":null}
                """;
        IntentResult r = parser.parseAssistantContent(json);
        assertThat(r.getIntent()).isEqualTo(IntentType.QUERY_TICKET);
        assertThat(r.getTicketId()).isEqualTo(7L);
    }

    @Test
    void parseAssistantContent_stripsMarkdownFence() {
        String wrapped = """
                ```json
                {"intent":"CREATE_TICKET","ticketId":null,"title":"T","description":"D"}
                ```
                """;
        IntentResult r = parser.parseAssistantContent(wrapped);
        assertThat(r.getIntent()).isEqualTo(IntentType.CREATE_TICKET);
        assertThat(r.getTitle()).isEqualTo("T");
        assertThat(r.getDescription()).isEqualTo("D");
    }
}
