package com.tickettriage.ticket.dto;

/**
 * Result of creating a ticket. {@code warning} is null when everything went fine,
 * and set when the ticket was saved but AI triage could not be applied.
 */
public record CreateTicketResponse(TicketResponse ticket, String warning) {
}
