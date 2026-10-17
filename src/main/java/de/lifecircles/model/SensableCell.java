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
    default boolean isRemains() { return false; }

    /** Legacy objects expose only primary energy; actual cells override all typed accessors. */
    default double getEnergy(EnergyType energyType) {
        java.util.Objects.requireNonNull(energyType);
        return energyType == EnergyType.PRIMARY ? getEnergy() : 0;
    }

    default double getMaxEnergy(EnergyType energyType) {
        java.util.Objects.requireNonNull(energyType);
        return energyType == EnergyType.PRIMARY ? getMaxEnergy() : 0;
    }

    default void setEnergy(EnergyType energyType, double energy) {
        java.util.Objects.requireNonNull(energyType);
        if (energyType == EnergyType.PRIMARY) setEnergy(energy);
        else if (energy != 0) throw new UnsupportedOperationException("Object has no secondary energy store");
    }

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
