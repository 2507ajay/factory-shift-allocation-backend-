package com.zenalyst.factory.domain.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A verified technical certification held by an operator.
 */
@Entity
@Table(name = "certifications")
public class Certification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String skillCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CertificationLevel level;

    @Column(nullable = false)
    private LocalDate validUntil;

    public Certification() {
    }

    public Certification(String skillCode, String name, CertificationLevel level, LocalDate validUntil) {
        this.skillCode = skillCode;
        this.name = name;
        this.level = level;
        this.validUntil = validUntil;
    }

    public boolean isValidOn(LocalDate date) {
        return validUntil != null && !validUntil.isBefore(date);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSkillCode() {
        return skillCode;
    }

    public void setSkillCode(String skillCode) {
        this.skillCode = skillCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CertificationLevel getLevel() {
        return level;
    }

    public void setLevel(CertificationLevel level) {
        this.level = level;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Certification that = (Certification) o;
        return Objects.equals(skillCode, that.skillCode) && level == that.level;
    }

    @Override
    public int hashCode() {
        return Objects.hash(skillCode, level);
    }

    @Override
    public String toString() {
        return skillCode + " (" + level + ", exp: " + validUntil + ")";
    }
}
