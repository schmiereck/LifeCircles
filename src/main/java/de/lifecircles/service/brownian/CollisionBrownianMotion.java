package de.lifecircles.service.brownian;

import de.lifecircles.model.Cell;
import java.util.Random;

/** Single particle hits: a Poisson number per step, each changing the velocity by {@code strength}. */
public final class CollisionBrownianMotion implements BrownianMotion {
    @Override
    public void agitate(Cell cell, double dt, double density, double strength, Random random) {
        int hits = poisson(BrownianMotion.hitRate(cell, density) * dt, random);
        for (int i = 0; i < hits; i++) BrownianKicks.particleHit(cell, strength, dt, random);
    }

    /** Knuth's method; the expected count per step is small. */
    static int poisson(double mean, Random random) {
        if (mean <= 0) return 0;
        double limit = Math.exp(-mean), product = random.nextDouble();
        int count = 0;
        while (product > limit) { count++; product *= random.nextDouble(); }
        return count;
    }
}
