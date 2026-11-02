package com.tickettriage.triage;

import com.tickettriage.ticket.Category;
import com.tickettriage.ticket.Priority;
import com.tickettriage.ticket.Sentiment;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockTriageClientTest {

    private final MockTriageClient client = new MockTriageClient();

    @Test
    void classifiesBillingProblem() {
        TriageResult result = client.triage("Charged twice", "I was charged twice for my subscription, this is wrong.");

        assertThat(result.category()).isEqualTo(Category.BILLING);
        assertThat(result.priority()).isEqualTo(Priority.HIGH);
        assertThat(result.sentiment()).isEqualTo(Sentiment.NEGATIVE);
        assertThat(result.summary()).contains("Charged twice");
        assertThat(result.suggestedReply()).isNotBlank();
    }

    @Test
    void classifiesUrgentOutage() {
        TriageResult result = client.triage("Production down", "Your API returns errors, our shop is down!");

        assertThat(result.category()).isEqualTo(Category.TECHNICAL);
        assertThat(result.priority()).isEqualTo(Priority.URGENT);
    }

    @Test
    void classifiesPositiveFeedback() {
        TriageResult result = client.triage("Kudos", "I love the new dashboard, great work and thank you!");

        assertThat(result.category()).isEqualTo(Category.FEEDBACK);
        assertThat(result.priority()).isEqualTo(Priority.LOW);
        assertThat(result.sentiment()).isEqualTo(Sentiment.POSITIVE);
    }

    @Test
    void fallsBackToOtherWhenNothingMatches() {
        TriageResult result = client.triage("Office hours", "Are you open on Sundays?");

        assertThat(result.category()).isEqualTo(Category.OTHER);
        assertThat(result.priority()).isEqualTo(Priority.LOW);
        assertThat(result.sentiment()).isEqualTo(Sentiment.NEUTRAL);
    }

    @Test
    void throwsOnFailureTrigger() {
        assertThatThrownBy(() -> client.triage("Test", "please " + MockTriageClient.FAILURE_TRIGGER))
                .isInstanceOf(IllegalStateException.class);
    }
}
