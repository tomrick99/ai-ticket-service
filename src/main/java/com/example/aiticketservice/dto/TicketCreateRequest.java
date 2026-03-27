package com.example.aiticketservice.dto;

import jakarta.validation.constraints.NotBlank;

public class TicketCreateRequest {
    @NotBlank(message = "title 不能为空")
    private String title;

    @NotBlank(message = "description 不能为空")
    private String description;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
