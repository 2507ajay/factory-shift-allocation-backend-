package com.zenalyst.factory.domain.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A human operator in the factory workforce.
 * Incorporates technical certifications, overtime history, attendance state,
 * and psychological fatigue / willingness metrics.
 */
@Entity
@Table(name = "operators")
public class Operator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String employeeCode;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShiftType primaryShift;

    @Column(length = 10)
    private String defaultMachineCode;

    @Column(nullable = false)
    private double baseHourlyRate;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "operator_certifications",
            joinColumns = @JoinColumn(name = "operator_id"),
            inverseJoinColumns = @JoinColumn(name = "certification_id")
    )
    private Set<Certification> certifications = new HashSet<>();

    /**
     * Total overtime hours accumulated in the current work week.
     */
    @Column(nullable = false)
    private double weeklyOvertimeHoursWorked = 0.0;

    /**
     * Hard statutory/contractual weekly overtime cap (default 16.0 hours).
     */
    @Column(nullable = false)
    private double overtimeCapHours = 16.0;

    /**
     * Number of consecutive days worked without a 24-hour rest day.
     */
    @Column(nullable = false)
    private int consecutiveDaysWorked = 1;

    /**
     * Timestamp when their last shift completed, used to enforce mandatory rest intervals (e.g. 8h).
     */
    private LocalDateTime lastShiftEndTime;

    /**
     * Willingness / Co-operativeness score (0.0 to 1.0).
     * High willingness operators (0.9+) are the dedicated team players who rarely say no,
     * which historically leads supervisors to over-rely on them until they burn out.
     */
    @Column(nullable = false)
    private double willingnessScore = 0.5;

    /**
     * Number of emergency coverage shifts taken on short notice in the current cycle.
     */
    @Column(nullable = false)
    private int historicalCoverageCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AttendanceStatus attendanceStatus = AttendanceStatus.SCHEDULED;

    @Column(length = 255)
    private String notes;

    public Operator() {
    }

    public Operator(String employeeCode, String fullName, ShiftType primaryShift,
                    String defaultMachineCode, double baseHourlyRate,
                    double willingnessScore, double weeklyOvertimeHoursWorked,
                    int historicalCoverageCount) {
        this.employeeCode = employeeCode;
        this.fullName = fullName;
        this.primaryShift = primaryShift;
        this.defaultMachineCode = defaultMachineCode;
        this.baseHourlyRate = baseHourlyRate;
        this.willingnessScore = willingnessScore;
        this.weeklyOvertimeHoursWorked = weeklyOvertimeHoursWorked;
        this.historicalCoverageCount = historicalCoverageCount;
        this.attendanceStatus = AttendanceStatus.SCHEDULED;
    }

    /**
     * Checks if this operator holds an active, valid certification that satisfies the machine's requirement.
     */
    public boolean isCertifiedFor(Machine machine, LocalDate onDate) {
        if (!machine.requiresCertification()) {
            return true;
        }

        String reqSkill = machine.getRequiredSkillCode();
        CertificationLevel reqLevel = machine.getRequiredCertLevel();

        for (Certification cert : certifications) {
            if (cert.isValidOn(onDate)) {
                // If machine specifies a specific skill code, verify match or broad competence
                boolean skillMatches = (reqSkill == null || reqSkill.isBlank() ||
                        reqSkill.equalsIgnoreCase(cert.getSkillCode()));
                if (skillMatches && cert.getLevel().satisfies(reqLevel)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Evaluates if assigning the operator additional overtime hours stays within legal and safety caps.
     */
    public boolean canLegallyWorkOvertime(double additionalHours) {
        return (weeklyOvertimeHoursWorked + additionalHours) <= overtimeCapHours;
    }

    /**
     * Checks if the operator has received sufficient rest since their previous shift.
     */
    public boolean hasSufficientRest(LocalDateTime targetShiftStartTime, double minimumRestHours) {
        if (lastShiftEndTime == null) {
            return true;
        }
        long minutesRest = ChronoUnit.MINUTES.between(lastShiftEndTime, targetShiftStartTime);
        return minutesRest >= (minimumRestHours * 60);
    }

    /**
     * Computes the dynamic Burnout / Resentment Index (0.0 to 1.0+).
     * High values indicate the operator is near their breaking point.
     * Core formula directly solves the "same 4 willing people quietly furious" problem:
     * - Overtime utilization (50% weight)
     * - Willingness-exploitation ratio (30% weight)
     * - Consecutive days fatigue (20% weight)
     */
    public double calculateBurnoutIndex() {
        double otRatio = Math.min(1.0, weeklyOvertimeHoursWorked / Math.max(1.0, overtimeCapHours));
        double consecutiveFactor = Math.min(1.0, consecutiveDaysWorked / 6.0);

        // Willingness abuse factor: Willing people who are repeatedly called in feel disproportionate resentment
        double abuseFactor = (willingnessScore > 0.8)
                ? Math.min(1.0, (historicalCoverageCount * 0.25) + (otRatio * 0.4))
                : (historicalCoverageCount * 0.1);

        double index = (otRatio * 0.50) + (abuseFactor * 0.30) + (consecutiveFactor * 0.20);
        return Math.round(index * 100.0) / 100.0;
    }

    public boolean isQuietlyFurious() {
        return willingnessScore >= 0.85 && calculateBurnoutIndex() >= 0.65;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public ShiftType getPrimaryShift() {
        return primaryShift;
    }

    public void setPrimaryShift(ShiftType primaryShift) {
        this.primaryShift = primaryShift;
    }

    public String getDefaultMachineCode() {
        return defaultMachineCode;
    }

    public void setDefaultMachineCode(String defaultMachineCode) {
        this.defaultMachineCode = defaultMachineCode;
    }

    public double getBaseHourlyRate() {
        return baseHourlyRate;
    }

    public void setBaseHourlyRate(double baseHourlyRate) {
        this.baseHourlyRate = baseHourlyRate;
    }

    public Set<Certification> getCertifications() {
        return certifications;
    }

    public void setCertifications(Set<Certification> certifications) {
        this.certifications = certifications;
    }

    public void addCertification(Certification cert) {
        this.certifications.add(cert);
    }

    public double getWeeklyOvertimeHoursWorked() {
        return weeklyOvertimeHoursWorked;
    }

    public void setWeeklyOvertimeHoursWorked(double weeklyOvertimeHoursWorked) {
        this.weeklyOvertimeHoursWorked = weeklyOvertimeHoursWorked;
    }

    public double getOvertimeCapHours() {
        return overtimeCapHours;
    }

    public void setOvertimeCapHours(double overtimeCapHours) {
        this.overtimeCapHours = overtimeCapHours;
    }

    public int getConsecutiveDaysWorked() {
        return consecutiveDaysWorked;
    }

    public void setConsecutiveDaysWorked(int consecutiveDaysWorked) {
        this.consecutiveDaysWorked = consecutiveDaysWorked;
    }

    public LocalDateTime getLastShiftEndTime() {
        return lastShiftEndTime;
    }

    public void setLastShiftEndTime(LocalDateTime lastShiftEndTime) {
        this.lastShiftEndTime = lastShiftEndTime;
    }

    public double getWillingnessScore() {
        return willingnessScore;
    }

    public void setWillingnessScore(double willingnessScore) {
        this.willingnessScore = willingnessScore;
    }

    public int getHistoricalCoverageCount() {
        return historicalCoverageCount;
    }

    public void setHistoricalCoverageCount(int historicalCoverageCount) {
        this.historicalCoverageCount = historicalCoverageCount;
    }

    public AttendanceStatus getAttendanceStatus() {
        return attendanceStatus;
    }

    public void setAttendanceStatus(AttendanceStatus attendanceStatus) {
        this.attendanceStatus = attendanceStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Operator operator = (Operator) o;
        return Objects.equals(employeeCode, operator.employeeCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(employeeCode);
    }

    @Override
    public String toString() {
        return fullName + " (" + employeeCode + ", Shift: " + primaryShift + ", Burnout: " + calculateBurnoutIndex() + ")";
    }
}
