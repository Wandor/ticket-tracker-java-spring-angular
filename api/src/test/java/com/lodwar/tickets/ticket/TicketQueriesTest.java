package com.lodwar.tickets.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lodwar.tickets.web.ValidationException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TicketQueriesTest {

    @Test
    void listParamsApplyDefaults() {
        var params = TicketQueries.listParams(null, "", null);

        assertThat(params.status()).isEmpty();
        assertThat(params.page()).isEqualTo(1);
        assertThat(params.limit()).isEqualTo(TicketQueries.DEFAULT_LIMIT);
    }

    @Test
    void listParamsReportEveryInvalidField() {
        assertThatThrownBy(() -> TicketQueries.listParams("done", "0", "101"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.fields()).containsOnlyKeys("status", "page", "limit"));
    }

    @Test
    void changesRejectUnknownFieldsAndEmptyBodies() {
        assertThatThrownBy(() -> TicketQueries.changes(Map.of("title", "x")))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.fields()).containsEntry("title", "cannot be updated"));
        assertThatThrownBy(() -> TicketQueries.changes(Map.of()))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.fields()).containsKey("body"));
    }
}
