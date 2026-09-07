package com.zenalyst.factory.engine;

import com.zenalyst.factory.config.FactoryPolicyProperties;
import com.zenalyst.factory.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FairnessAndBurnoutTest {

    private FairnessEvaluator fairnessEvaluator;
    private AssignmentSolver solver;
    private FactoryPolicyProperties policy;

    @BeforeEach
    void setUp() {
        policy = new FactoryPolicyProperties();
        ConstraintValidator validator = new ConstraintValidator(policy);
        fairnessEvaluator = new FairnessEvaluator(policy);
        CostCalculator costCalculator = new CostCalculator(policy);
        solver = new AssignmentSolver(validator, fairnessEvaluator, costCalculator, policy);
    }

    @Test
    @DisplayName("FairnessEvaluator should severely penalize overburdening a willing operator")
    void shouldPenalizeWillingOperatorOveruse() {
        // Operator A: One of the 'Willing Four' (high willingness, 14h OT, 5 coverage shifts, 6 consecutive days)
        Operator willingOp = new Operator("OP-W1", "Dave Miller", ShiftType.NIGHT, null, 40.0, 0.95, 14.0, 5);
        willingOp.setConsecutiveDaysWorked(6);

        // Operator B: Rested operator with 0h OT
        Operator restedOp = new Operator("OP-R1", "Nathan Drake", ShiftType.NIGHT, null, 38.0, 0.60, 0.0, 0);
        restedOp.setConsecutiveDaysWorked(2);

        double willingPenalty = fairnessEvaluator.calculatePenalty(willingOp, AssignmentType.OVERTIME_HOLDOVER);
        double restedPenalty = fairnessEvaluator.calculatePenalty(restedOp, AssignmentType.OVERTIME_HOLDOVER);

        // Willing operator should have a drastically higher fairness penalty to deter the solver
        assertThat(willingPenalty).isGreaterThan(restedPenalty * 2.5);
    }

    @Test
    @DisplayName("Solver should pick rested certified operator and shield willing operator from burnout")
    void shouldShieldWillingOperatorWhenAlternativeExists() {
        LocalDate today = LocalDate.now();
        Certification cncCert = new Certification("CNC_5AXIS", "CNC", CertificationLevel.LEVEL_3_CRITICAL, today.plusYears(1));

        Machine cncMachine = new Machine("M01", "5-Axis CNC Alpha", ProductionCell.PRECISION_MACHINING,
                "CNC_5AXIS", CertificationLevel.LEVEL_3_CRITICAL, 1, 950.0);

        // Candidate 1: The willing operator (Dave Miller, near burnout)
        Operator dave = new Operator("OP-WILLING-1", "Dave Miller", ShiftType.NIGHT, null, 40.0, 0.95, 13.0, 5);
        dave.setAttendanceStatus(AttendanceStatus.PRESENT);
        dave.setConsecutiveDaysWorked(6);
        dave.setLastShiftEndTime(LocalDateTime.now().minusHours(8));
        dave.addCertification(cncCert);

        // Candidate 2: Fresh night-shift operator (Nathan, 0h OT, well-rested)
        Operator nathan = new Operator("OP-N-01", "Nathan Drake", ShiftType.NIGHT, null, 38.0, 0.65, 0.0, 0);
        nathan.setAttendanceStatus(AttendanceStatus.PRESENT);
        nathan.setConsecutiveDaysWorked(2);
        nathan.setLastShiftEndTime(LocalDateTime.now().minusHours(8));
        nathan.addCertification(cncCert);

        var result = solver.solve(ShiftType.MORNING, today, List.of(cncMachine), List.of(dave, nathan), List.of());

        assertThat(result.assignments()).hasSize(1);
        MachineAssignment assignment = result.assignments().get(0);

        // The system MUST have chosen Nathan, protecting Dave from burnout!
        assertThat(assignment.getOperatorEmployeeCode()).isEqualTo("OP-N-01");
        assertThat(assignment.getOperatorName()).isEqualTo("Nathan Drake");
    }
}
