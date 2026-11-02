package com.tickettriage.ticket;

import com.tickettriage.common.TicketNotFoundException;
import com.tickettriage.ticket.dto.CreateTicketRequest;
import com.tickettriage.ticket.dto.CreateTicketResponse;
import com.tickettriage.ticket.dto.StatsResponse;
import com.tickettriage.ticket.dto.TicketResponse;
import com.tickettriage.ticket.dto.UpdateTicketRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private TicketService ticketService;

    @Test
    void create_savesOpenTicketWithDefaults() {
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateTicketResponse response = ticketService.create(
                new CreateTicketRequest("anna@example.com", "Hello", "I have a question"));

        TicketResponse ticket = response.ticket();
        assertThat(ticket.customerEmail()).isEqualTo("anna@example.com");
        assertThat(ticket.category()).isEqualTo(Category.OTHER);
        assertThat(ticket.priority()).isEqualTo(Priority.MEDIUM);
        assertThat(ticket.sentiment()).isEqualTo(Sentiment.NEUTRAL);
        assertThat(ticket.status()).isEqualTo(TicketStatus.OPEN);
        assertThat(response.warning()).isNull();
    }

    @Test
    void list_usesProvidedFiltersAndSortsNewestFirst() {
        Ticket ticket = new Ticket("a@example.com", "Invoice", "Wrong amount");
        when(ticketRepository.findAll(any(Example.class), any(Sort.class))).thenReturn(List.of(ticket));

        List<TicketResponse> result = ticketService.list(Category.BILLING, null, TicketStatus.OPEN);

        assertThat(result).hasSize(1);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Example<Ticket>> example = ArgumentCaptor.forClass(Example.class);
        ArgumentCaptor<Sort> sort = ArgumentCaptor.forClass(Sort.class);
        verify(ticketRepository).findAll(example.capture(), sort.capture());

        Ticket probe = example.getValue().getProbe();
        assertThat(probe.getCategory()).isEqualTo(Category.BILLING);
        assertThat(probe.getPriority()).isNull();
        assertThat(probe.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(sort.getValue().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void get_throwsWhenTicketDoesNotExist() {
        when(ticketRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.get(42L))
                .isInstanceOf(TicketNotFoundException.class)
                .hasMessageContaining("42");
    }

    @Test
    void update_changesOnlyProvidedFields() {
        Ticket ticket = new Ticket("a@example.com", "Login", "Can't log in");
        ticket.setSuggestedReply("Original reply");
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.saveAndFlush(ticket)).thenReturn(ticket);

        TicketResponse result = ticketService.update(1L,
                new UpdateTicketRequest(null, Priority.URGENT, null, TicketStatus.RESOLVED));

        assertThat(result.priority()).isEqualTo(Priority.URGENT);
        assertThat(result.status()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(result.category()).isEqualTo(Category.OTHER);          // unchanged
        assertThat(result.suggestedReply()).isEqualTo("Original reply");  // unchanged
    }

    @Test
    void update_throwsWhenTicketDoesNotExist() {
        when(ticketRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.update(7L,
                new UpdateTicketRequest(Category.BILLING, null, null, null)))
                .isInstanceOf(TicketNotFoundException.class);
        verify(ticketRepository, never()).saveAndFlush(any());
    }

    @Test
    void stats_includesZeroForMissingGroups() {
        when(ticketRepository.countByCategory()).thenReturn(List.of(categoryCount(Category.BILLING, 3)));
        when(ticketRepository.countByPriority()).thenReturn(List.of(priorityCount(Priority.HIGH, 2)));

        StatsResponse stats = ticketService.stats();

        assertThat(stats.byCategory())
                .containsEntry(Category.BILLING, 3L)
                .containsEntry(Category.TECHNICAL, 0L)
                .hasSize(Category.values().length);
        assertThat(stats.byPriority())
                .containsEntry(Priority.HIGH, 2L)
                .containsEntry(Priority.URGENT, 0L)
                .hasSize(Priority.values().length);
    }

    private static TicketRepository.CategoryCount categoryCount(Category category, long total) {
        return new TicketRepository.CategoryCount() {
            public Category getCategory() {
                return category;
            }

            public long getTotal() {
                return total;
            }
        };
    }

    private static TicketRepository.PriorityCount priorityCount(Priority priority, long total) {
        return new TicketRepository.PriorityCount() {
            public Priority getPriority() {
                return priority;
            }

            public long getTotal() {
                return total;
            }
        };
    }
}
