package com.example.aiticketservice.controller;

import com.example.aiticketservice.dto.ApiResponse;
import com.example.aiticketservice.dto.TicketCreateRequest;
import com.example.aiticketservice.dto.TicketResponse;
import com.example.aiticketservice.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tickets")
public class TicketController {
    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ApiResponse<TicketResponse> createTicket(@Valid @RequestBody TicketCreateRequest request) {
        return ApiResponse.ok("工单创建成功", ticketService.createTicket(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<TicketResponse> getTicket(@PathVariable Long id) {
        return ApiResponse.ok("工单查询成功", ticketService.getTicket(id));
    }

    @PutMapping("/{id}/close")
    public ApiResponse<TicketResponse> closeTicket(@PathVariable Long id) {
        return ApiResponse.ok("工单关闭成功", ticketService.closeTicket(id));
    }
}
