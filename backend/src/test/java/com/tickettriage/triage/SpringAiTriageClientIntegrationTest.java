package com.tickettriage.triage;

import com.sun.net.httpserver.HttpServer;
import com.tickettriage.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests the REAL Spring AI client (no "mock" profile) against a tiny fake Anthropic API
 * running in-process on the JDK's built-in HTTP server. No API key, no network, no cost,
 * but the request building, structured-output parsing and fallback all run for real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class SpringAiTriageClientIntegrationTest {

    private static final HttpServer fakeAnthropic = startFakeAnthropic();
    private static final AtomicInteger responseStatus = new AtomicInteger(200);
    private static final AtomicReference<String> responseBody = new AtomicReference<>();
    private static final AtomicReference<String> lastRequestBody = new AtomicReference<>();

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void anthropicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.anthropic.base-url",
                () -> "http://localhost:" + fakeAnthropic.getAddress().getPort());
        registry.add("spring.ai.anthropic.api-key", () -> "test-key");
        registry.add("spring.ai.anthropic.max-retries", () -> 0);
    }

    @BeforeEach
    void reset() {
        responseStatus.set(200);
        responseBody.set(null);
        lastRequestBody.set(null);
    }

    @AfterAll
    static void stopServer() {
        fakeAnthropic.stop(0);
    }

    @Test
    void appliesStructuredLlmOutputToTicket() throws Exception {
        responseBody.set(messageResponse("""
                {"category":"BILLING","priority":"HIGH","sentiment":"NEGATIVE",
                 "summary":"Customer was charged twice.","suggestedReply":"Sorry! We have refunded you."}
                """));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerEmail":"a@example.com","subject":"Double charge",
                                 "body":"You charged me twice {not a template}!"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.warning").value(nullValue()))
                .andExpect(jsonPath("$.ticket.category").value("BILLING"))
                .andExpect(jsonPath("$.ticket.priority").value("HIGH"))
                .andExpect(jsonPath("$.ticket.sentiment").value("NEGATIVE"))
                .andExpect(jsonPath("$.ticket.summary").value("Customer was charged twice."))
                .andExpect(jsonPath("$.ticket.suggestedReply").value("Sorry! We have refunded you."));

        String request = lastRequestBody.get();
        assertThat(request)
                .contains("\"model\":\"claude-opus-5-5\"")
                .contains("triage assistant")                    // system prompt
                .contains("You charged me twice {not a template}!") // email passed through untouched
                .contains("output_config");                      // native structured output requested
    }

    @Test
    void savesTicketWithDefaultsAndWarningWhenLlmFails() throws Exception {
        responseStatus.set(500);
        responseBody.set("""
                {"type":"error","error":{"type":"api_error","message":"Internal server error"}}
                """);

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerEmail":"b@example.com","subject":"Help","body":"Something broke"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.warning", containsString("AI triage is unavailable")))
                .andExpect(jsonPath("$.ticket.id").isNumber())
                .andExpect(jsonPath("$.ticket.category").value("OTHER"))
                .andExpect(jsonPath("$.ticket.priority").value("MEDIUM"));
    }

    /** A minimal Anthropic Messages API response whose text block is the JSON answer. */
    private static String messageResponse(String json) {
        String escaped = json.strip().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
        return """
                {"id":"msg_test","type":"message","role":"assistant","model":"claude-opus-5-5",
                 "content":[{"type":"text","text":"%s"}],
                 "stop_reason":"end_turn","stop_sequence":null,
                 "usage":{"input_tokens":120,"output_tokens":60}}
                """.formatted(escaped);
    }

    private static HttpServer startFakeAnthropic() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/v1/messages", exchange -> {
                lastRequestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                byte[] bytes = responseBody.get().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(responseStatus.get(), bytes.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(bytes);
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
