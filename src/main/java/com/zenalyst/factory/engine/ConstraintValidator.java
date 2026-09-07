package com.zenalyst.factory.engine;

import com.zenalyst.factory.config.FactoryPolicyProperties;
import com.zenalyst.factory.domain.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Enforces non-negotiable legal, statutory, safety, and certification constraints.
 * An assignment that violates any hard constraint is strictly disqualified.
 */
@Component
public class ConstraintValidator {

    private final FactoryPolicyProperties policy;

    public ConstraintValidator(FactoryPolicyProperties policy) {
        this.policy = policy;
    }

    /**
     * Verifies whether an operator can legally, safely, and technically be assigned to a machine.
     *
     * @param operator The candidate operator
     * @param machine The target machine to staff
     * @param shift The shift in question
     * @param date The date of the shift
     * @param assignmentType Proposed type of assignment (regular, holdover, call-in)
     * @param requestedHours Additional hours to work
     * @return ValidationResult indicating pass/fail with explicit human-readable reasons
     */
    public ValidationResult validate(Operator operator, Machine machine, ShiftType shift,
                                     LocalDate date, AssignmentType assignmentType, double requestedHours) {
        if (operator == null) {
            return ValidationResult.fail("No operator specified");
        }
        if (machine == null) {
            return ValidationResult.fail("No machine specified");
        }

        // 1. Mandatory Certification Check (Life-Safety & Technical Qualification)
        if (!operator.isCertifiedFor(machine, date)) {
            return ValidationResult.fail(String.format(
                    "Operator %s lacks required certification (%s %s) for machine %s",
                    operator.getFullName(),
                    machine.getRequiredSkillCode() != null ? machine.getRequiredSkillCode() : "GENERAL",
                    machine.getRequiredCertLevel(),
                    machine.getCode()
            ));
        }

        // 2. Overtime Statutory/Contractual Cap Enforcement
        if (assignmentType.isOvertime()) {
            if (!operator.canLegallyWorkOvertime(requestedHours)) {
                return ValidationResult.fail(String.format(
                        "Overtime cap breached: %s has already worked %.1fh OT this week (cap is %.1fh). Adding %.1fh would violate policy.",
                        operator.getFullName(),
                        operator.getWeeklyOvertimeHoursWorked(),
                        operator.getOvertimeCapHours(),
                        requestedHours
                ));
            }

            // 3. Shift Duration Limit (Fatigue safety: max 12 hours continuous work)
            double existingShiftHours = (operator.getPrimaryShift() == shift) ? 8.0 : 0.0;
            if (assignmentType == AssignmentType.OVERTIME_HOLDOVER) {
                // If staying over from preceding shift, total consecutive hours is 8h prior + requestedHours
                existingShiftHours = 8.0;
            }
            if ((existingShiftHours + requestedHours) > policy.getMaxShiftDurationHours()) {
                return ValidationResult.fail(String.format(
                        "Max continuous shift duration (%.1fh) exceeded: operator would be working %.1fh consecutively.",
                        policy.getMaxShiftDurationHours(),
                        (existingShiftHours + requestedHours)
                ));
            }

            // 4. Mandatory Rest Interval Enforcement (e.g. 8h between shifts)
            if (assignmentType == AssignmentType.OVERTIME_CALL_IN) {
                LocalDateTime targetShiftStart = LocalDateTime.of(date, shift.getStartTime());
                if (!operator.hasSufficientRest(targetShiftStart, policy.getMinRestHoursBetweenShifts())) {
                    return ValidationResult.fail(String.format(
                            "Insufficient rest: operator last finished shift at %s. Mandatory rest is %.1fh.",
                            operator.getLastShiftEndTime(),
                            policy.getMinRestHoursBetweenShifts()
                    ));
                }
            }
        }

        // 5. Approved Leave Check
        if (operator.getAttendanceStatus() == AttendanceStatus.ON_APPROVED_LEAVE) {
            return ValidationResult.fail(String.format(
                    "Operator %s is on approved PTO / medical leave", operator.getFullName()
            ));
        }

        return ValidationResult.pass();
    }

    public record ValidationResult(boolean valid, String reason) {
        public static ValidationResult pass() {
            return new ValidationResult(true, "All constraints satisfied");
        }

        public static ValidationResult fail(String reason) {
            return new ValidationResult(false, reason);
        }
    }
}
