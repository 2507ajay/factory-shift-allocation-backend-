package com.zenalyst.factory.engine;

import com.zenalyst.factory.config.FactoryPolicyProperties;
import com.zenalyst.factory.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ConstraintValidatorTest {

    private ConstraintValidator validator;
    private FactoryPolicyProperties policy;

    @BeforeEach
    void setUp() {
        policy = new FactoryPolicyProperties();
        validator = new ConstraintValidator(policy);
    }

    @Test
    @DisplayName("Should strictly reject uncertified operator from Level 3 critical machine")
    void shouldRejectUncertifiedOperator() {
        Machine cncMachine = new Machine("M01", "5-Axis CNC Alpha", ProductionCell.PRECISION_MACHINING,
                "CNC_5AXIS", CertificationLevel.LEVEL_3_CRITICAL, 1, 950.0);

        Operator basicOp = new Operator("OP-01", "Basic Worker", ShiftType.MORNING, "M17", 25.0, 0.5, 0.0, 0);
        // Only has basic packaging cert, NOT CNC
        basicOp.addCertification(new Certification("PACKAGING_AUTO", "Packaging", CertificationLevel.LEVEL_1_BASIC, LocalDate.now().plusYears(1)));

        var result = validator.validate(basicOp, cncMachine, ShiftType.MORNING, LocalDate.now(),
                AssignmentType.PRIMARY_SCHEDULED, 0.0);

        assertThat(result.valid()).isFalse();
        assertThat(result.reason()).contains("lacks required certification");
    }

    @Test
    @DisplayName("Should accept certified operator with matching skill and level")
    void shouldAcceptCertifiedOperator() {
        Machine cncMachine = new Machine("M01", "5-Axis CNC Alpha", ProductionCell.PRECISION_MACHINING,
                "CNC_5AXIS", CertificationLevel.LEVEL_3_CRITICAL, 1, 950.0);

        Operator masterCnc = new Operator("OP-02", "Master Machinist", ShiftType.MORNING, "M01", 40.0, 0.5, 0.0, 0);
        masterCnc.addCertification(new Certification("CNC_5AXIS", "CNC 5-Axis", CertificationLevel.LEVEL_3_CRITICAL, LocalDate.now().plusYears(1)));

        var result = validator.validate(masterCnc, cncMachine, ShiftType.MORNING, LocalDate.now(),
                AssignmentType.PRIMARY_SCHEDULED, 0.0);

        assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("Should enforce statutory weekly overtime cap (16 hours)")
    void shouldEnforceWeeklyOvertimeCap() {
        Machine laser = new Machine("M07", "Laser System 1", ProductionCell.FABRICATION_FORMING,
                "LASER_CUT", CertificationLevel.LEVEL_2_STANDARD, 1, 700.0);

        Operator willingOp = new Operator("OP-03", "Exhausted Worker", ShiftType.NIGHT, null, 35.0, 0.95, 14.0, 5);
        willingOp.setOvertimeCapHours(16.0);
        willingOp.addCertification(new Certification("LASER_CUT", "Laser", CertificationLevel.LEVEL_2_STANDARD, LocalDate.now().plusYears(1)));

        // Requesting 4 hours of holdover overtime would push them to 18.0h (breaching 16.0h cap)
        var result = validator.validate(willingOp, laser, ShiftType.MORNING, LocalDate.now(),
                AssignmentType.OVERTIME_HOLDOVER, 4.0);

        assertThat(result.valid()).isFalse();
        assertThat(result.reason()).contains("Overtime cap breached");
    }

    @Test
    @DisplayName("Should enforce minimum mandatory rest interval between shifts")
    void shouldEnforceRestInterval() {
        Machine weld = new Machine("M10", "Weld Cell 1", ProductionCell.ASSEMBLY_WELDING,
                "ROBOTIC_WELD", CertificationLevel.LEVEL_2_STANDARD, 2, 500.0);

        Operator op = new Operator("OP-04", "Recent Worker", ShiftType.AFTERNOON, null, 36.0, 0.6, 0.0, 0);
        op.addCertification(new Certification("ROBOTIC_WELD", "Weld", CertificationLevel.LEVEL_2_STANDARD, LocalDate.now().plusYears(1)));
        // Operator finished their shift only 3 hours ago (mandatory rest is 8h)
        op.setLastShiftEndTime(LocalDateTime.of(LocalDate.now(), ShiftType.MORNING.getStartTime()).minusHours(3));

        var result = validator.validate(op, weld, ShiftType.MORNING, LocalDate.now(),
                AssignmentType.OVERTIME_CALL_IN, 8.0);

        assertThat(result.valid()).isFalse();
        assertThat(result.reason()).contains("Insufficient rest");
    }
}
