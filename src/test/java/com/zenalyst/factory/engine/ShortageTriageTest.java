package com.zenalyst.factory.engine;

import com.zenalyst.factory.config.FactoryPolicyProperties;
import com.zenalyst.factory.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ShortageTriageTest {

    private AssignmentSolver solver;

    @BeforeEach
    void setUp() {
        FactoryPolicyProperties policy = new FactoryPolicyProperties();
        ConstraintValidator validator = new ConstraintValidator(policy);
        FairnessEvaluator fairnessEvaluator = new FairnessEvaluator(policy);
        CostCalculator costCalculator = new CostCalculator(policy);
        solver = new AssignmentSolver(validator, fairnessEvaluator, costCalculator, policy);
    }

    @Test
    @DisplayName("Should gracefully reallocate certified operator from buffer machine to save critical bottleneck")
    void shouldTriageShortageByReallocatingFromBufferMachine() {
        LocalDate today = LocalDate.now();

        Certification cncCert = new Certification("CNC_5AXIS", "CNC", CertificationLevel.LEVEL_3_CRITICAL, today.plusYears(1));
        Certification pkgCert = new Certification("PACKAGING_AUTO", "Packaging", CertificationLevel.LEVEL_1_BASIC, today.plusYears(1));

        // Machine 1: Critical 5-Axis CNC (Priority 1 Bottleneck)
        Machine cncBottleneck = new Machine("M01", "5-Axis CNC", ProductionCell.PRECISION_MACHINING,
                "CNC_5AXIS", CertificationLevel.LEVEL_3_CRITICAL, 1, 950.0);

        // Machine 2: Packaging Buffer (Priority 4 Low Criticality)
        Machine pkgBuffer = new Machine("M17", "Carton Packer", ProductionCell.FINISHING_PACKAGING,
                "PACKAGING_AUTO", CertificationLevel.LEVEL_1_BASIC, 4, 200.0);

        // Only one operator on site: Gavin, who is assigned to M17 but is cross-certified in CNC
        Operator crossTrainedOp = new Operator("OP-M17", "Gavin Reed", ShiftType.MORNING, "M17", 32.0, 0.5, 0.0, 0);
        crossTrainedOp.setAttendanceStatus(AttendanceStatus.PRESENT);
        crossTrainedOp.addCertification(pkgCert);
        crossTrainedOp.addCertification(cncCert);

        var result = solver.solve(ShiftType.MORNING, today,
                List.of(cncBottleneck, pkgBuffer),
                List.of(crossTrainedOp),
                List.of());

        // The critical bottleneck M01 must be staffed by Gavin
        assertThat(result.assignments()).hasSize(1);
        MachineAssignment m01Assignment = result.assignments().get(0);
        assertThat(m01Assignment.getMachineCode()).isEqualTo("M01");
        assertThat(m01Assignment.getOperatorEmployeeCode()).isEqualTo("OP-M17");
        assertThat(m01Assignment.getAssignmentType()).isEqualTo(AssignmentType.ONSITE_REALLOCATION);

        // Low-priority M17 should be unassigned with a triage alert explaining the reason
        assertThat(result.unassignedMachines()).extracting(Machine::getCode).contains("M17");
        assertThat(result.triageAlerts()).anyMatch(alert ->
                alert.alertType().equals("SHORTAGE_TRIAGE") && alert.machineCode().equals("M17")
        );
    }
}
