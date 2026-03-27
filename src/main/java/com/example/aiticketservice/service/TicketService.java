package com.example.aiticketservice.service;

import com.example.aiticketservice.dto.TicketCreateRequest;
import com.example.aiticketservice.dto.TicketResponse;

public interface TicketService {
    TicketResponse createTicket(TicketCreateRequest request);

    TicketResponse getTicket(Long id);

    TicketResponse closeTicket(Long id);
}
