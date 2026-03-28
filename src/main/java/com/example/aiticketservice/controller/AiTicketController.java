package com.example.aiticketservice.controller;

import com.example.aiticketservice.dto.AiHandleResponse;
import com.example.aiticketservice.dto.AiTicketRequest;
import com.example.aiticketservice.dto.ApiResponse;
import com.example.aiticketservice.service.AiTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai/tickets")
@Tag(name = "AI Ticket", description = "AI-driven ticket APIs")
public class AiTicketController {
    private final AiTicketService aiTicketService;

    public AiTicketController(AiTicketService aiTicketService) {
        this.aiTicketService = aiTicketService;
    }

    @PostMapping("/handle")
    @Operation(
            summary = "Handle ticket text with AI",
            description = "Recognize user intent from text and execute the matched ticket action.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "AI handling finished"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Request validation failed",
                            content = @Content(schema = @Schema(implementation = ApiResponse.class))
                    )
            }
    )
    public ApiResponse<AiHandleResponse> handle(@Valid @RequestBody AiTicketRequest request) {
        return ApiResponse.ok("AI处理完成", aiTicketService.handleText(request.getText()));
    }
}
