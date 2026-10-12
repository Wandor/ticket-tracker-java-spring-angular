package com.lodwar.tickets.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.lodwar.tickets.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Exercises the full stack (MVC, validation, JPA, Flyway schema) against a real MySQL started
 * by Testcontainers, so CHECK constraints, ordering and GROUP BY behave as in production.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TicketApiIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    TicketRepository tickets;

    @BeforeEach
    void clearTickets() {
        tickets.deleteAllInBatch();
    }

    private ResultActions send(MockHttpServletRequestBuilder request, String json) throws Exception {
        return mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long create(String json) throws Exception {
        String body = send(post("/tickets"), json)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void createAppliesDefaultsTrimsAndPersists() throws Exception {
        send(post("/tickets"), """
                {"title": "  VPN drops every hour  ", "description": ""}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("VPN drops every hour"))
                .andExpect(jsonPath("$.description").isEmpty())
                .andExpect(jsonPath("$.status").value("open"))
                .andExpect(jsonPath("$.priority").value("medium"))
                .andExpect(jsonPath("$.created_at").value(endsWith("Z")));

        assertThat(tickets.findAll()).singleElement()
                .satisfies(t -> assertThat(t.getTitle()).isEqualTo("VPN drops every hour"));
    }

    @Test
    void createRejectsInvalidInputWithoutWriting() throws Exception {
        send(post("/tickets"), """
                {"title": "   ", "priority": "urgent"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("validation_error"))
                .andExpect(jsonPath("$.error.fields.title").value("is required"))
                .andExpect(jsonPath("$.error.fields.priority").value("must be one of low, medium, high"));

        assertThat(tickets.count()).isZero();
    }

    @Test
    void listFiltersByStatusAndPaginatesNewestFirst() throws Exception {
        for (String[] t : new String[][] {{"a", "open"}, {"b", "closed"}, {"c", "open"}, {"d", "open"}}) {
            create("{\"title\": \"%s\", \"status\": \"%s\"}".formatted(t[0], t[1]));
        }

        mvc.perform(get("/tickets").param("status", "open").param("limit", "2").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].title", contains("d", "c")))
                .andExpect(content().json("""
                        {"pagination": {"page": 1, "limit": 2, "total": 3, "total_pages": 2}}"""));

        mvc.perform(get("/tickets").param("status", "open").param("limit", "2").param("page", "2"))
                .andExpect(jsonPath("$.data[*].title", contains("a")));

        mvc.perform(get("/tickets").param("status", "pending"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.status").exists());
    }

    @Test
    void updateChangesOnlyTheGivenFields() throws Exception {
        long id = create("""
                {"title": "Reset password", "priority": "high"}""");

        send(patch("/tickets/{id}", id), """
                {"status": "in_progress"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("in_progress"))
                .andExpect(jsonPath("$.priority").value("high"));

        send(patch("/tickets/{id}", id), """
                {"title": "Renamed"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.title").value("cannot be updated"));

        send(patch("/tickets/{id}", 9999), """
                {"status": "closed"}""")
                .andExpect(status().isNotFound());
    }

    @Test
    void statsReturnsZeroFilledCountsGroupedByStatus() throws Exception {
        mvc.perform(get("/tickets/stats"))
                .andExpect(content().json("""
                        {"open": 0, "in_progress": 0, "closed": 0}""", JsonCompareMode.STRICT));

        create("{\"title\": \"one\"}");
        create("{\"title\": \"two\"}");
        long third = create("{\"title\": \"three\"}");
        send(patch("/tickets/{id}", third), "{\"status\": \"closed\"}").andExpect(status().isOk());

        mvc.perform(get("/tickets/stats"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"open": 2, "in_progress": 0, "closed": 1}""", JsonCompareMode.STRICT));
    }
}
