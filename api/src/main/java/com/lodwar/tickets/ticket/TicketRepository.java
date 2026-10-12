package com.lodwar.tickets.ticket;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Page<Ticket> findByStatus(Status status, Pageable pageable);

    @Query("select t.status as status, count(t) as total from Ticket t group by t.status")
    List<StatusCount> countByStatus();

    interface StatusCount {
        Status getStatus();

        long getTotal();
    }
}
