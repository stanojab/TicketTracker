package com.tickettriage.ticket;

import com.tickettriage.ticket.dto.StatsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatsController {

    private final TicketService ticketService;

    public StatsController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/api/stats")
    public StatsResponse stats() {
        return ticketService.stats();
    }
}
