package de.lifecircles.service;

import de.lifecircles.model.Cell;
import de.lifecircles.model.Blocker;
import de.lifecircles.model.SunRay;
import de.lifecircles.model.Vector2D;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

/**
 * Service handling generation of sun energy rays,
 * applying energy to cells, and returning rays for visualization.
 */
public class EnergySunCalcService {
    private double rayAccumulator = 0.0;
    private double timeInCycle = 0.0; // Tracks time within the day/night cycle
    private final Random random = new Random();

    public List<SunRay> calculateSunEnergy(
            final List<Cell> cells,
            final List<Blocker> blockers,
            final double width,
            final double height,
            final SimulationConfig config,
            final double deltaTime) {
        final List<SunRay> rays = new ArrayList<>();

        // Update time in the day/night cycle
        this.timeInCycle = (this.timeInCycle + deltaTime) % SimulationConfig.DAY_NIGHT_CYCLE_DURATION;

        // Calculate intensity based on sinusoidal function
        final double intensity = Math.sin((this.timeInCycle / SimulationConfig.DAY_NIGHT_CYCLE_DURATION) * Math.PI);

        // Accumulate ray count based on pixel spacing to maintain constant density
        // Adjust rays per second based on intensity
        final double raysPerSecond = intensity * (width / config.getSunRaySpacingPx()) * config.getSunRayRate();
        this.rayAccumulator += raysPerSecond * deltaTime;
        final int numRays = (int) this.rayAccumulator;
        this.rayAccumulator -= numRays;
        for (int rayNo = 0; rayNo < numRays; rayNo++) {
            final double rayX = random.nextDouble() * width;
            // Find nearest cell intersection
            double nearestCellHitY = Double.MAX_VALUE;
            Cell hitCell = null;
            for (Cell cell : cells) {
                double dx = Math.abs(cell.getPosition().getX() - rayX);
                double r = cell.getRadiusSize();
                if (dx <= r) {
                    double yIntersect = cell.getPosition().getY() - Math.sqrt(r * r - dx * dx);
                    if (yIntersect >= 0 && yIntersect < nearestCellHitY) {
                        nearestCellHitY = yIntersect;
                        hitCell = cell;
                    }
                }
            }
            // Find nearest blocker intersection
            double nearestBlockerHitY = Double.MAX_VALUE;
            for (Blocker blocker : blockers) {
                final double left = blocker.getPosition().getX() - blocker.getWidth() / 2;
                final double right = blocker.getPosition().getX() + blocker.getWidth() / 2;
                if (rayX >= left && rayX <= right) {
                    final double y = blocker.getPosition().getY() - blocker.getHeight() / 2;
                    if (y >= 0 && y < nearestBlockerHitY) {
                        nearestBlockerHitY = y;
                    }
                }
            }
            // Determine ray end and apply energy
            double endY;
            if (hitCell != null && nearestCellHitY <= nearestBlockerHitY) {
                endY = nearestCellHitY;
                hitCell.setEnergy(hitCell.getEnergy() + config.getEnergyPerRay());
                hitCell.notifySunRayHit();
            } else if (nearestBlockerHitY < Double.MAX_VALUE) {
                endY = nearestBlockerHitY;
            } else {
                endY = height;
            }
            rays.add(new SunRay(
                new Vector2D(rayX, 0),
                new Vector2D(rayX, endY)
            ));
        }
        return rays;
    }
}
