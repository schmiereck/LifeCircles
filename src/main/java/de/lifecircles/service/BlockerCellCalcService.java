package de.lifecircles.service;

import de.lifecircles.model.Blocker;
import de.lifecircles.model.Cell;
import de.lifecircles.model.Vector2D;

import java.util.List;

/**
 * Service responsible for processing collisions between blockers and cells.
 */
public class BlockerCellCalcService {

    // Verhindere die Instanziierung der Klasse
    private BlockerCellCalcService() {
        throw new UnsupportedOperationException("Utility class");
    }
    public static boolean checkCellIsInsideBlocker(final Vector2D cellPos, final List<Blocker> blockers) {
        if (cellPos == null || blockers == null) return false;
        boolean cellIsInsideBlocker = false;
        for (final Blocker blocker : blockers) {
            if (blocker != null && blocker.containsPoint(cellPos)) {
                cellIsInsideBlocker = true;
                break;
            }
        }
        return cellIsInsideBlocker;
    }

    /**
     * Handles collisions between the given cell and the specified blockers.
     * Moves the cell to the nearest point on the blocker surface and adjusts velocity.
     *
     * @param cell     the cell to process
     * @param blockers the list of blockers to check
     */
    public static void handleBlockerCollisions(final Cell cell, final List<Blocker> blockers) {
        if (cell == null || blockers == null) return;
        final Vector2D cellPos = cell.getPosition();
        if (cellPos == null) return;
        final double radius = cell.getRadiusSize();
        for (final Blocker blocker : blockers) {
            if (blocker == null) continue;
            // Ersetze die Verwendung von getNearestPoint durch getCellCenterOutside
            final Vector2D newCellPos = blocker.getCellCenterOutside(cellPos, radius);
            final Vector2D deltaVec = cellPos.subtract(newCellPos);
            final double distance = deltaVec.length();

            final Vector2D nearestPoint = blocker.getNearestPoint(cellPos);
            final Vector2D nearestDeltaVec = cellPos.subtract(nearestPoint);
            final double nearestDistance = nearestDeltaVec.length();

            // Debugging: Logge die relevanten Werte zur Überprüfung
            //System.out.println("----------------------------------");
            //System.out.println("Cell Position: " + cellPos);
            //System.out.println("New Cell Position: " + newCellPos);
            //System.out.println("Delta Vector: " + deltaVec);
            //System.out.println("Distance: " + distance);
            //System.out.println("Radius: " + radius);

            // Prüfen, ob sich der Mittelpunkt der Zelle innerhalb des Blockers befindet
            //if (blocker.containsPoint(cellPos)) {
            //    //System.out.println("Cell center is inside the blocker.");
            //    // Berechne die Richtung vom Zellmittelpunkt zur Blocker-Oberfläche
            //    Vector2D direction = nearestPoint.subtract(cellPos).normalize();
            //    // Verschiebe die Zelle aus dem Blocker heraus
            //    double epsilon = 0.1; // Ein kleiner Puffer
            //    Vector2D pushOut = nearestPoint.add(direction.multiply(radius + epsilon));
            //    cell.setPosition(pushOut);
            //    // Geschwindigkeit vollständig auf 0 setzen
            //    //cell.setVelocity(new Vector2D(0, 0));
            //    // Debugging: Logge die neue Position
            //    //System.out.println("Cell pushed out to: " + cell.getPosition());
            //    continue; // Überspringe die weitere Verarbeitung für diese Zelle
            //}

            // Kollision erkannt: Zelle überlappt mit dem Blocker
            //if (distance <= radius) {
            if (nearestDistance <= radius) {
                //System.out.println("Collision detected: Cell is overlapping with the blocker.");
                double penetration = radius - nearestDistance;

                // Richtung immer vom Blocker-Oberflächenpunkt zum Zellmittelpunkt
                Vector2D direction = newCellPos.subtract(cellPos).normalize();

                // Abstoßungskraft anwenden
                double strength = SimulationConfig.getInstance().getBlockerRepulsionStrength();
                Vector2D repulsion = direction.multiply(strength * penetration);
                cell.applyForce(repulsion, cellPos);

                // Debugging: Logge die aktuelle Position und Richtung
                //System.out.println("Before PushOut - Cell Position: " + cell.getPosition());
                //System.out.println("Before PushOut - Direction: " + direction);

                // Zelle knapp außerhalb des Blockers positionieren (unter Berücksichtigung des Zellradius)
                //double epsilon = 0.1; // Ein kleiner Puffer
                //Vector2D pushOut = newCellPos.add(direction.multiply(radius + epsilon));
                //cell.setPosition(pushOut);

                // Geschwindigkeit vollständig auf 0 setzen, um erneutes Eindringen zu verhindern
                //cell.setVelocity(new Vector2D(0, 0));

                // Debugging: Logge die neue Position nach dem Verschieben
                //System.out.println("After PushOut - Cell Position: " + cell.getPosition());
                //System.out.println("Adjusted Velocity: " + cell.getVelocity());
            } else {
                if (blocker.containsPoint(cellPos)) {
                    //double penetration = radius - nearestDistance;

                    // Richtung immer vom Blocker-Oberflächenpunkt zum Zellmittelpunkt
                    Vector2D direction = newCellPos.subtract(cellPos).normalize();

                    // Abstoßungskraft anwenden
                    double strength = SimulationConfig.getInstance().getBlockerRepulsionStrength();
                    Vector2D repulsion = direction.multiply(strength);
                    cell.applyForce(repulsion, cellPos);

                    // Zelle auf dem Rand des Blockers positionieren.
                    Vector2D pushOut = newCellPos.add(deltaVec.normalize().multiply(nearestDistance));
                    cell.setPosition(pushOut);
                } else {
                    //System.out.println("No collision detected: Cell is outside the blocker.");
                }
            }
        }
    }
}
