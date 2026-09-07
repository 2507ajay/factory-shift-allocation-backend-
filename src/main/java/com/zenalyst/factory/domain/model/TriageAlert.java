package com.zenalyst.factory.domain.model;

/**
 * Actionable operational alert generated when real-life factory chaos prevents normal staffing.
 * Explains root cause and suggests practical mitigations (e.g. idling buffer packaging vs halting CNC line).
 */
public record TriageAlert(
        String alertType,        // SHORTAGE, BURNOUT_PROTECTION, OVERTIME_CAP_EXCEEDED, IDLED_BUFFER
        String machineCode,
        String machineName,
        int priorityRank,
        String severity,         // CRITICAL, HIGH, WARNING, INFO
        String description,
        String suggestedMitigation
) {
}
