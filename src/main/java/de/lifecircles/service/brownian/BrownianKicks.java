package de.lifecircles.service.brownian;

import de.lifecircles.model.Cell;
import de.lifecircles.model.Vector2D;
import de.lifecircles.service.SimulationConfig;
import java.util.Random;

/** Shared physics: one hit as a force acting during this step on a point of the cell's rim. */
public final class BrownianKicks {
    private BrownianKicks() {}

    /**
     * Changes the cell's velocity by {@code dv} (vector, px/s) through a force at the rim point at {@code rimAngle}.
     * The cell's usual lever model turns the tangential part into rotation.
     */
    public static void hit(Cell cell, Vector2D dv, double rimAngle, double dt) {
        if (dt <= 0) return;
        // updateForce adds velocityForce * dt * 60, applyForce scales the force by FORCE_TO_VELOCITY.
        double scale = 1 / (Cell.FORCE_TO_VELOCITY * dt * SimulationConfig.initialCalcFps);
        Vector2D rim = new Vector2D(Math.cos(rimAngle), Math.sin(rimAngle)).multiply(cell.getRadiusSize());
        cell.applyForce(dv.multiply(scale), cell.getPosition().add(rim));
    }

    /**
     * A particle from outside hits the rim at a uniform angle and pushes inwards, its direction tilted from the
     * inward normal by up to ± 90°: the push is mostly inward, the tilt turns the cell.
     */
    public static void particleHit(Cell cell, double speed, double dt, Random random) {
        double rim = random.nextDouble() * 2 * Math.PI;
        double direction = rim + Math.PI + (random.nextDouble() - .5) * Math.PI;
        hit(cell, new Vector2D(Math.cos(direction), Math.sin(direction)).multiply(speed), rim, dt);
    }
}
