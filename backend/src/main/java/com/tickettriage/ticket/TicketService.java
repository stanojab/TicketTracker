package com.tickettriage.ticket;

import com.tickettriage.common.TicketNotFoundException;
import com.tickettriage.ticket.dto.CreateTicketRequest;
import com.tickettriage.ticket.dto.CreateTicketResponse;
import com.tickettriage.ticket.dto.StatsResponse;
import com.tickettriage.ticket.dto.TicketResponse;
import com.tickettriage.ticket.dto.UpdateTicketRequest;
import com.tickettriage.triage.TriageClient;
import com.tickettriage.triage.TriageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class TicketService {

    static final String TRIAGE_WARNING =
            "AI triage is unavailable. The ticket was saved with default values (OTHER / MEDIUM); please review it.";

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");
    private static final int MAX_SUMMARY_LENGTH = 500;

    private final TicketRepository ticketRepository;
    private final TriageClient triageClient;

    public TicketService(TicketRepository ticketRepository, TriageClient triageClient) {
        this.ticketRepository = ticketRepository;
        this.triageClient = triageClient;
    }

    /**
     * Triage with AI, then save. Deliberately NOT @Transactional: the LLM call can take
     * seconds and must not hold a database connection. repository.save() runs in its own
     * short transaction. If triage fails, the ticket keeps its defaults and we return a warning.
     */
    public CreateTicketResponse create(CreateTicketRequest request) {
        Ticket ticket = new Ticket(request.customerEmail(), request.subject(), request.body());

        String warning = null;
        try {
            applyTriage(ticket, triageClient.triage(request.subject(), request.body()));
        } catch (RuntimeException ex) {
            log.warn("AI triage failed; saving ticket with default values", ex);
            warning = TRIAGE_WARNING;
        }

        Ticket saved = ticketRepository.save(ticket);
        return new CreateTicketResponse(TicketResponse.from(saved), warning);
    }

    /**
     * Lists tickets, newest first. Each filter is optional: Query by Example
     * ignores null fields on the probe, so only the filters provided end up in the WHERE clause.
     */
    @Transactional(readOnly = true)
    public List<TicketResponse> list(Category category, Priority priority, TicketStatus status) {
        Ticket probe = new Ticket();
        probe.setCategory(category);
        probe.setPriority(priority);
        probe.setStatus(status);

        return ticketRepository.findAll(Example.of(probe), NEWEST_FIRST).stream()
                .map(TicketResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
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

    private static void applyTriage(Ticket ticket, TriageResult result) {
        ticket.setCategory(result.category());
        ticket.setPriority(result.priority());
        ticket.setSentiment(result.sentiment());
        ticket.setSummary(truncate(result.summary(), MAX_SUMMARY_LENGTH));
        ticket.setSuggestedReply(result.suggestedReply());
    }

    /** LLM output is untrusted: never let an over-long summary break the insert. */
    private static String truncate(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private Ticket findTicket(Long id) {
        return ticketRepository.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
    }
}
