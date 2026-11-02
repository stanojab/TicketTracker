package com.tickettriage.ticket;

import com.tickettriage.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full stack test: HTTP (MockMvc) -> controller -> service -> JPA -> Flyway-migrated PostgreSQL.
 * Uses the "mock" profile, so triage is done by the keyword-based MockTriageClient.
 * {@code @Transactional} rolls back after each test, so tests don't see each other's data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("mock")
@Import(TestcontainersConfiguration.class)
@Transactional
class TicketApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createListUpdateAndCountTickets() throws Exception {
        long id = createTicket("lisa@example.com", "Wrong invoice", "My invoice shows the wrong amount.");

        // Triaged by MockTriageClient ("invoice" -> BILLING)
        mockMvc.perform(get("/api/tickets/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("Wrong invoice"))
                .andExpect(jsonPath("$.category").value("BILLING"))
                .andExpect(jsonPath("$.suggestedReply").isNotEmpty())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        // The agent corrects the AI's choice and resolves the ticket
        mockMvc.perform(patch("/api/tickets/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category": "BILLING", "priority": "HIGH",
                                 "suggestedReply": "We are on it.", "status": "RESOLVED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("BILLING"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.suggestedReply").value("We are on it."))
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        mockMvc.perform(get("/api/tickets").param("category", "BILLING").param("status", "RESOLVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(id));

        mockMvc.perform(get("/api/tickets").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].status", everyItem(is("OPEN"))));

        mockMvc.perform(get("/api/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byCategory.BILLING").value(1))
                .andExpect(jsonPath("$.byCategory.TECHNICAL").value(0))
                .andExpect(jsonPath("$.byPriority.HIGH").value(1));
    }

    @Test
    void createStillSavesTicketWhenTriageFails() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerEmail": "x@example.com", "subject": "Invoice question",
                                 "body": "simulate-ai-failure"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticket.id").isNumber())
                .andExpect(jsonPath("$.ticket.category").value("OTHER"))
                .andExpect(jsonPath("$.ticket.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.warning").isNotEmpty());
    }

    @Test
    void createRejectsInvalidInput() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerEmail": "not-an-email", "subject": "", "body": "Hi"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.errors.customerEmail").exists())
                .andExpect(jsonPath("$.errors.subject").exists());
    }

    @Test
    void unknownTicketReturns404() throws Exception {
        mockMvc.perform(get("/api/tickets/{id}", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Ticket 999999 not found"));
    }

    @Test
    void invalidFilterValueReturns400() throws Exception {
        mockMvc.perform(get("/api/tickets").param("priority", "SUPER_HIGH"))
                .andExpect(status().isBadRequest());
    }

    private long createTicket(String email, String subject, String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerEmail": "%s", "subject": "%s", "body": "%s"}
                                """.formatted(email, subject, body)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("http://localhost/api/tickets/\\d+")))
                .andExpect(jsonPath("$.ticket.id").isNumber())
                .andExpect(jsonPath("$.warning").value(nullValue()))
                .andReturn();
        String location = result.getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }
}
