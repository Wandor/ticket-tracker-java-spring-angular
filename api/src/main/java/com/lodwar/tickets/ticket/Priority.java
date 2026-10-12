package com.lodwar.tickets.ticket;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Optional;

public enum Priority {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high");

    public static final String MESSAGE = "must be one of low, medium, high";

    private final String value;

    Priority(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public static Optional<Priority> fromValue(String value) {
        return Arrays.stream(values()).filter(p -> p.value.equals(value)).findFirst();
    }
}
