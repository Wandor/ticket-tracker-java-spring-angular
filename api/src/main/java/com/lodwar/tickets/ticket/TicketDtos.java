package com.lodwar.tickets.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/** Request and response shapes for the ticket API. JSON names are snake_case (see application.yml). */
public final class TicketDtos {

    private TicketDtos() {
    }

    /**
     * POST body. Enums arrive as strings and are checked with {@code @Pattern} so a bad value is
     * reported as a field error like any other, rather than as an unreadable-body error.
     */
    public record CreateTicketRequest(
            @NotBlank(message = "is required")
            @Size(max = 200, message = "must be at most 200 characters")
            String title,
            String description,
            @Pattern(regexp = "open|in_progress|closed", message = Status.MESSAGE)
            String status,
            @Pattern(regexp = "low|medium|high", message = Priority.MESSAGE)
            String priority) {

        public CreateTicketRequest {
            title = title == null ? null : title.strip();
            // A blank description is stored as NULL so clients can always send the field.
            description = description == null || description.isBlank() ? null : description.strip();
        }

        public Status statusOrDefault() {
            return status == null ? Status.OPEN : Status.fromValue(status).orElseThrow();
        }

        public Priority priorityOrDefault() {
            return priority == null ? Priority.MEDIUM : Priority.fromValue(priority).orElseThrow();
        }
    }

    public record TicketResponse(
            long id, String title, String description, Status status, Priority priority, Instant createdAt) {

        static TicketResponse from(Ticket t) {
            return new TicketResponse(
                    t.getId(), t.getTitle(), t.getDescription(), t.getStatus(), t.getPriority(), t.getCreatedAt());
        }
    }

    public record Pagination(int page, int limit, long total, int totalPages) {
    }

    public record TicketPage(List<TicketResponse> data, Pagination pagination) {
    }
}
