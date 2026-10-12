package com.lodwar.tickets.ticket;

import com.lodwar.tickets.web.ValidationException;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Parses the inputs Bean Validation handles poorly: query strings (where every value is text)
 * and the PATCH body (where unknown fields must be rejected, not ignored).
 */
final class TicketQueries {

    static final int DEFAULT_LIMIT = 20;
    static final int MAX_LIMIT = 100;

    private TicketQueries() {
    }

    record ListParams(Optional<Status> status, int page, int limit) {
    }

    record Changes(Optional<Status> status, Optional<Priority> priority) {
    }

    static ListParams listParams(String status, String page, String limit) {
        Map<String, String> errors = new TreeMap<>();
        Optional<Status> parsedStatus = Optional.empty();
        if (status != null && !status.isEmpty()) {
            parsedStatus = Status.fromValue(status);
            if (parsedStatus.isEmpty()) {
                errors.put("status", Status.MESSAGE);
            }
        }
        Integer parsedPage = parseInt(page, 1, Integer.MAX_VALUE);
        if (parsedPage == null) {
            errors.put("page", "must be a positive integer");
        }
        Integer parsedLimit = parseInt(limit, DEFAULT_LIMIT, MAX_LIMIT);
        if (parsedLimit == null) {
            errors.put("limit", "must be an integer between 1 and " + MAX_LIMIT);
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        return new ListParams(parsedStatus, parsedPage, parsedLimit);
    }

    static Changes changes(Map<String, Object> body) {
        Map<String, String> errors = new TreeMap<>();
        // Reject other fields outright so a client never believes it renamed a ticket.
        body.keySet().stream()
                .filter(key -> !key.equals("status") && !key.equals("priority"))
                .forEach(key -> errors.put(key, "cannot be updated"));

        Optional<Status> status = Optional.empty();
        if (body.get("status") != null) {
            status = body.get("status") instanceof String s ? Status.fromValue(s) : Optional.empty();
            if (status.isEmpty()) {
                errors.put("status", Status.MESSAGE);
            }
        }
        Optional<Priority> priority = Optional.empty();
        if (body.get("priority") != null) {
            priority = body.get("priority") instanceof String p ? Priority.fromValue(p) : Optional.empty();
            if (priority.isEmpty()) {
                errors.put("priority", Priority.MESSAGE);
            }
        }
        if (errors.isEmpty() && status.isEmpty() && priority.isEmpty()) {
            errors.put("body", "provide status and/or priority");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        return new Changes(status, priority);
    }

    private static Integer parseInt(String raw, int fallback, int max) {
        if (raw == null || raw.isEmpty()) {
            return fallback;
        }
        try {
            int value = Integer.parseInt(raw);
            return value >= 1 && value <= max ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
