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
        for (Blocker blocker : blockers) {
            if (blocker == null) continue;
            Vector2D position = cell.getPosition();
            Vector2D corrected = blocker.getCellCenterOutside(position, cell.getRadiusSize());
            Vector2D correction = corrected.subtract(position);
            if (correction.lengthSquared() <= 1e-24) continue;
            Vector2D normal = correction.normalize();
            cell.setPosition(corrected);
            Vector2D velocity = cell.getVelocity();
            double inwardSpeed = velocity.getX() * normal.getX() + velocity.getY() * normal.getY();
            if (inwardSpeed < 0) cell.setVelocity(velocity.subtract(normal.multiply(inwardSpeed)));
        }
    }
}
