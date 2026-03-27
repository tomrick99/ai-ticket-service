package com.example.aiticketservice.controller;

import com.example.aiticketservice.dto.AiHandleResponse;
import com.example.aiticketservice.dto.AiTicketRequest;
import com.example.aiticketservice.dto.ApiResponse;
import com.example.aiticketservice.service.AiTicketService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai/tickets")
public class AiTicketController {
    private final AiTicketService aiTicketService;

    public AiTicketController(AiTicketService aiTicketService) {
        this.aiTicketService = aiTicketService;
    }

    @PostMapping("/handle")
    public ApiResponse<AiHandleResponse> handle(@Valid @RequestBody AiTicketRequest request) {
        return ApiResponse.ok("AI处理完成", aiTicketService.handleText(request.getText()));
    }
}
