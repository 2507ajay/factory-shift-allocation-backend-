package com.zenalyst.factory.domain.model;

/**
 * The manufacturing plant is organized into 4 primary production cells.
 */
public enum ProductionCell {
    PRECISION_MACHINING("Precision Machining (CNC & EDM)", 1),
    FABRICATION_FORMING("Fabrication & Sheet Metal Forming", 2),
    ASSEMBLY_WELDING("Automated Welding & Assembly", 3),
    FINISHING_PACKAGING("Finishing, Quality & Packaging", 4);

    private final String title;
    private final int defaultCriticality;

    ProductionCell(String title, int defaultCriticality) {
        this.title = title;
        this.defaultCriticality = defaultCriticality;
    }

    public String getTitle() {
        return title;
    }

    public int getDefaultCriticality() {
        return defaultCriticality;
    }
}
