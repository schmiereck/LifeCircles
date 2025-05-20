package de.lifecircles.model;

public interface SensableCell {

    /**
     * Gibt den Typ des wahrnehmbaren Objekts zurück.
     * @return Der CellType des Objekts
     */
    CellType getType();

    /**
     * Gibt die Energie des Objekts zurück.
     * @return Die Energie des Objekts oder 0.0 wenn nicht anwendbar
     */
    double getEnergy();

    /**
     * Gibt das Alter des Objekts zurück.
     * @return Das Alter des Objekts oder 0.0 wenn nicht anwendbar
     */
    default double getAge() {
        return 0.0D;
    }

    /**
     * Gibt den normalisierten Zellzustand zurück.
     * @return Der normalisierte Zellzustand oder null wenn nicht anwendbar
     */
    default double[] getNormalizedCellState() {
        return new double[]{0.0D, 0.0D, 0.0D};
    }

    void setEnergy(double energy);

    double getMaxEnergy();
}
