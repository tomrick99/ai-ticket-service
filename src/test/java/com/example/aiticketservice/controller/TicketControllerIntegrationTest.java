package com.example.aiticketservice.controller;

import com.example.aiticketservice.dto.TicketCreateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TicketControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createTicket_success_returnsChineseFields() throws Exception {
        TicketCreateRequest req = new TicketCreateRequest();
        req.setTitle("登录失败");
        req.setDescription("用户无法登录后台");

        mockMvc.perform(post("/tickets")
                        .contentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("工单创建成功"))
                .andExpect(jsonPath("$.data.title").value("登录失败"))
                .andExpect(jsonPath("$.data.description").value("用户无法登录后台"))
                .andExpect(jsonPath("$.data.status").value("OPEN"));
    }

    @Test
    void getTicket_success_afterCreate() throws Exception {
        TicketCreateRequest req = new TicketCreateRequest();
        req.setTitle("查询用");
        req.setDescription("描述");
        String createBody = objectMapper.writeValueAsString(req);

        String response = mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        long id = objectMapper.readTree(response).at("/data/id").asLong();

        mockMvc.perform(get("/tickets/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("工单查询成功"))
                .andExpect(jsonPath("$.data.title").value("查询用"));
    }

    @Test
    void getTicket_notFound_returns400AndMessage() throws Exception {
        mockMvc.perform(get("/tickets/{id}", 999_999L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("工单不存在: 999999"));
    }

    @Test
    void closeTicket_success() throws Exception {
        TicketCreateRequest req = new TicketCreateRequest();
        req.setTitle("待关闭");
        req.setDescription("描述");
        String createBody = objectMapper.writeValueAsString(req);

        String response = mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        long id = objectMapper.readTree(response).at("/data/id").asLong();

        mockMvc.perform(put("/tickets/{id}/close", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("工单关闭成功"))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));
    }

    @Test
    void createTicket_validationFails_whenTitleBlank() throws Exception {
        TicketCreateRequest req = new TicketCreateRequest();
        req.setTitle("");
        req.setDescription("有描述");

        mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("title 不能为空"));
    }
}
