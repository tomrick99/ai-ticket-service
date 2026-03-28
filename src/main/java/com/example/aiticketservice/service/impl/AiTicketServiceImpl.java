package com.example.aiticketservice.service.impl;

import com.example.aiticketservice.client.IntentClient;
import com.example.aiticketservice.client.IntentResult;
import com.example.aiticketservice.dto.AiHandleResponse;
import com.example.aiticketservice.dto.TicketCreateRequest;
import com.example.aiticketservice.dto.TicketResponse;
import com.example.aiticketservice.service.AiTicketService;
import com.example.aiticketservice.service.TicketService;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiTicketServiceImpl implements AiTicketService {
    private static final Pattern ID_PATTERN = Pattern.compile("(\\d+)");

    private final IntentClient intentClient;
    private final TicketService ticketService;

    public AiTicketServiceImpl(IntentClient intentClient, TicketService ticketService) {
        this.intentClient = intentClient;
        this.ticketService = ticketService;
    }

    @Override
    public AiHandleResponse handleText(String text) {
        IntentResult result = intentClient.analyze(text);

        return switch (result.getIntent()) {
            case CREATE_TICKET -> new AiHandleResponse(result.getIntent().name(), handleCreate(result, text));
            case QUERY_TICKET -> new AiHandleResponse(result.getIntent().name(), ticketService.getTicket(requireTicketId(result, text)));
            case CLOSE_TICKET -> new AiHandleResponse(result.getIntent().name(), ticketService.closeTicket(requireTicketId(result, text)));
            default -> new AiHandleResponse(result.getIntent().name(), "未识别到可执行意图，请使用“创建/查询/关闭”相关描述");
        };
    }

    private TicketResponse handleCreate(IntentResult result, String text) {
        TicketCreateRequest request = new TicketCreateRequest();
        String title = result.getTitle() != null && !result.getTitle().isBlank()
                ? result.getTitle()
                : "AI创建工单";
        String description = result.getDescription() != null && !result.getDescription().isBlank()
                ? result.getDescription()
                : (text != null ? text : "");
        request.setTitle(title);
        request.setDescription(description);
        return ticketService.createTicket(request);
    }

    private Long requireTicketId(IntentResult result, String text) {
        if (result.getTicketId() != null) {
            return result.getTicketId();
        }
        return extractId(text);
    }

    private Long extractId(String text) {
        Matcher matcher = ID_PATTERN.matcher(text == null ? "" : text);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        throw new IllegalArgumentException("未从输入中提取到工单ID，请在句子中包含数字ID");
    }
}
