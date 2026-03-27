package com.example.aiticketservice.service.impl;

import com.example.aiticketservice.dto.TicketCreateRequest;
import com.example.aiticketservice.dto.TicketResponse;
import com.example.aiticketservice.entity.Ticket;
import com.example.aiticketservice.repository.TicketRepository;
import com.example.aiticketservice.service.TicketService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TicketServiceImpl implements TicketService {
    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_CLOSED = "CLOSED";

    private final TicketRepository ticketRepository;

    public TicketServiceImpl(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Override
    public TicketResponse createTicket(TicketCreateRequest request) {
        Ticket ticket = new Ticket();
        ticket.setTitle(request.getTitle());
        ticket.setDescription(request.getDescription());
        ticket.setStatus(STATUS_OPEN);
        ticket.setCreateTime(LocalDateTime.now());
        return toResponse(ticketRepository.save(ticket));
    }

    @Override
    public TicketResponse getTicket(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("工单不存在: " + id));
        return toResponse(ticket);
    }

    @Override
    public TicketResponse closeTicket(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("工单不存在: " + id));
        ticket.setStatus(STATUS_CLOSED);
        return toResponse(ticketRepository.save(ticket));
    }

    private TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getCreateTime()
        );
    }
}
