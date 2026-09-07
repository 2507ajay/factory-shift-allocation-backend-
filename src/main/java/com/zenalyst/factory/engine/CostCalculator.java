package com.zenalyst.factory.engine;

import com.zenalyst.factory.config.FactoryPolicyProperties;
import com.zenalyst.factory.domain.model.AssignmentType;
import com.zenalyst.factory.domain.model.Operator;
import org.springframework.stereotype.Component;

/**
 * Computes labor and overtime compensation for machine assignments.
 */
@Component
public class CostCalculator {

    private final FactoryPolicyProperties policy;

    public CostCalculator(FactoryPolicyProperties policy) {
        this.policy = policy;
    }

    /**
     * Calculates the exact wage cost for a shift assignment.
     */
    public CostBreakdown calculateCost(Operator operator, AssignmentType assignmentType,
                                       double regularHours, double overtimeHours) {
        if (operator == null) {
            return new CostBreakdown(0.0, 0.0, 0.0, 1.0);
        }

        double rate = operator.getBaseHourlyRate();
        double regularCost = regularHours * rate;

        double otMultiplier = policy.getStandardOvertimeMultiplier();
        // If operator has worked 6 consecutive days, 7th day is double-time (2.0x)
        if (operator.getConsecutiveDaysWorked() >= 6) {
            otMultiplier = policy.getDoubleOvertimeMultiplier();
        }

        double overtimeCost = overtimeHours * rate * otMultiplier;
        double totalCost = Math.round((regularCost + overtimeCost) * 100.0) / 100.0;

        return new CostBreakdown(regularCost, overtimeCost, totalCost, otMultiplier);
    }

    public record CostBreakdown(
            double regularCost,
            double overtimeCost,
            double totalCost,
            double effectiveOvertimeMultiplier
    ) {
    }
}
