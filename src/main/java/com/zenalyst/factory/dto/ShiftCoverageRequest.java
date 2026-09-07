package com.zenalyst.factory.dto;

import com.zenalyst.factory.domain.model.ShiftType;
import java.time.LocalDate;

public record ShiftCoverageRequest(
        ShiftType shiftType,
        LocalDate date,
        boolean simulateMorningCrisis,
        boolean resetOverrides
) {
    public ShiftCoverageRequest {
        if (shiftType == null) {
            shiftType = ShiftType.MORNING;
        }
        if (date == null) {
            date = LocalDate.now();
        }
    }
}
