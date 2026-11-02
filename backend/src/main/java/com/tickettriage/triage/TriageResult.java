package com.tickettriage.triage;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.tickettriage.ticket.Category;
import com.tickettriage.ticket.Priority;
import com.tickettriage.ticket.Sentiment;

/**
 * What the LLM returns for one customer email. Spring AI turns this record into a
 * JSON schema (the enums become allowed values) and maps the model's JSON answer back onto it.
 */
public record TriageResult(
        Category category,
        Priority priority,
        Sentiment sentiment,
        @JsonPropertyDescription("One sentence summarising the customer's issue")
        String summary,
        @JsonPropertyDescription("A polite, helpful reply the support agent can send to the customer")
        String suggestedReply) {
}
