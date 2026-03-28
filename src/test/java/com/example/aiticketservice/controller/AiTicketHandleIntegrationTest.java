package com.example.aiticketservice.controller;

import com.example.aiticketservice.dto.TicketCreateRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AiTicketHandleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void aiCreateTicket_success() throws Exception {
        mockMvc.perform(post("/ai/tickets/handle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "text", "帮我创建一个网络中断的工单"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("AI处理完成"))
                .andExpect(jsonPath("$.data.intent").value("CREATE_TICKET"))
                .andExpect(jsonPath("$.data.result.title").value("AI创建工单"))
                .andExpect(jsonPath("$.data.result.description").value("帮我创建一个网络中断的工单"))
                .andExpect(jsonPath("$.data.result.status").value("OPEN"));
    }

    @Test
    void aiQueryTicket_success() throws Exception {
        long id = seedTicket("预置工单", "用于 AI 查询");

        mockMvc.perform(post("/ai/tickets/handle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "text", "帮我查询" + id + "号工单"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.intent").value("QUERY_TICKET"))
                .andExpect(jsonPath("$.data.result.title").value("预置工单"))
                .andExpect(jsonPath("$.data.result.id").value((int) id));
    }

    @Test
    void aiCloseTicket_success() throws Exception {
        long id = seedTicket("待 AI 关闭", "描述");

        mockMvc.perform(post("/ai/tickets/handle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "text", "把" + id + "号工单关闭"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.intent").value("CLOSE_TICKET"))
                .andExpect(jsonPath("$.data.result.status").value("CLOSED"))
                .andExpect(jsonPath("$.data.result.id").value((int) id));
    }

    @Test
    void aiUnknownIntent_stillReturns200WithUnknown() throws Exception {
        mockMvc.perform(post("/ai/tickets/handle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "text", "hello random text without keywords"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.intent").value("UNKNOWN"));
    }

    @Test
    void chineseRequestBody_roundTripsUtf8() throws Exception {
        long id = seedTicket("中文标题", "中文描述");
        String payload = String.format("{\"text\":\"请用中文查询 %d 号工单\"}", id);
        String raw = mockMvc.perform(post("/ai/tickets/handle")
                        .contentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
                        .content(payload.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        JsonNode root = objectMapper.readTree(raw);
        assertThat(root.path("success").asBoolean()).isTrue();
        assertThat(root.path("data").path("intent").asText()).isEqualTo("QUERY_TICKET");
        assertThat(root.path("data").path("result").path("title").asText()).isEqualTo("中文标题");
    }

    private long seedTicket(String title, String description) throws Exception {
        TicketCreateRequest req = new TicketCreateRequest();
        req.setTitle(title);
        req.setDescription(description);
        String response = mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(response).at("/data/id").asLong();
    }
}
