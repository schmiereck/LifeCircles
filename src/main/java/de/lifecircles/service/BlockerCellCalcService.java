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
            
            // getNearestPoint berechnet jetzt immer den korrekten nächsten Punkt,
            // auch wenn sich der Zellmittelpunkt im Blocker befindet
            Vector2D nearestPoint = blocker.getNearestPoint(cellPos);
            
            Vector2D deltaVec = cellPos.subtract(nearestPoint);
            double distance = deltaVec.length();
            double radius = cell.getRadiusSize();
            
            // Kollision erkannt: Zelle überlappt mit dem Blocker
            if (distance <= radius) {
                double penetration = radius - distance;
                
                // Verbesserte Berechnung der Abstoßungsrichtung
                Vector2D direction;
                if (distance > 0.001) {
                    direction = deltaVec.divide(distance);
                } else {
                    // Wenn Zellmittelpunkt im Blocker ist, berechne Richtung basierend auf kürzestem Weg zur Oberfläche
                    Vector2D nearestEdgePoint = blocker.getNearestPoint(cellPos);
                    direction = nearestEdgePoint.subtract(cellPos).normalize();
                }
                
                // Abstoßungskraft anwenden
                double strength = SimulationConfig.getInstance().getBlockerRepulsionStrength();
                Vector2D repulsion = direction.multiply(strength * penetration);
                cell.applyForce(repulsion, cellPos);
                
                // Zelle knapp außerhalb des Blockers positionieren
                Vector2D pushOut = nearestPoint.add(direction.multiply(radius));
                cell.setPosition(pushOut);
                
                // Geschwindigkeitsanpassung für bestimmte Blockertypen
                if (blocker.getBlockerType() == Blocker.BlockerType.GROUND
                        || blocker.getBlockerType() == Blocker.BlockerType.PLATFORM
                        || blocker.getBlockerType() == Blocker.BlockerType.WALL) {
                    Vector2D velocity = cell.getVelocity();
                    cell.setVelocity(new Vector2D(velocity.getX() * 0.7D, 0));
                }
            }
        }
    }
}
