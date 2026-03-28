package com.example.aiticketservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AiHandleResponse", description = "AI handling result payload")
public class AiHandleResponse {
    @Schema(description = "Recognized intent", example = "CREATE_TICKET")
    private String intent;
    @Schema(description = "Business result of the recognized intent")
    private Object result;

    public AiHandleResponse() {
    }

    public AiHandleResponse(String intent, Object result) {
        this.intent = intent;
        this.result = result;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
