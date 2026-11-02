package com.tickettriage.ticket;

import com.tickettriage.ticket.dto.CreateTicketRequest;
import com.tickettriage.ticket.dto.CreateTicketResponse;
import com.tickettriage.ticket.dto.TicketResponse;
import com.tickettriage.ticket.dto.UpdateTicketRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<CreateTicketResponse> create(@Valid @RequestBody CreateTicketRequest request) {
        CreateTicketResponse response = ticketService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.ticket().id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public List<TicketResponse> list(@RequestParam(required = false) Category category,
                                     @RequestParam(required = false) Priority priority,
                                     @RequestParam(required = false) TicketStatus status) {
        return ticketService.list(category, priority, status);
    }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable Long id) {
        return ticketService.get(id);
    }

    @PatchMapping("/{id}")
    public TicketResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTicketRequest request) {
        return ticketService.update(id, request);
    }
}
