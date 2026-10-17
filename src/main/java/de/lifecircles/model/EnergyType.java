package de.lifecircles.model;

import java.util.List;

/** Independent energy stores; no conversion or function-specific metabolism is implied. */
public enum EnergyType {
    PRIMARY(0, "000 · Maintenance"),
    SECONDARY(1, "001 · Brain"),
    RGB_010(2, "010"),
    RGB_011(3, "011"),
    RGB_100(4, "100"),
    RGB_101(5, "101"),
    RGB_110(6, "110"),
    RGB_111(7, "111");

    /** Immutable iteration without allocating a values() array in simulation loops. */
    public static final List<EnergyType> ALL = List.of(values());
    private final String displayName;
    private final int code;
    EnergyType(int code, String displayName) { this.code = code; this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
    public int getCode() { return code; }
    public String getRgbCode() { return displayName.substring(0, 3); }
    public double getRed() { return (code & 4) == 0 ? 0 : 1; }
    public double getGreen() { return (code & 2) == 0 ? 0 : 1; }
    public double getBlue() { return (code & 1) == 0 ? 0 : 1; }
    public static EnergyType fromRgb(double red, double green, double blue) {
        return ALL.get(bit(red) * 4 + bit(green) * 2 + bit(blue));
    }
    private static int bit(double value) {
        if (value != 0 && value != 1) throw new IllegalArgumentException("Energy RGB components must be exactly 0 or 1");
        return value == 0 ? 0 : 1;
    }
}
