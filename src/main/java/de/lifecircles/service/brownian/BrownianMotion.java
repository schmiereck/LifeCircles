package de.lifecircles.service.brownian;

import de.lifecircles.model.Cell;
import java.util.Random;

/**
 * BROWNSCHE_BEWEGUNG.md: an unseen background of small particles that pushes and turns the cells, evenly in every
 * direction (no current). Implementations differ in how they model the hits; all of them take the same two settings
 * so they can be compared: density (hits per second on a cell of REFERENCE_RADIUS, scaling with the circumference)
 * and strength (velocity change of one hit in px/s).
 * <p>
 * Implementations only apply forces for the current step (through {@link BrownianKicks}); the step integrates them
 * like every other force, so damping, collisions and bonds act on the result as usual. No cell energy is involved.
 */
public interface BrownianMotion {
    /** Radius (px) of the cell that receives exactly {@code density} hits per second. */
    double REFERENCE_RADIUS = 10;

    void agitate(Cell cell, double dt, double density, double strength, Random random);

    /** Expected hits per second on this cell. */
    static double hitRate(Cell cell, double density) {
        return density * cell.getRadiusSize() / REFERENCE_RADIUS;
    }
}
