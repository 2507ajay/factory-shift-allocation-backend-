package com.zenalyst.factory.domain.model;

/**
 * Categorizes how an operator was scheduled onto a machine.
 * Useful for labor cost accounting and fairness audits.
 */
public enum AssignmentType {
    PRIMARY_SCHEDULED("Regular rostered operator present on site", 0.0),
    ONSITE_REALLOCATION("Cross-trained operator reallocated from a lower-priority machine or floater pool", 0.0),
    OVERTIME_HOLDOVER("Holdover overtime from preceding shift (staying beyond regular shift)", 1.5),
    OVERTIME_CALL_IN("Emergency call-in of off-duty operator on rest day or subsequent shift", 1.5),
    SUPERVISOR_OVERRIDE("Manual assignment pinned directly by supervisor", 1.0);

    private final String description;
    private final double defaultOvertimeMultiplier;

    AssignmentType(String description, double defaultOvertimeMultiplier) {
        this.description = description;
        this.defaultOvertimeMultiplier = defaultOvertimeMultiplier;
    }

    public String getDescription() {
        return description;
    }

    public double getDefaultOvertimeMultiplier() {
        return defaultOvertimeMultiplier;
    }

    public boolean isOvertime() {
        return this == OVERTIME_HOLDOVER || this == OVERTIME_CALL_IN;
    }
}
