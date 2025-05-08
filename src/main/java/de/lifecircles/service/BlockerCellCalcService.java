package de.lifecircles.service;

import de.lifecircles.model.Blocker;
import de.lifecircles.model.Cell;
import de.lifecircles.model.Vector2D;

import java.util.List;

/**
 * Service responsible for processing collisions between blockers and cells.
 */
public class BlockerCellCalcService {

    /**
     * Handles collisions between the given cell and the specified blockers.
     * Moves the cell to the nearest point on the blocker surface and adjusts velocity.
     *
     * @param cell     the cell to process
     * @param blockers the list of blockers to check
     * @param deltaTime the time elapsed since the last update
     */
    public static void handleBlockerCollisions(Cell cell, List<Blocker> blockers, double deltaTime) {
        for (Blocker blocker : blockers) {
            Vector2D cellPos = cell.getPosition();
            Vector2D nearestPoint = blocker.getNearestPoint(cellPos);
            Vector2D deltaVec = cellPos.subtract(nearestPoint);
            double distance = deltaVec.length();
            double radius = cell.getRadiusSize();
            if (distance < radius) {
                double penetration = radius - distance;
                Vector2D direction = distance > 0 ? deltaVec.divide(distance) : new Vector2D(0, -1);
                double strength = SimulationConfig.getInstance().getBlockerRepulsionStrength();
                Vector2D repulsion = direction.multiply(strength * penetration);
                cell.applyForce(repulsion, cellPos, deltaTime);
                // push cell just outside blocker
                Vector2D pushOut = nearestPoint.add(direction.multiply(radius));
                cell.setPosition(pushOut);
                if (blocker.getType() == Blocker.BlockerType.GROUND
                        || blocker.getType() == Blocker.BlockerType.PLATFORM
                        || blocker.getType() == Blocker.BlockerType.WALL) {
                    Vector2D velocity = cell.getVelocity();
                    cell.setVelocity(new Vector2D(velocity.getX() * 0.8, 0));
                }
            }
        }
    }
}
