package de.lifecircles.model;

import java.io.Serializable;

/**
 * Represents a cell or sensor/actor type using RGB values.
 * Each type is defined by its red, green, and blue components.
 */
public class CellType implements Serializable {
    private static final long serialVersionUID = 1L;

    private final double red;
    private final double green;
    private final double blue;

    public CellType(double red, double green, double blue) {
        this.red = clamp(red);
        this.green = clamp(green);
        this.blue = clamp(blue);
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    public double getRed() {
        return red;
    }

    public double getGreen() {
        return green;
    }

    public double getBlue() {
        return blue;
    }

    /**
     * Calculates the similarity between two types.
     * @param other The other type to compare with
     * @return A value between 0 (completely different) and 1 (identical)
     */
    public double similarity(CellType other) {
        double dr = red - other.red;
        double dg = green - other.green;
        double db = blue - other.blue;
        return 1.0 - Math.sqrt((dr * dr + dg * dg + db * db) / 3.0);
    }

    /**
     * Creates a mixed type from multiple types.
     * @param types Array of types to mix
     * @return A new type representing the average of all input types
     */
    public static CellType mix(CellType... types) {
        if (types.length == 0) {
            return new CellType(0, 0, 0);
        }

        double r = 0, g = 0, b = 0;
        for (CellType type : types) {
            r += type.red;
            g += type.green;
            b += type.blue;
        }
        return new CellType(r / types.length, g / types.length, b / types.length);
    }

    /**
     * Override equals to compare by RGB values.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CellType)) return false;
        CellType other = (CellType) obj;
        return Double.compare(red, other.red) == 0
            && Double.compare(green, other.green) == 0
            && Double.compare(blue, other.blue) == 0;
    }

    @Override
    public int hashCode() {
        int result = 17;
        long rBits = Double.doubleToLongBits(red);
        long gBits = Double.doubleToLongBits(green);
        long bBits = Double.doubleToLongBits(blue);
        result = 31 * result + (int)(rBits ^ (rBits >>> 32));
        result = 31 * result + (int)(gBits ^ (gBits >>> 32));
        result = 31 * result + (int)(bBits ^ (bBits >>> 32));
        return result;
    }
}
