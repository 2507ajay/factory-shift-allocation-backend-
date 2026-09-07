package com.zenalyst.factory.dto;

import com.zenalyst.factory.domain.model.ShiftType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ManualOverrideRequest(
        @NotBlank String machineCode,
        @NotBlank String operatorEmployeeCode,
        ShiftType shiftType,
        LocalDate date,
        boolean lockAssignment,
        String supervisorNotes
) {
    public ManualOverrideRequest {
        if (shiftType == null) {
            shiftType = ShiftType.MORNING;
        }
        if (date == null) {
            date = LocalDate.now();
        }
    }
}
