package com.example.aiticketservice.dto;

import jakarta.validation.constraints.NotBlank;

public class AiTicketRequest {
    @NotBlank(message = "text 不能为空")
    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
