package de.lifecircles.model;

/**
 * Interface für Objekte, die von einem Sensor wahrgenommen werden können.
 * Implementiert von Cell, SensorActor und Blocker.
 */
public interface Sensable {
    
    /**
     * Gibt den Typ des wahrnehmbaren Objekts zurück.
     * @return Der CellType des Objekts
     */
    CellType getType();
    
    /**
     * Gibt die Energie des Objekts zurück.
     * @return Die Energie des Objekts oder 0.0 wenn nicht anwendbar
     */
    default double getEnergy() {
        return 0.0;
    }
    
    /**
     * Gibt das Alter des Objekts zurück.
     * @return Das Alter des Objekts oder 0.0 wenn nicht anwendbar
     */
    default double getAge() {
        return 0.0;
    }
    
    /**
     * Gibt die Kraftstärke zurück.
     * @return Die Kraftstärke oder 0.0 wenn nicht anwendbar
     */
    default double getForceStrength() {
        return 0.0;
    }
    
    /**
     * Gibt den normalisierten Zellzustand zurück.
     * @return Der normalisierte Zellzustand oder null wenn nicht anwendbar
     */
    default double[] getNormalizedCellState() {
        return new double[]{0.0, 0.0, 0.0};
    }
}
