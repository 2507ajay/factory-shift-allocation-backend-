package com.zenalyst.factory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "factory.policy")
public class FactoryPolicyProperties {

    private double maxWeeklyOvertimeHours = 16.0;
    private double maxShiftDurationHours = 12.0;
    private double minRestHoursBetweenShifts = 8.0;

    private double burnoutWarningThreshold = 0.65;
    private double burnoutCriticalThreshold = 0.85;

    private double standardOvertimeMultiplier = 1.5;
    private double doubleOvertimeMultiplier = 2.0;

    private Weights weights = new Weights();

    public static class Weights {
        private double fairness = 45.0;
        private double cost = 30.0;
        private double experience = 25.0;

        public double getFairness() {
            return fairness;
        }

        public void setFairness(double fairness) {
            this.fairness = fairness;
        }

        public double getCost() {
            return cost;
        }

        public void setCost(double cost) {
            this.cost = cost;
        }

        public double getExperience() {
            return experience;
        }

        public void setExperience(double experience) {
            this.experience = experience;
        }
    }

    public double getMaxWeeklyOvertimeHours() {
        return maxWeeklyOvertimeHours;
    }

    public void setMaxWeeklyOvertimeHours(double maxWeeklyOvertimeHours) {
        this.maxWeeklyOvertimeHours = maxWeeklyOvertimeHours;
    }

    public double getMaxShiftDurationHours() {
        return maxShiftDurationHours;
    }

    public void setMaxShiftDurationHours(double maxShiftDurationHours) {
        this.maxShiftDurationHours = maxShiftDurationHours;
    }

    public double getMinRestHoursBetweenShifts() {
        return minRestHoursBetweenShifts;
    }

    public void setMinRestHoursBetweenShifts(double minRestHoursBetweenShifts) {
        this.minRestHoursBetweenShifts = minRestHoursBetweenShifts;
    }

    public double getBurnoutWarningThreshold() {
        return burnoutWarningThreshold;
    }

    public void setBurnoutWarningThreshold(double burnoutWarningThreshold) {
        this.burnoutWarningThreshold = burnoutWarningThreshold;
    }

    public double getBurnoutCriticalThreshold() {
        return burnoutCriticalThreshold;
    }

    public void setBurnoutCriticalThreshold(double burnoutCriticalThreshold) {
        this.burnoutCriticalThreshold = burnoutCriticalThreshold;
    }

    public double getStandardOvertimeMultiplier() {
        return standardOvertimeMultiplier;
    }

    public void setStandardOvertimeMultiplier(double standardOvertimeMultiplier) {
        this.standardOvertimeMultiplier = standardOvertimeMultiplier;
    }

    public double getDoubleOvertimeMultiplier() {
        return doubleOvertimeMultiplier;
    }

    public void setDoubleOvertimeMultiplier(double doubleOvertimeMultiplier) {
        this.doubleOvertimeMultiplier = doubleOvertimeMultiplier;
    }

    public Weights getWeights() {
        return weights;
    }

    public void setWeights(Weights weights) {
        this.weights = weights;
    }
}
