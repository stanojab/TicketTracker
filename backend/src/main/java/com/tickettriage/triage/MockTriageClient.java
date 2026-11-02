package com.tickettriage.triage;

import com.tickettriage.ticket.Category;
import com.tickettriage.ticket.Priority;
import com.tickettriage.ticket.Sentiment;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Fake classifier for the "mock" profile: simple keyword rules, so the app and the
 * tests run without an API key, offline and deterministically.
 * <p>
 * An email containing {@value #FAILURE_TRIGGER} makes it throw, which lets you see
 * the fallback behaviour (warning + default values) without a real outage.
 */
@Component
@Profile("mock")
public class MockTriageClient implements TriageClient {

    static final String FAILURE_TRIGGER = "simulate-ai-failure";

    private static final List<String> BILLING = List.of("invoice", "refund", "charge", "payment", "billing", "subscription", "price");
    private static final List<String> TECHNICAL = List.of("error", "bug", "crash", "500", "slow", "api", "down", "not working");
    private static final List<String> ACCOUNT = List.of("password", "login", "log in", "locked", "account", "sign in", "2fa");
    private static final List<String> FEEDBACK = List.of("suggestion", "feature", "love", "great", "feedback", "thank");
    private static final List<String> URGENT = List.of("urgent", "asap", "immediately", "outage", "down", "production");
    private static final List<String> HIGH = List.of("blocked", "cannot", "can't", "charged twice", "locked", "crash");
    private static final List<String> NEGATIVE = List.of("angry", "terrible", "unacceptable", "frustrated", "disappointed", "cannot", "can't", "not working", "wrong");
    private static final List<String> POSITIVE = List.of("love", "great", "thank", "excellent", "awesome", "fantastic");

    @Override
    public TriageResult triage(String subject, String body) {
        String text = (subject + " " + body).toLowerCase(Locale.ROOT);
        if (text.contains(FAILURE_TRIGGER)) {
            throw new IllegalStateException("Simulated AI failure");
        }

        Category category = category(text);
        Priority priority = priority(text, category);
        Sentiment sentiment = containsAny(text, NEGATIVE) ? Sentiment.NEGATIVE
                : containsAny(text, POSITIVE) ? Sentiment.POSITIVE
                : Sentiment.NEUTRAL;

        String summary = "Customer wrote about: " + subject;
        String reply = "Hello,\n\nthank you for contacting us about \"" + subject + "\". "
                + "We have received your message and a member of our team will get back to you shortly.\n\n"
                + "Best regards,\nSupport Team";
        return new TriageResult(category, priority, sentiment, summary, reply);
    }

    private static Category category(String text) {
        if (containsAny(text, BILLING)) return Category.BILLING;
        if (containsAny(text, TECHNICAL)) return Category.TECHNICAL;
        if (containsAny(text, ACCOUNT)) return Category.ACCOUNT;
        if (containsAny(text, FEEDBACK)) return Category.FEEDBACK;
        return Category.OTHER;
    }

    private static Priority priority(String text, Category category) {
        if (containsAny(text, URGENT)) return Priority.URGENT;
        if (containsAny(text, HIGH)) return Priority.HIGH;
        if (category == Category.FEEDBACK || category == Category.OTHER) return Priority.LOW;
        return Priority.MEDIUM;
    }

    private static boolean containsAny(String text, List<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }
}
