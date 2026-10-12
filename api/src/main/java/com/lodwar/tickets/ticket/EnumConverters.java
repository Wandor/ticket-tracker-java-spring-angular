package com.lodwar.tickets.ticket;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persist enums by their lowercase wire value so the column content matches the API and the
 * table's CHECK constraints, rather than JPA's default ordinal or constant name.
 */
final class EnumConverters {

    private EnumConverters() {
    }

    @Converter(autoApply = true)
    static class StatusConverter implements AttributeConverter<Status, String> {
        @Override
        public String convertToDatabaseColumn(Status status) {
            return status == null ? null : status.value();
        }

        @Override
        public Status convertToEntityAttribute(String value) {
            return value == null ? null : Status.fromValue(value)
                    .orElseThrow(() -> new IllegalStateException("Unknown status in database: " + value));
        }
    }

    @Converter(autoApply = true)
    static class PriorityConverter implements AttributeConverter<Priority, String> {
        @Override
        public String convertToDatabaseColumn(Priority priority) {
            return priority == null ? null : priority.value();
        }

        @Override
        public Priority convertToEntityAttribute(String value) {
            return value == null ? null : Priority.fromValue(value)
                    .orElseThrow(() -> new IllegalStateException("Unknown priority in database: " + value));
        }
    }
}
