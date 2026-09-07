package com.zenalyst.factory.dto;

import com.zenalyst.factory.domain.model.Machine;
import com.zenalyst.factory.domain.model.MachineAssignment;
import com.zenalyst.factory.domain.model.ShiftType;
import com.zenalyst.factory.domain.model.TriageAlert;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ShiftCoverageResponse(
        String planId,
        ShiftType shiftType,
        LocalDate shiftDate,
        LocalDateTime generatedAt,
        int totalMachines,
        int coveredMachines,
        int unstaffedMachines,
        List<MachineAssignment> assignments,
        List<Machine> unassignedMachines,
        List<TriageAlert> triageAlerts,
        List<WillingOperatorStatusDto> willingOperatorsSummary,
        double totalRegularLaborCost,
        double totalOvertimeCost,
        double totalShiftLaborCost,
        double totalOvertimeHours,
        double averageWorkforceFatigue,
        String operationalSummaryMessage
) {
}
