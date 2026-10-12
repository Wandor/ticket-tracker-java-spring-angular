package com.lodwar.tickets.ticket;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Optional;

public enum Status {
    OPEN("open"),
    IN_PROGRESS("in_progress"),
    CLOSED("closed");

    public static final String MESSAGE = "must be one of open, in_progress, closed";

    private final String value;

    Status(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public static Optional<Status> fromValue(String value) {
        return Arrays.stream(values()).filter(s -> s.value.equals(value)).findFirst();
    }
}
