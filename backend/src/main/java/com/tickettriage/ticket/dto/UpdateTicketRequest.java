package com.tickettriage.ticket.dto;

import com.tickettriage.ticket.Category;
import com.tickettriage.ticket.Priority;
import com.tickettriage.ticket.TicketStatus;
import jakarta.validation.constraints.Size;

/**
 * Partial update (PATCH): every field is optional and only non-null fields are applied.
 */
public record UpdateTicketRequest(
        Category category,
        Priority priority,
        @Size(max = 5_000) String suggestedReply,
        TicketStatus status) {
}
