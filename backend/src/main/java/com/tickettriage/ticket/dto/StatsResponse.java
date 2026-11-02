package com.tickettriage.ticket.dto;

import com.tickettriage.ticket.Category;
import com.tickettriage.ticket.Priority;

import java.util.Map;

public record StatsResponse(Map<Category, Long> byCategory, Map<Priority, Long> byPriority) {
}
