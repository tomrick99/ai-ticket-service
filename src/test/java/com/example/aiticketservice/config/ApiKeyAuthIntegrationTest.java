package com.example.aiticketservice.config;

import com.example.aiticketservice.dto.TicketCreateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.security.enabled=true",
        "app.security.api-key=test-key",
        "app.audit.enabled=true",
        "logging.level.AUDIT=INFO"
})
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class ApiKeyAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void protectedTicketEndpoint_requiresApiKey() throws Exception {
        TicketCreateRequest request = new TicketCreateRequest();
        request.setTitle("auth");
        request.setDescription("missing api key");

        mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void healthEndpoint_remainsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void metricsEndpoint_requiresApiKey() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void authorizedRequest_succeedsAndWritesAuditSummary(CapturedOutput output) throws Exception {
        TicketCreateRequest request = new TicketCreateRequest();
        request.setTitle("auth-ok");
        request.setDescription("with api key");

        mockMvc.perform(post("/tickets")
                        .header("X-API-Key", "test-key")
                        .header("X-Client-Id", "integration-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.success").value(true));

        assertThat(output.getOut()).contains("method=POST");
        assertThat(output.getOut()).contains("path=/tickets");
        assertThat(output.getOut()).contains("status=200");
        assertThat(output.getOut()).contains("durationMs=");
        assertThat(output.getOut()).contains("caller=integration-test");
    }
}
