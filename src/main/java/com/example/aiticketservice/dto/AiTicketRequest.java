package com.example.aiticketservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "AiTicketRequest", description = "Request body for AI ticket handling")
public class AiTicketRequest {
    @NotBlank(message = "text 不能为空")
    @Schema(description = "Natural language text for AI intent recognition", example = "帮我创建一个网络中断的工单")
    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
