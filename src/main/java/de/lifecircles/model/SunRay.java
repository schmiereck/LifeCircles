package de.lifecircles.model;

/**
 * Represents a transient sun ray in the environment for visualization.
 */
public class SunRay {
    private final Vector2D start;
    private final Vector2D end;

    public SunRay(Vector2D start, Vector2D end) {
        this.start = start;
        this.end = end;
    }

    public Vector2D getStart() {
        return start;
    }

    public Vector2D getEnd() {
        return end;
    }
}
