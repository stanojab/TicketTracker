package com.tickettriage.triage;

/**
 * Classifies a customer email. There are two implementations, and the active
 * Spring profile decides which one becomes the bean:
 * <ul>
 *     <li>{@link SpringAiTriageClient}: calls a real LLM (default)</li>
 *     <li>{@link MockTriageClient}: keyword rules, no API key needed ("mock" profile)</li>
 * </ul>
 * Implementations throw an exception when triage is not possible; the caller decides on a fallback.
 */
public interface TriageClient {

    TriageResult triage(String subject, String body);
}
