package com.lodwar.tickets.web;

import java.util.Map;

/** Field-level validation failure rendered as a 400 with a {@code fields} map. */
public class ValidationException extends RuntimeException {

    private final Map<String, String> fields;

    public ValidationException(Map<String, String> fields) {
        super("Request validation failed");
        this.fields = Map.copyOf(fields);
    }

    public Map<String, String> fields() {
        return fields;
    }
}
