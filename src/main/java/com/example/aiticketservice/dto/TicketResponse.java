package com.example.aiticketservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "TicketResponse", description = "Ticket response payload")
public class TicketResponse {
    @Schema(description = "Ticket id", example = "1")
    private Long id;
    @Schema(description = "Ticket title", example = "登录失败")
    private String title;
    @Schema(description = "Ticket description", example = "用户反馈账号无法登录系统")
    private String description;
    @Schema(description = "Ticket status", example = "OPEN")
    private String status;
    @Schema(description = "Ticket creation time", example = "2026-03-27T10:00:00")
    private LocalDateTime createTime;

    public TicketResponse() {
    }

    public TicketResponse(Long id, String title, String description, String status, LocalDateTime createTime) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.createTime = createTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
