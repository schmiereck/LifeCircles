package de.lifecircles.model;

/**
 * Represents a sensor/actor point on a cell's surface.
 * Can both sense nearby actors and emit force fields.
 */
public class SensorActor {

    private final Cell parentCell;
    private final double angleOnCell; // Angle position on the cell surface (in radians)
    private final double cosAngleOnCell;
    private final double sinAngleOnCell;
    private CellType type;
    private double forceStrength; // Positive for attraction, negative for repulsion
    private boolean fireEnergyBeam; // trigger for energy beam
    // temporarily stores the sensed actor and its cell
    private SensorActor sensedActor;
    private Cell sensedCell;

    public SensorActor(Cell parentCell, double angleOnCell) {
        this.parentCell = parentCell;
        this.angleOnCell = angleOnCell;
        this.cosAngleOnCell = Math.cos(angleOnCell);
        this.sinAngleOnCell = Math.sin(angleOnCell);
        this.type = new CellType(0, 0, 0);
        this.forceStrength = 0;
        this.fireEnergyBeam = false;
        this.sensedActor = null;
        this.sensedCell = null;
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
    public Vector2D getPosition() {
        // Inline optimized: use precomputed angle unit vector and inline rotation
        double halfSize = parentCell.getSize() * 0.5;
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

    /** Temporarily stores the sensed actor */
    public SensorActor getSensedActor() { return sensedActor; }
    public void setSensedActor(SensorActor sensedActor) { this.sensedActor = sensedActor; }
    /** References the cell of the sensed actor */
    public Cell getSensedCell() { return sensedCell; }
    public void setSensedCell(Cell sensedCell) { this.sensedCell = sensedCell; }
}
