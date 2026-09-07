package com.zenalyst.factory.domain.model;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * Represents one of the 18 factory machines that must be staffed before the production line starts.
 */
@Entity
@Table(name = "machines")
public class Machine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductionCell cell;

    @Column(length = 50)
    private String requiredSkillCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CertificationLevel requiredCertLevel;

    /**
     * Operational priority (1 = Critical Bottleneck, 4 = Buffer / End-of-line packaging).
     * If an unavoidable labor shortage occurs, Priority 1 machines must be staffed first,
     * while Priority 4 buffer machines can be paused temporarily.
     */
    @Column(nullable = false)
    private int priorityRank;

    /**
     * Estimated revenue generated per hour while operating.
     * Used for triage impact calculation.
     */
    @Column(nullable = false)
    private double revenueRatePerHour;

    @Column(nullable = false)
    private boolean active = true;

    public Machine() {
    }

    public Machine(String code, String name, ProductionCell cell, String requiredSkillCode,
                   CertificationLevel requiredCertLevel, int priorityRank, double revenueRatePerHour) {
        this.code = code;
        this.name = name;
        this.cell = cell;
        this.requiredSkillCode = requiredSkillCode;
        this.requiredCertLevel = requiredCertLevel;
        this.priorityRank = priorityRank;
        this.revenueRatePerHour = revenueRatePerHour;
        this.active = true;
    }

    public boolean requiresCertification() {
        return requiredCertLevel != null && requiredCertLevel != CertificationLevel.NONE;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ProductionCell getCell() {
        return cell;
    }

    public void setCell(ProductionCell cell) {
        this.cell = cell;
    }

    public String getRequiredSkillCode() {
        return requiredSkillCode;
    }

    public void setRequiredSkillCode(String requiredSkillCode) {
        this.requiredSkillCode = requiredSkillCode;
    }

    public CertificationLevel getRequiredCertLevel() {
        return requiredCertLevel;
    }

    public void setRequiredCertLevel(CertificationLevel requiredCertLevel) {
        this.requiredCertLevel = requiredCertLevel;
    }

    public int getPriorityRank() {
        return priorityRank;
    }

    public void setPriorityRank(int priorityRank) {
        this.priorityRank = priorityRank;
    }

    public double getRevenueRatePerHour() {
        return revenueRatePerHour;
    }

    public void setRevenueRatePerHour(double revenueRatePerHour) {
        this.revenueRatePerHour = revenueRatePerHour;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Machine machine = (Machine) o;
        return Objects.equals(code, machine.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return code + " - " + name + " (Pri: P" + priorityRank + ", Req: " + requiredCertLevel + ")";
    }
}
