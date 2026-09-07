package com.zenalyst.factory.domain.model;

/**
 * Skill tiers required to safely and effectively operate factory equipment.
 * Some machines have life-safety and tight-tolerance requirements that
 * mandate certified training.
 */
public enum CertificationLevel {
    NONE(0, "General / No formal certificate required"),
    LEVEL_1_BASIC(1, "Basic operation and routine monitoring"),
    LEVEL_2_STANDARD(2, "Standard autonomous setup, tooling adjustments, and operation"),
    LEVEL_3_CRITICAL(3, "Master/Precision operator, high-risk safety hazards and tight tolerances");

    private final int rank;
    private final String description;

    CertificationLevel(int rank, String description) {
        this.rank = rank;
        this.description = description;
    }

    public int getRank() {
        return rank;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Checks if this certification level meets or exceeds the required level.
     */
    public boolean satisfies(CertificationLevel required) {
        if (required == null || required == NONE) {
            return true;
        }
        return this.rank >= required.rank;
    }
}
