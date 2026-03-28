package com.example.aiticketservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "TicketCreateRequest", description = "Request body for creating a ticket")
public class TicketCreateRequest {
    @NotBlank(message = "title 不能为空")
    @Schema(description = "Ticket title", example = "登录失败")
    private String title;

    @NotBlank(message = "description 不能为空")
    @Schema(description = "Ticket description", example = "用户反馈账号无法登录系统")
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
