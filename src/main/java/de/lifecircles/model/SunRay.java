package de.lifecircles.model;

/**
 * Represents a transient sun ray in the environment for visualization.
 */
public class SunRay {
    private final Vector2D start;
    private final Vector2D end;
    private final EnergyType energyType;

    public SunRay(Vector2D start, Vector2D end) {
        this(start, end, EnergyType.PRIMARY);
    }

    public SunRay(Vector2D start, Vector2D end, EnergyType energyType) {
        this.start = start;
        this.end = end;
        this.energyType = energyType;
    }

    public EnergyType getEnergyType() {
        return energyType;
    }

    public Vector2D getStart() {
        return start;
    }

    public Vector2D getEnd() {
        return end;
    }
}
