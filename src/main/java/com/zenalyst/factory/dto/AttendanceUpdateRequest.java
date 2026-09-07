package com.zenalyst.factory.dto;

import com.zenalyst.factory.domain.model.AttendanceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AttendanceUpdateRequest(
        @NotBlank String employeeCode,
        @NotNull AttendanceStatus status,
        String reason
) {
}
