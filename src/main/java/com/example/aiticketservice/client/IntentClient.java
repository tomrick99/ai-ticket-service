package com.example.aiticketservice.client;

public interface IntentClient {
    IntentType detectIntent(String text);
}
