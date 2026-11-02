package com.tickettriage.ticket.dto;

import com.tickettriage.ticket.Category;
import com.tickettriage.ticket.Priority;
import com.tickettriage.ticket.Sentiment;
import com.tickettriage.ticket.Ticket;
import com.tickettriage.ticket.TicketStatus;

import java.time.Instant;

public record TicketResponse(
        Long id,
        String customerEmail,
        String subject,
        String body,
        Category category,
        Priority priority,
        Sentiment sentiment,
        String summary,
        String suggestedReply,
        TicketStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getCustomerEmail(),
                ticket.getSubject(),
                ticket.getBody(),
                ticket.getCategory(),
                ticket.getPriority(),
                ticket.getSentiment(),
                ticket.getSummary(),
                ticket.getSuggestedReply(),
                ticket.getStatus(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt());
    }
}
