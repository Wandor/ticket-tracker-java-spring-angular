package com.lodwar.tickets.ticket;

import com.lodwar.tickets.ticket.TicketDtos.CreateTicketRequest;
import com.lodwar.tickets.ticket.TicketDtos.Pagination;
import com.lodwar.tickets.ticket.TicketDtos.TicketPage;
import com.lodwar.tickets.ticket.TicketDtos.TicketResponse;
import com.lodwar.tickets.ticket.TicketQueries.Changes;
import com.lodwar.tickets.ticket.TicketQueries.ListParams;
import com.lodwar.tickets.web.NotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

    // Newest first; id breaks ties so pagination is stable.
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final TicketRepository tickets;

    public TicketService(TicketRepository tickets) {
        this.tickets = tickets;
    }

    @Transactional
    public TicketResponse create(CreateTicketRequest request) {
        Ticket ticket = new Ticket(
                request.title(), request.description(), request.statusOrDefault(), request.priorityOrDefault());
        return TicketResponse.from(tickets.saveAndFlush(ticket));
    }

    @Transactional(readOnly = true)
    public TicketPage list(ListParams params) {
        PageRequest pageRequest = PageRequest.of(params.page() - 1, params.limit(), NEWEST_FIRST);
        Page<Ticket> page = params.status()
                .map(status -> tickets.findByStatus(status, pageRequest))
                .orElseGet(() -> tickets.findAll(pageRequest));

        return new TicketPage(
                page.map(TicketResponse::from).getContent(),
                new Pagination(params.page(), params.limit(), page.getTotalElements(), page.getTotalPages()));
    }

    @Transactional
    public TicketResponse update(long id, Changes changes) {
        Ticket ticket = tickets.findById(id).orElseThrow(() -> new NotFoundException("Ticket not found"));
        changes.status().ifPresent(ticket::setStatus);
        changes.priority().ifPresent(ticket::setPriority);
        return TicketResponse.from(ticket);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> countByStatus() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Status status : Status.values()) {
            counts.put(status.value(), 0L);
        }
        tickets.countByStatus().forEach(row -> counts.put(row.getStatus().value(), row.getTotal()));
        return counts;
    }
}
