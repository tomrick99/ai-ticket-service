package com.example.aiticketservice.dto;

public class AiHandleResponse {
    private String intent;
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
