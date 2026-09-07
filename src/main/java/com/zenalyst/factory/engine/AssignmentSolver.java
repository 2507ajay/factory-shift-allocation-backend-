package com.zenalyst.factory.engine;

import com.zenalyst.factory.config.FactoryPolicyProperties;
import com.zenalyst.factory.domain.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Multi-objective solver for factory shift machine coverage.
 *
 * Balances:
 * 1. Strict certification & skill qualification (Life safety / technical)
 * 2. Overtime budget control & statutory caps
 * 3. Human fatigue & anti-burnout fairness (specifically protecting the "Willing Four")
 * 4. Machine criticality prioritization & graceful shortage triage
 */
@Component
public class AssignmentSolver {

    private final ConstraintValidator constraintValidator;
    private final FairnessEvaluator fairnessEvaluator;
    private final CostCalculator costCalculator;
    private final FactoryPolicyProperties policy;

    public AssignmentSolver(ConstraintValidator constraintValidator,
                            FairnessEvaluator fairnessEvaluator,
                            CostCalculator costCalculator,
                            FactoryPolicyProperties policy) {
        this.constraintValidator = constraintValidator;
        this.fairnessEvaluator = fairnessEvaluator;
        this.costCalculator = costCalculator;
        this.policy = policy;
    }

    /**
     * Solves shift coverage across all machines given the workforce state and attendance roll call.
     */
    public SolverResult solve(ShiftType targetShift, LocalDate targetDate,
                              List<Machine> machines, List<Operator> allOperators,
                              List<MachineAssignment> existingPinnedAssignments) {

        List<MachineAssignment> finalAssignments = new ArrayList<>();
        List<TriageAlert> triageAlerts = new ArrayList<>();
        Set<String> assignedMachineCodes = new HashSet<>();
        Set<String> assignedOperatorCodes = new HashSet<>();

        // Sort machines by operational priority: P1 (Bottlenecks) -> P4 (Buffer/Packaging)
        List<Machine> sortedMachines = new ArrayList<>(machines);
        sortedMachines.sort(Comparator.comparingInt(Machine::getPriorityRank).thenComparing(Machine::getCode));

        // -------------------------------------------------------------
        // PASS 1: Honor and preserve supervisor manual pins
        // -------------------------------------------------------------
        if (existingPinnedAssignments != null) {
            for (MachineAssignment pin : existingPinnedAssignments) {
                if (pin.isPinned()) {
                    finalAssignments.add(pin);
                    assignedMachineCodes.add(pin.getMachineCode());
                    if (pin.getOperatorEmployeeCode() != null) {
                        assignedOperatorCodes.add(pin.getOperatorEmployeeCode());
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // PASS 2: Primary Scheduled Assignment
        // Assign regular rostered operators who are PRESENT on site to their default machines
        // -------------------------------------------------------------
        Map<String, Operator> presentDefaultOperators = new HashMap<>();
        for (Operator op : allOperators) {
            if (op.getPrimaryShift() == targetShift &&
                    op.getAttendanceStatus() == AttendanceStatus.PRESENT &&
                    op.getDefaultMachineCode() != null &&
                    !assignedOperatorCodes.contains(op.getEmployeeCode())) {
                presentDefaultOperators.put(op.getDefaultMachineCode(), op);
            }
        }

        for (Machine machine : sortedMachines) {
            if (assignedMachineCodes.contains(machine.getCode())) {
                continue;
            }

            Operator defaultOp = presentDefaultOperators.get(machine.getCode());
            if (defaultOp != null && !assignedOperatorCodes.contains(defaultOp.getEmployeeCode())) {
                var validation = constraintValidator.validate(defaultOp, machine, targetShift, targetDate,
                        AssignmentType.PRIMARY_SCHEDULED, 0.0);

                if (validation.valid()) {
                    var cost = costCalculator.calculateCost(defaultOp, AssignmentType.PRIMARY_SCHEDULED, 8.0, 0.0);
                    MachineAssignment assignment = new MachineAssignment(
                            machine, defaultOp, targetShift, targetDate,
                            AssignmentType.PRIMARY_SCHEDULED, 8.0, 0.0, cost.totalCost(),
                            "Primary rostered operator present on site. Verified certified.",
                            false
                    );
                    finalAssignments.add(assignment);
                    assignedMachineCodes.add(machine.getCode());
                    assignedOperatorCodes.add(defaultOp.getEmployeeCode());
                }
            }
        }

        // -------------------------------------------------------------
        // PASS 3: Intelligent Coverage of Unassigned Machines
        // For machines where the primary operator is absent or unassigned,
        // search candidates across On-Site Floaters, Holdover OT, and Call-In OT.
        // -------------------------------------------------------------
        for (Machine machine : sortedMachines) {
            if (assignedMachineCodes.contains(machine.getCode())) {
                continue;
            }

            CandidateSelection bestCandidate = findBestCandidate(
                    machine, targetShift, targetDate, allOperators, assignedOperatorCodes
            );

            if (bestCandidate != null) {
                Operator op = bestCandidate.operator();
                AssignmentType type = bestCandidate.assignmentType();
                double regularHours = (type.isOvertime() && op.getPrimaryShift() != targetShift) ? 0.0 : 8.0;
                double otHours = bestCandidate.overtimeHours();

                var cost = costCalculator.calculateCost(op, type, regularHours, otHours);
                double penalty = fairnessEvaluator.calculatePenalty(op, type);

                String decisionReason = buildDecisionReason(op, type, penalty, cost.totalCost(), bestCandidate.explanation());

                MachineAssignment assignment = new MachineAssignment(
                        machine, op, targetShift, targetDate,
                        type, regularHours, otHours, cost.totalCost(),
                        decisionReason, false
                );
                finalAssignments.add(assignment);
                assignedMachineCodes.add(machine.getCode());
                assignedOperatorCodes.add(op.getEmployeeCode());

                // Check for Burnout Warning
                if (fairnessEvaluator.isBurnoutAlert(op)) {
                    triageAlerts.add(new TriageAlert(
                            "BURNOUT_WARNING",
                            machine.getCode(),
                            machine.getName(),
                            machine.getPriorityRank(),
                            "WARNING",
                            String.format("Operator %s assigned to %s via %s. Weekly OT will reach %.1fh. Burnout Index: %.2f.",
                                    op.getFullName(), machine.getCode(), type, (op.getWeeklyOvertimeHoursWorked() + otHours), op.calculateBurnoutIndex()),
                            "Ensure operator receives mandatory recovery rest tomorrow. Proactively recruit additional certified operators for this skill."
                    ));
                }
            }
        }

        // -------------------------------------------------------------
        // PASS 4: Shortage Triage & Graceful Degradation
        // If a high-priority machine (P1/P2) is still unassigned,
        // see if a certified operator assigned to a lower-priority (P3/P4) machine can be reallocated.
        // -------------------------------------------------------------
        for (Machine highPriMachine : sortedMachines) {
            if (assignedMachineCodes.contains(highPriMachine.getCode())) {
                continue;
            }
            if (highPriMachine.getPriorityRank() > 2) {
                // Only critical bottleneck machines warrant cannibalizing lower lines
                continue;
            }

            // Search for lower priority assignments that can donate a certified operator
            MachineAssignment donorAssignment = null;
            Operator donorOperator = null;
            for (MachineAssignment existing : finalAssignments) {
                if (existing.isPinned() || existing.getMachinePriority() <= highPriMachine.getPriorityRank()) {
                    continue;
                }
                String opCode = existing.getOperatorEmployeeCode();
                if (opCode == null) continue;

                Operator candidateOp = allOperators.stream()
                        .filter(o -> Objects.equals(o.getEmployeeCode(), opCode))
                        .findFirst().orElse(null);

                if (candidateOp != null && candidateOp.isCertifiedFor(highPriMachine, targetDate)) {
                    donorAssignment = existing;
                    donorOperator = candidateOp;
                    break;
                }
            }

            if (donorAssignment != null && donorOperator != null) {
                // Reallocate the operator to the critical bottleneck machine!
                finalAssignments.remove(donorAssignment);
                assignedMachineCodes.remove(donorAssignment.getMachineCode());

                var cost = costCalculator.calculateCost(donorOperator, AssignmentType.ONSITE_REALLOCATION, 8.0, 0.0);
                MachineAssignment triageAssignment = new MachineAssignment(
                        highPriMachine, donorOperator, targetShift, targetDate,
                        AssignmentType.ONSITE_REALLOCATION, 8.0, 0.0, cost.totalCost(),
                        String.format("TRIAGE SWAP: Reallocated from %s (P%d) to protect critical bottleneck line. Certified Level %s.",
                                donorAssignment.getMachineCode(), donorAssignment.getMachinePriority(), highPriMachine.getRequiredCertLevel()),
                        false
                );
                finalAssignments.add(triageAssignment);
                assignedMachineCodes.add(highPriMachine.getCode());

                triageAlerts.add(new TriageAlert(
                        "SHORTAGE_TRIAGE",
                        donorAssignment.getMachineCode(),
                        donorAssignment.getMachineName(),
                        donorAssignment.getMachinePriority(),
                        "HIGH",
                        String.format("Machine %s temporarily idled to save critical bottleneck %s. Operator %s was transferred.",
                                donorAssignment.getMachineCode(), highPriMachine.getCode(), donorOperator.getFullName()),
                        "Utilize existing downstream buffer inventory while buffer machine is idled. Contact on-call standby."
                ));
            }
        }

        // -------------------------------------------------------------
        // PASS 5: Unassigned Machines Reporting
        // Any machine that remains unassigned produces a clear incident alert
        // -------------------------------------------------------------
        List<Machine> unassignedMachines = new ArrayList<>();
        for (Machine m : sortedMachines) {
            if (!assignedMachineCodes.contains(m.getCode())) {
                unassignedMachines.add(m);
                triageAlerts.add(new TriageAlert(
                        "UNSTAFFED_MACHINE",
                        m.getCode(),
                        m.getName(),
                        m.getPriorityRank(),
                        m.getPriorityRank() <= 2 ? "CRITICAL" : "WARNING",
                        String.format("No qualified operator available on site or eligible for overtime to run %s (Req: %s %s).",
                                m.getCode(), m.getRequiredSkillCode() != null ? m.getRequiredSkillCode() : "GENERAL", m.getRequiredCertLevel()),
                        "Line cannot start this machine. Review certification pipeline or authorize emergency contractor."
                ));
            }
        }

        // Calculate aggregate statistics
        double totalRegularCost = 0.0;
        double totalOvertimeCost = 0.0;
        double totalOvertimeHours = 0.0;
        double sumFatigue = 0.0;
        int activeCount = 0;

        for (MachineAssignment a : finalAssignments) {
            if (a.getOperatorId() != null) {
                totalRegularCost += (a.getRegularHours() * (a.getTotalCost() / Math.max(1.0, a.getRegularHours() + a.getOvertimeHours() * 1.5)));
                totalOvertimeCost += (a.getOvertimeHours() > 0 ? (a.getTotalCost() - (a.getRegularHours() * (a.getTotalCost() / Math.max(1.0, a.getRegularHours() + a.getOvertimeHours() * 1.5)))) : 0.0);
                totalOvertimeHours += a.getOvertimeHours();
                sumFatigue += a.getFatigueScoreAtAssignment();
                activeCount++;
            }
        }

        double avgFatigue = activeCount > 0 ? (sumFatigue / activeCount) : 0.0;

        return new SolverResult(
                finalAssignments,
                unassignedMachines,
                triageAlerts,
                Math.round(totalRegularCost * 100.0) / 100.0,
                Math.round(totalOvertimeCost * 100.0) / 100.0,
                Math.round((totalRegularCost + totalOvertimeCost) * 100.0) / 100.0,
                totalOvertimeHours,
                Math.round(avgFatigue * 100.0) / 100.0
        );
    }

    /**
     * Evaluates all potential candidates across on-site floaters, holdovers, and call-ins,
     * applying our multi-objective scoring formula.
     */
    private CandidateSelection findBestCandidate(Machine machine, ShiftType targetShift, LocalDate date,
                                                 List<Operator> allOperators, Set<String> assignedOperatorCodes) {

        List<ScoredCandidate> candidates = new ArrayList<>();

        for (Operator op : allOperators) {
            if (assignedOperatorCodes.contains(op.getEmployeeCode())) {
                continue;
            }

            // Candidate Category 1: On-site present operator without machine assignment (Floater/Cross-trained)
            if (op.getPrimaryShift() == targetShift && op.getAttendanceStatus() == AttendanceStatus.PRESENT) {
                var validation = constraintValidator.validate(op, machine, targetShift, date,
                        AssignmentType.ONSITE_REALLOCATION, 0.0);
                if (validation.valid()) {
                    double score = computeScore(op, machine, AssignmentType.ONSITE_REALLOCATION, 8.0, 0.0);
                    candidates.add(new ScoredCandidate(op, AssignmentType.ONSITE_REALLOCATION, 0.0, score, "On-site present cross-trained operator. Zero overtime cost."));
                }
            }

            // Candidate Category 2: Preceding shift Holdover Overtime (e.g. Night Shift ending at 6:00 AM)
            ShiftType precedingShift = targetShift.getPrecedingShift();
            if (op.getPrimaryShift() == precedingShift && op.getAttendanceStatus() == AttendanceStatus.PRESENT) {
                double holdoverHours = 4.0; // Standard 4-hour holdover block to maintain safety
                var validation = constraintValidator.validate(op, machine, targetShift, date,
                        AssignmentType.OVERTIME_HOLDOVER, holdoverHours);
                if (validation.valid()) {
                    double score = computeScore(op, machine, AssignmentType.OVERTIME_HOLDOVER, 0.0, holdoverHours);
                    candidates.add(new ScoredCandidate(op, AssignmentType.OVERTIME_HOLDOVER, holdoverHours, score, "Preceding shift certified holdover. Rest & OT cap checked."));
                }
            }

            // Candidate Category 3: Off-duty Call-In Overtime (Rest day or subsequent shift)
            if (op.getPrimaryShift() != targetShift && op.getPrimaryShift() != precedingShift &&
                    op.getAttendanceStatus() != AttendanceStatus.ON_APPROVED_LEAVE &&
                    op.getAttendanceStatus() != AttendanceStatus.ABSENT_UNPLANNED) {
                double callInHours = 8.0;
                var validation = constraintValidator.validate(op, machine, targetShift, date,
                        AssignmentType.OVERTIME_CALL_IN, callInHours);
                if (validation.valid()) {
                    double score = computeScore(op, machine, AssignmentType.OVERTIME_CALL_IN, 0.0, callInHours);
                    candidates.add(new ScoredCandidate(op, AssignmentType.OVERTIME_CALL_IN, callInHours, score, "Off-duty certified call-in. Rest interval verified."));
                }
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        // Select candidate with the highest overall optimization score
        candidates.sort(Comparator.comparingDouble(ScoredCandidate::score).reversed());
        ScoredCandidate top = candidates.get(0);
        return new CandidateSelection(top.operator(), top.type(), top.overtimeHours(), top.explanation());
    }

    /**
     * Multi-objective optimization scoring formula:
     * Score = - (W_fairness * fairnessPenalty) - (W_cost * costPenalty) + (W_experience * experienceBonus)
     */
    private double computeScore(Operator op, Machine machine, AssignmentType type,
                                double regularHours, double overtimeHours) {

        double fairnessPenalty = fairnessEvaluator.calculatePenalty(op, type);
        var cost = costCalculator.calculateCost(op, type, regularHours, overtimeHours);

        // Normalize overtime cost to a manageable scale
        double costPenalty = cost.overtimeCost() / 10.0;

        // Experience bonus
        double experienceBonus = 0.0;
        if (machine.getCode().equals(op.getDefaultMachineCode())) {
            experienceBonus += 25.0; // Familiarity with this specific equipment
        }
        for (Certification c : op.getCertifications()) {
            if (c.getLevel().getRank() > machine.getRequiredCertLevel().getRank()) {
                experienceBonus += 10.0; // Higher mastery level
            }
        }

        double wFair = policy.getWeights().getFairness();
        double wCost = policy.getWeights().getCost();
        double wExp = policy.getWeights().getExperience();

        return -(wFair * fairnessPenalty) - (wCost * costPenalty) + (wExp * experienceBonus);
    }

    private String buildDecisionReason(Operator op, AssignmentType type, double fairnessPenalty,
                                       double totalCost, String baseExplanation) {
        StringBuilder sb = new StringBuilder(baseExplanation);
        sb.append(String.format(" [Fairness Pen: %.1f | Est Cost: $%.2f | Burnout: %.2f]",
                fairnessPenalty, totalCost, op.calculateBurnoutIndex()));

        if (op.getWillingnessScore() >= 0.85 && op.calculateBurnoutIndex() < policy.getBurnoutWarningThreshold()) {
            sb.append(" (Willing operator eligible without burnout threshold breach).");
        }
        return sb.toString();
    }

    private record ScoredCandidate(Operator operator, AssignmentType type, double overtimeHours,
                                   double score, String explanation) {}

    public record CandidateSelection(Operator operator, AssignmentType assignmentType,
                                     double overtimeHours, String explanation) {}

    public record SolverResult(
            List<MachineAssignment> assignments,
            List<Machine> unassignedMachines,
            List<TriageAlert> triageAlerts,
            double totalRegularCost,
            double totalOvertimeCost,
            double totalCost,
            double totalOvertimeHours,
            double averageFatigueIndex
    ) {}
}
