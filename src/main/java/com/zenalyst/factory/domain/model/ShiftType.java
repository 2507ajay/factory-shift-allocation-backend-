package com.zenalyst.factory.domain.model;

import java.time.LocalTime;

/**
 * Represents the three operational shifts running in the factory.
 */
public enum ShiftType {
    MORNING("Morning Shift", LocalTime.of(6, 0), LocalTime.of(14, 0), 8.0),
    AFTERNOON("Afternoon Shift", LocalTime.of(14, 0), LocalTime.of(22, 0), 8.0),
    NIGHT("Night Shift", LocalTime.of(22, 0), LocalTime.of(6, 0), 8.0);

    private final String displayName;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final double durationHours;

    ShiftType(String displayName, LocalTime startTime, LocalTime endTime, double durationHours) {
        this.displayName = displayName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationHours = durationHours;
    }

    public String getDisplayName() {
        return displayName;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public double getDurationHours() {
        return durationHours;
    }

    /**
     * Returns the preceding shift (useful to determine eligible holdover candidates).
     */
    public ShiftType getPrecedingShift() {
        return switch (this) {
            case MORNING -> NIGHT;
            case AFTERNOON -> MORNING;
            case NIGHT -> AFTERNOON;
        };
    }

    /**
     * Returns the following shift.
     */
    public ShiftType getNextShift() {
        return switch (this) {
            case MORNING -> AFTERNOON;
            case AFTERNOON -> NIGHT;
            case NIGHT -> MORNING;
        };
    }
}
