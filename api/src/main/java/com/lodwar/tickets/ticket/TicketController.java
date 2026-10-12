package com.lodwar.tickets.ticket;

import com.lodwar.tickets.ticket.TicketDtos.CreateTicketRequest;
import com.lodwar.tickets.ticket.TicketDtos.TicketPage;
import com.lodwar.tickets.ticket.TicketDtos.TicketResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService service;

    public TicketController(TicketService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse create(@Valid @RequestBody CreateTicketRequest request) {
        return service.create(request);
    }

    @GetMapping
    public TicketPage list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String limit) {
        return service.list(TicketQueries.listParams(status, page, limit));
    }

    // Literal paths win over patterns in Spring MVC, and {id} only matches digits anyway.
    @GetMapping("/stats")
    public Map<String, Long> stats() {
        return service.countByStatus();
    }

    @PatchMapping("/{id:\\d+}")
    public TicketResponse update(@PathVariable long id, @RequestBody Map<String, Object> body) {
        return service.update(id, TicketQueries.changes(body));
    }
}
