package com.zenalyst.factory.domain.model;

/**
 * Real-time attendance state of an operator at morning roll call.
 */
public enum AttendanceStatus {
    SCHEDULED("Scheduled to work today"),
    PRESENT("Checked in / On site"),
    ABSENT_UNPLANNED("Unplanned absence / No-show at start"),
    LATE("Reported late (delayed arrival)"),
    ON_APPROVED_LEAVE("Planned vacation/PTO/medical leave");

    private final String description;

    AttendanceStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isAvailableOnSite() {
        return this == PRESENT;
    }
}
