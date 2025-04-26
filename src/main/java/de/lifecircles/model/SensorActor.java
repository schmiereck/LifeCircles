package de.lifecircles.model;

/**
 * Represents a sensor/actor point on a cell's surface.
 * Can both sense nearby actors and emit force fields.
 */
public class SensorActor {

    private final Cell parentCell;
    private final double angleOnCell; // Angle position on the cell surface (in radians)
    private CellType type;
    private double forceStrength; // Positive for attraction, negative for repulsion
    private boolean fireEnergyBeam; // trigger for energy beam

    public SensorActor(Cell parentCell, double angleOnCell) {
        this.parentCell = parentCell;
        this.angleOnCell = angleOnCell;
        this.type = new CellType(0, 0, 0);
        this.forceStrength = 0;
        this.fireEnergyBeam = false;
    }

    public Vector2D getPosition() {
        // Calculate position relative to cell center
        Vector2D offset = new Vector2D(
            Math.cos(angleOnCell) * parentCell.getSize() / 2,
            Math.sin(angleOnCell) * parentCell.getSize() / 2
        );
        // Rotate by cell's rotation and add to cell's position
        return offset.rotate(parentCell.getRotation()).add(parentCell.getPosition());
    }

    public double getAngleOnCell() {
        return angleOnCell;
    }

    public CellType getType() {
        return type;
    }

    public void setType(CellType type) {
        this.type = type;
    }

    public double getForceStrength() {
        return forceStrength;
    }

    public void setForceStrength(double forceStrength) {
        this.forceStrength = forceStrength;
    }

    /** Flag indicating whether to fire an energy beam */
    public boolean shouldFireEnergyBeam() { return fireEnergyBeam; }
    public void setFireEnergyBeam(boolean fireEnergyBeam) { this.fireEnergyBeam = fireEnergyBeam; }

    /** Exposes parent cell for dynamic sensor field radius */
    public Cell getParentCell() {
        return parentCell;
    }
}
