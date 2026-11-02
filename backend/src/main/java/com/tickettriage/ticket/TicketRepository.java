package com.tickettriage.ticket;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Spring Data generates the implementation at runtime. JpaRepository gives us
 * save/findById/findAll, and (via QueryByExampleExecutor) findAll(Example, Sort).
 */
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Query("select t.category as category, count(t) as total from Ticket t group by t.category")
    List<CategoryCount> countByCategory();

    @Query("select t.priority as priority, count(t) as total from Ticket t group by t.priority")
    List<PriorityCount> countByPriority();

    /** Interface projection: Spring Data maps each result row onto these getters by alias. */
    interface CategoryCount {
        Category getCategory();

        long getTotal();
    }

    interface PriorityCount {
        Priority getPriority();

        long getTotal();
    }
}
