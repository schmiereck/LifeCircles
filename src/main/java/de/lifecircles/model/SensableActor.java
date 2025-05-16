package de.lifecircles.model;

/**
 * Interface für Objekte, die von einem Sensor wahrgenommen werden können.
 * Implementiert von Cell, SensorActor und Blocker.
 */
public interface SensableActor {
    
    /**
     * Gibt den Typ des wahrnehmbaren Objekts zurück.
     * @return Der CellType des Objekts
     */
    CellType getType();

    /**
     * Gibt die Kraftstärke zurück.
     * @return Die Kraftstärke oder 0.0 wenn nicht anwendbar
     */
    default double getForceStrength() {
        return 0.0;
    }
}
