package com.zenalyst.factory.dto;

/**
 * Transparency monitor tracking the specific status of the "Willing Four" operators,
 * proving that the system actively shields them from unfair exploitation.
 */
public record WillingOperatorStatusDto(
        String employeeCode,
        String fullName,
        double willingnessScore,
        double weeklyOvertimeHours,
        double burnoutIndex,
        String protectionStatus,  // PROTECTED_FROM_OVERLOAD, WORKING_REGULAR, RESTING_OFF_DUTY, EMERGENCY_BACKSTOP
        String auditMessage
) {
}
