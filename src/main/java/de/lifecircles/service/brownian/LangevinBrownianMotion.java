package de.lifecircles.service.brownian;

import de.lifecircles.model.Cell;
import de.lifecircles.model.Vector2D;
import java.util.Random;

/**
 * The limit of many small hits: one Gaussian push per step with the same mean square velocity change as the
 * collisions (hit rate · dt · strength²), at a random rim point so that it also turns the cell.
 */
public final class LangevinBrownianMotion implements BrownianMotion {
    @Override
    public void agitate(Cell cell, double dt, double density, double strength, Random random) {
        double sigma = strength * Math.sqrt(BrownianMotion.hitRate(cell, density) * dt / 2); // per axis
        if (!(sigma > 0)) return;
        Vector2D dv = new Vector2D(random.nextGaussian() * sigma, random.nextGaussian() * sigma);
        BrownianKicks.hit(cell, dv, random.nextDouble() * 2 * Math.PI, dt);
    }
}
