package com.zenalyst.factory.domain.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * An approved or recommended operational assignment of an operator to a specific machine.
 */
@Entity
@Table(name = "machine_assignments")
public class MachineAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String machineCode;

    @Column(nullable = false, length = 100)
    private String machineName;

    @Column(nullable = false)
    private int machinePriority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductionCell cell;

    private Long operatorId;

    @Column(length = 20)
    private String operatorEmployeeCode;

    @Column(nullable = false, length = 100)
    private String operatorName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShiftType shiftType;

    @Column(nullable = false)
    private LocalDate shiftDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssignmentType assignmentType;

    @Column(nullable = false)
    private double regularHours = 8.0;

    @Column(nullable = false)
    private double overtimeHours = 0.0;

    @Column(nullable = false)
    private double totalCost = 0.0;

    @Column(nullable = false)
    private double fatigueScoreAtAssignment = 0.0;

    @Column(nullable = false)
    private double willingnessScore = 0.5;

    @Column(length = 500)
    private String decisionReason;

    /**
     * If true, this assignment was pinned by the supervisor and cannot be altered by auto-solve.
     */
    @Column(nullable = false)
    private boolean pinned = false;

    @Column(nullable = false)
    private LocalDateTime assignedAt = LocalDateTime.now();

    public MachineAssignment() {
    }

    public MachineAssignment(Machine machine, Operator operator, ShiftType shiftType,
                             LocalDate shiftDate, AssignmentType assignmentType,
                             double regularHours, double overtimeHours, double totalCost,
                             String decisionReason, boolean pinned) {
        this.machineCode = machine.getCode();
        this.machineName = machine.getName();
        this.machinePriority = machine.getPriorityRank();
        this.cell = machine.getCell();

        if (operator != null) {
            this.operatorId = operator.getId();
            this.operatorEmployeeCode = operator.getEmployeeCode();
            this.operatorName = operator.getFullName();
            this.fatigueScoreAtAssignment = operator.calculateBurnoutIndex();
            this.willingnessScore = operator.getWillingnessScore();
        } else {
            this.operatorName = "UNASSIGNED";
        }

        this.shiftType = shiftType;
        this.shiftDate = shiftDate;
        this.assignmentType = assignmentType;
        this.regularHours = regularHours;
        this.overtimeHours = overtimeHours;
        this.totalCost = totalCost;
        this.decisionReason = decisionReason;
        this.pinned = pinned;
        this.assignedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMachineCode() {
        return machineCode;
    }

    public void setMachineCode(String machineCode) {
        this.machineCode = machineCode;
    }

    public String getMachineName() {
        return machineName;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    public int getMachinePriority() {
        return machinePriority;
    }

    public void setMachinePriority(int machinePriority) {
        this.machinePriority = machinePriority;
    }

    public ProductionCell getCell() {
        return cell;
    }

    public void setCell(ProductionCell cell) {
        this.cell = cell;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public String getOperatorEmployeeCode() {
        return operatorEmployeeCode;
    }

    public void setOperatorEmployeeCode(String operatorEmployeeCode) {
        this.operatorEmployeeCode = operatorEmployeeCode;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public ShiftType getShiftType() {
        return shiftType;
    }

    public void setShiftType(ShiftType shiftType) {
        this.shiftType = shiftType;
    }

    public LocalDate getShiftDate() {
        return shiftDate;
    }

    public void setShiftDate(LocalDate shiftDate) {
        this.shiftDate = shiftDate;
    }

    public AssignmentType getAssignmentType() {
        return assignmentType;
    }

    public void setAssignmentType(AssignmentType assignmentType) {
        this.assignmentType = assignmentType;
    }

    public double getRegularHours() {
        return regularHours;
    }

    public void setRegularHours(double regularHours) {
        this.regularHours = regularHours;
    }

    public double getOvertimeHours() {
        return overtimeHours;
    }

    public void setOvertimeHours(double overtimeHours) {
        this.overtimeHours = overtimeHours;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public double getFatigueScoreAtAssignment() {
        return fatigueScoreAtAssignment;
    }

    public void setFatigueScoreAtAssignment(double fatigueScoreAtAssignment) {
        this.fatigueScoreAtAssignment = fatigueScoreAtAssignment;
    }

    public double getWillingnessScore() {
        return willingnessScore;
    }

    public void setWillingnessScore(double willingnessScore) {
        this.willingnessScore = willingnessScore;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }
}
