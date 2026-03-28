package com.example.aiticketservice.controller;

import com.example.aiticketservice.dto.ApiResponse;
import com.example.aiticketservice.dto.TicketCreateRequest;
import com.example.aiticketservice.dto.TicketResponse;
import com.example.aiticketservice.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tickets")
@Tag(name = "Ticket", description = "Ticket CRUD APIs")
public class TicketController {
    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @Operation(
            summary = "Create a ticket",
            description = "Create a new ticket with title and description.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ticket created"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Validation failed",
                            content = @Content(schema = @Schema(implementation = ApiResponse.class))
                    )
            }
    )
    public ApiResponse<TicketResponse> createTicket(@Valid @RequestBody TicketCreateRequest request) {
        return ApiResponse.ok("工单创建成功", ticketService.createTicket(request));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get ticket by id",
            description = "Query a ticket by its id.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ticket found"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Ticket not found",
                            content = @Content(schema = @Schema(implementation = ApiResponse.class))
                    )
            }
    )
    public ApiResponse<TicketResponse> getTicket(@PathVariable Long id) {
        return ApiResponse.ok("工单查询成功", ticketService.getTicket(id));
    }

    @PutMapping("/{id}/close")
    @Operation(
            summary = "Close ticket by id",
            description = "Close an existing ticket.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ticket closed"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Ticket not found",
                            content = @Content(schema = @Schema(implementation = ApiResponse.class))
                    )
            }
    )
    public ApiResponse<TicketResponse> closeTicket(@PathVariable Long id) {
        return ApiResponse.ok("工单关闭成功", ticketService.closeTicket(id));
    }
}
