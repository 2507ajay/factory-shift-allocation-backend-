package com.zenalyst.factory.engine;

import com.zenalyst.factory.config.FactoryPolicyProperties;
import com.zenalyst.factory.domain.model.AssignmentType;
import com.zenalyst.factory.domain.model.Operator;
import org.springframework.stereotype.Component;

/**
 * Quantifies fairness, fatigue, and prevents the "Willing Four" burnout syndrome.
 *
 * In factory operations, supervisors tend to repeatedly call the most cooperative,
 * versatile workers because they never say no. Over time, these willing workers
 * become exhausted and resentful.
 *
 * This evaluator introduces mathematical counter-pressure: penalizing candidates
 * who have already shouldered disproportionate coverage, thereby forcing the
 * assignment algorithm to distribute coverage across the wider qualified pool.
 */
@Component
public class FairnessEvaluator {

    private final FactoryPolicyProperties policy;

    public FairnessEvaluator(FactoryPolicyProperties policy) {
        this.policy = policy;
    }

    /**
     * Calculates the fairness penalty score for assigning an operator (lower penalty is better/fairer).
     *
     * @param operator The candidate operator
     * @param assignmentType Proposed assignment type
     * @return Score between 0.0 (perfectly fair / regular shift) to 500.0+ (severe burnout risk)
     */
    public double calculatePenalty(Operator operator, AssignmentType assignmentType) {
        // If an operator is just working their normal primary shift on site, no fairness penalty
        if (assignmentType == AssignmentType.PRIMARY_SCHEDULED) {
            return 0.0;
        }

        double burnoutIndex = operator.calculateBurnoutIndex();
        double otWorked = operator.getWeeklyOvertimeHoursWorked();
        double otCap = operator.getOvertimeCapHours();
        double willingness = operator.getWillingnessScore();
        int historicalCoverage = operator.getHistoricalCoverageCount();

        // 1. Base Fatigue Penalty (0 - 100)
        double baseFatiguePenalty = burnoutIndex * 100.0;

        // 2. Overtime Load Concentration Penalty (0 - 150)
        // Heavily penalizes candidates who already have high OT compared to peers
        double otRatio = Math.min(1.0, otWorked / Math.max(1.0, otCap));
        double otConcentrationPenalty = Math.pow(otRatio, 2.0) * 150.0;

        // 3. The "Anti-Exploitation" Surcharge (Specifically protects the "Willing Four")
        // If someone has high willingness (>= 0.80) and has been repeatedly drafted,
        // we add an exponential surcharge to ensure the system seeks other qualified people first.
        double antiExploitationSurcharge = 0.0;
        if (willingness >= 0.80) {
            // Factor how many times they've already stepped up
            double abuseFactor = historicalCoverage * 20.0 + (otWorked * 8.0);
            antiExploitationSurcharge = (willingness * 40.0) + abuseFactor;

            // Extra penalty if they are in the "quietly furious" zone
            if (burnoutIndex >= policy.getBurnoutWarningThreshold()) {
                antiExploitationSurcharge += 100.0;
            }
        }

        // 4. Consecutive Days Fatigue
        double consecutiveDaysPenalty = 0.0;
        if (operator.getConsecutiveDaysWorked() >= 5) {
            consecutiveDaysPenalty = (operator.getConsecutiveDaysWorked() - 4) * 35.0;
        }

        return Math.round((baseFatiguePenalty + otConcentrationPenalty + antiExploitationSurcharge + consecutiveDaysPenalty) * 10.0) / 10.0;
    }

    /**
     * Checks if this candidate should be flagged with an active burnout alert.
     */
    public boolean isBurnoutAlert(Operator operator) {
        return operator.calculateBurnoutIndex() >= policy.getBurnoutWarningThreshold() ||
                operator.getWeeklyOvertimeHoursWorked() >= (policy.getMaxWeeklyOvertimeHours() * 0.80);
    }
}
