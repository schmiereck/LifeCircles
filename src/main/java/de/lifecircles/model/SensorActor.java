package de.lifecircles.model;

import de.lifecircles.service.SimulationConfig;

import java.io.*;

/**
 * Represents a sensor/actor point on a cell's surface.
 * Can both sense nearby actors and emit force fields.
 */
public class SensorActor implements SensableActor, Serializable {
    private static final long serialVersionUID = 1L;

    private transient Cell parentCell; // Nicht serialisierbar
    private final double angleOnCell; // Angle position on the cell surface (in radians)
    private final double cosAngleOnCell;
    private final double sinAngleOnCell;
    private CellType type;
    private double forceStrength; // Positive for attraction, negative for repulsion
    // temporarily stores the sensed actor and its cell
    private transient SensableActor sensedActor; // Nicht serialisierbar
    private transient SensableCell sensedCell;  // Nicht serialisierbar
    // cached position for current simulation step
    private transient Vector2D cachedPosition;
    private double reproductionDesire;
    private double energyAbsorption;
    private double energyDelivery;
    private boolean touchingBlocker = false;
    /**
     * Gibt den Energieanteil-Output des Actors bei einer Teilung an.
     * Energieanteil (zwischen 0.05 und 0.5 -> 5% bis 50% der Energie).
     */
    private double reproductionEnergyShareOutput;
    private int reproductionState;

    public SensorActor(Cell parentCell, double angleOnCell) {
        this.parentCell = parentCell;
        this.angleOnCell = angleOnCell;
        this.cosAngleOnCell = Math.cos(angleOnCell);
        this.sinAngleOnCell = Math.sin(angleOnCell);
        this.type = new CellType(0, 0, 0);
        this.forceStrength = 0;
        this.sensedActor = null;
        this.sensedCell = null;
        this.cachedPosition = null;
    }

    //public Vector2D getPosition() {
    //    // Calculate position relative to cell center
    //    Vector2D offset = new Vector2D(
    //        Math.cos(angleOnCell) * parentCell.getSize() / 2,
    //        Math.sin(angleOnCell) * parentCell.getSize() / 2
    //    );
    //    // Rotate by cell's rotation and add to cell's position
    //    return offset.rotate(parentCell.getRotation()).add(parentCell.getPosition());
    //}
    public Vector2D calcPosition() {
        // Inline optimized: use precomputed angle unit vector and inline rotation
        double halfSize = parentCell.getRadiusSize();
        double rotation = parentCell.getRotation();
        double cosR = Math.cos(rotation);
        double sinR = Math.sin(rotation);
        Vector2D cellPos = parentCell.getPosition();
        double x0 = cosAngleOnCell * halfSize;
        double y0 = sinAngleOnCell * halfSize;
        double x = x0 * cosR - y0 * sinR + cellPos.getX();
        double y = x0 * sinR + y0 * cosR + cellPos.getY();
        return new Vector2D(x, y);
    }

    public CellType getType() {
        return type;
    }

    public void setType(CellType type) {
        this.type = type;
    }

    public double getForceStrength() {
        return this.forceStrength;
    }

    public void setForceStrength(double forceStrength) {
        if (forceStrength < 0.0D) {
            this.forceStrength = Math.max(-SimulationConfig.getInstance().getCellActorMaxAttractiveForceStrength(), forceStrength);
        } else {
            this.forceStrength = Math.min(SimulationConfig.getInstance().getCellActorMaxRepulsiveForceStrength(), forceStrength);
        }
    }

    /** Exposes parent cell for dynamic sensor field radius */
    public Cell getParentCell() {
        return parentCell;
    }

    /** Temporarily stores the sensed actor */
    public SensableActor getSensedActor() { return sensedActor; }
    public void setSensedActor(SensableActor sensedActor) { this.sensedActor = sensedActor; }
    /** References the cell of the sensed actor */
    public SensableCell getSensedCell() { return sensedCell; }
    public void setSensedCell(SensableCell sensedCell) { this.sensedCell = sensedCell; }

    /**
     * Computes and stores the current position of this sensor actor.
     */
    public void updateCachedPosition() {
        this.cachedPosition = this.calcPosition();
    }

    /**
     * Returns the cached position computed for this simulation step.
     */
    public Vector2D getCachedPosition() {
        return this.cachedPosition;
    }

    public double getReproductionDesire() {
        return this.reproductionDesire;
    }

    public void setReproductionDesire(double reproductionDesire) {
        this.reproductionDesire = Math.max(0.0, Math.min(1.0, reproductionDesire));
    }

    public double getEnergyAbsorption() {
        return this.energyAbsorption;
    }

    public void setEnergyAbsorption(double energyAbsorption) {
        this.energyAbsorption = Math.max(0.0, Math.min(1.0, energyAbsorption));
    }

    public double getEnergyDelivery() {
        return this.energyDelivery;
    }

    public void setEnergyDelivery(double energyDelivery) {
        this.energyDelivery = Math.max(0.0, Math.min(1.0, energyDelivery));
    }

    /**
     * Prüft, ob der Sensor einen Blocker berührt
     */
    public boolean isTouchingBlocker() {
        return touchingBlocker;
    }

    /**
     * Setzt den Blocker-Berührungsstatus und aktualisiert die Typen entsprechend
     */
    public void setTouchingBlocker(boolean touchingBlocker) {
        this.touchingBlocker = touchingBlocker;

        if (touchingBlocker) {
            // Setze den Aktor-Typ auf grau (0.5, 0.5, 0.5) wenn ein Blocker berührt wird
            this.setType(new CellType(0.5, 0.5, 0.5));

            // Setze auch den Zell-Typ auf grau
            if (this.parentCell != null) {
                this.parentCell.setType(new CellType(0.5, 0.5, 0.5));
            }

            // Andere Sensor-Inputs zurücksetzen
            this.sensedActor = null;
            this.sensedCell = null;
        }
    }

    @Serial
    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        // Speichere die ID des parentCell, falls nötig
        // oos.writeObject(parentCell != null ? parentCell.getId() : null);
    }

    @Serial
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        this.sensedActor = null;
        this.sensedCell = null;
        this.cachedPosition = null;
        // parentCell wird nach der Deserialisierung von der Cell-Klasse gesetzt
    }

    public void setParentCell(Cell parentCell) {
        this.parentCell = parentCell;
    }

    public void setReproductionEnergyShareOutput(double reproductionEnergyShareOutput) {
        this.reproductionEnergyShareOutput = Math.max(0.1D, Math.min(0.5D, reproductionEnergyShareOutput));
    }

    public double getReproductionEnergyShareOutput() {
        return this.reproductionEnergyShareOutput;
    }

    public void setReproductionState(final int reproductionState) {
        this.reproductionState = reproductionState;
    }

    public int getReproductionState() {
        return this.reproductionState;
    }

}
