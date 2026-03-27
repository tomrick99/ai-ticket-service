package com.example.aiticketservice.repository;

import com.example.aiticketservice.entity.Ticket;

import java.util.Optional;

public interface TicketRepository {
    Ticket save(Ticket ticket);

    Optional<Ticket> findById(Long id);
}
