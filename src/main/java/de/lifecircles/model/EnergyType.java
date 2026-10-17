package de.lifecircles.model;

import java.util.List;

/** Independent energy stores; no conversion or function-specific metabolism is implied. */
public enum EnergyType {
    PRIMARY("Primary"),
    SECONDARY("Secondary");

    /** Immutable iteration without allocating a values() array in simulation loops. */
    public static final List<EnergyType> ALL = List.of(values());
    private final String displayName;
    EnergyType(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
