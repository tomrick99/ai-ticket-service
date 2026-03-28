package com.example.aiticketservice.repository;

import com.example.aiticketservice.entity.Ticket;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TicketRepositoryTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Test
    void saveAndFindById_shouldPersistAndGenerateId() {
        Ticket ticket = new Ticket();
        ticket.setTitle("标题");
        ticket.setDescription("描述");
        ticket.setStatus("OPEN");

        Ticket saved = ticketRepository.save(ticket);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreateTime()).isNotNull();
        assertThat(saved.getUpdateTime()).isNotNull();

        Optional<Ticket> found = ticketRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("标题");
    }

    @Test
    void saveAgain_shouldUpdateStatusInDatabase() {
        Ticket ticket = new Ticket();
        ticket.setTitle("t");
        ticket.setDescription("d");
        ticket.setStatus("OPEN");
        Ticket saved = ticketRepository.save(ticket);
        Long id = saved.getId();

        saved.setStatus("CLOSED");
        ticketRepository.save(saved);

        Optional<Ticket> reloaded = ticketRepository.findById(id);
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getStatus()).isEqualTo("CLOSED");
    }
}
