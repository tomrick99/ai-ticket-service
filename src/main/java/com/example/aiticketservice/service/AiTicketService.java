package com.example.aiticketservice.service;

import com.example.aiticketservice.dto.AiHandleResponse;

public interface AiTicketService {
    AiHandleResponse handleText(String text);
}
