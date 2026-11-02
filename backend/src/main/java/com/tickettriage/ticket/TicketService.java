package com.tickettriage.ticket;

import com.tickettriage.common.TicketNotFoundException;
import com.tickettriage.ticket.dto.CreateTicketRequest;
import com.tickettriage.ticket.dto.CreateTicketResponse;
import com.tickettriage.ticket.dto.StatsResponse;
import com.tickettriage.ticket.dto.TicketResponse;
import com.tickettriage.ticket.dto.UpdateTicketRequest;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class TicketService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public CreateTicketResponse create(CreateTicketRequest request) {
        Ticket ticket = new Ticket(request.customerEmail(), request.subject(), request.body());
        Ticket saved = ticketRepository.save(ticket);
        return new CreateTicketResponse(TicketResponse.from(saved), null);
    }

    /**
     * Lists tickets, newest first. Each filter is optional: Query by Example
     * ignores null fields on the probe, so only the filters provided end up in the WHERE clause.
     */
    public List<TicketResponse> list(Category category, Priority priority, TicketStatus status) {
        Ticket probe = new Ticket();
        probe.setCategory(category);
        probe.setPriority(priority);
        probe.setStatus(status);

        return ticketRepository.findAll(Example.of(probe), NEWEST_FIRST).stream()
                .map(TicketResponse::from)
                .toList();
    }

    public TicketResponse get(Long id) {
        return TicketResponse.from(findTicket(id));
    }

    @Transactional
    public TicketResponse update(Long id, UpdateTicketRequest request) {
        Ticket ticket = findTicket(id);
        if (request.category() != null) {
            ticket.setCategory(request.category());
        }
        if (request.priority() != null) {
            ticket.setPriority(request.priority());
        }
        if (request.suggestedReply() != null) {
            ticket.setSuggestedReply(request.suggestedReply());
        }
        if (request.status() != null) {
            ticket.setStatus(request.status());
        }
        // Flush now so @UpdateTimestamp is applied before we build the response.
        return TicketResponse.from(ticketRepository.saveAndFlush(ticket));
    }

    public StatsResponse stats() {
        Map<Category, Long> byCategory = new EnumMap<>(Category.class);
        for (Category category : Category.values()) {
            byCategory.put(category, 0L);
        }
        ticketRepository.countByCategory()
                .forEach(row -> byCategory.put(row.getCategory(), row.getTotal()));

        Map<Priority, Long> byPriority = new EnumMap<>(Priority.class);
        for (Priority priority : Priority.values()) {
            byPriority.put(priority, 0L);
        }
        ticketRepository.countByPriority()
                .forEach(row -> byPriority.put(row.getPriority(), row.getTotal()));

        return new StatsResponse(byCategory, byPriority);
    }

    private Ticket findTicket(Long id) {
        return ticketRepository.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
    }
}
