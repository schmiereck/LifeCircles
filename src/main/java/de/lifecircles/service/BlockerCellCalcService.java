package de.lifecircles.service;

import de.lifecircles.model.Blocker;
import de.lifecircles.model.Cell;
import de.lifecircles.model.Environment;
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
        handleBlockerCollisions(cell, blockers, null);
    }

    public static void handleBlockerCollisions(final Cell cell, final List<Blocker> blockers, final Environment world) {
        handleBlockerCollisions(cell, blockers, world, 0, 0, 0);
    }

    /**
     * As above; with friction or rolling > 0 the contact also resists sliding and rolling (BODENREIBUNG.md): Coulomb
     * friction at the contact point, bounded by the coefficient times the impulse that pressed the cell on, and
     * rolling resistance of the flattened contact patch, a torque bounded by rolling times that impulse times the
     * radius. The contact point moves with the cell and its spin, so friction also turns spin into rolling.
     * Like the normal projection, the contact acts within the step dt that just moved the cell: position and rotation
     * follow the changed speeds, so a held cell does not creep.
     */
    public static void handleBlockerCollisions(final Cell cell, final List<Blocker> blockers, final Environment world,
                                               final double friction, final double rolling, final double dt) {
        if (cell == null || blockers == null) return;
        for (Blocker blocker : blockers) {
            if (blocker == null) continue;
            Vector2D position = cell.getPosition();
            // Clearly apart on one axis (beyond radius plus rounding margin): the projection keeps the position.
            if (world != null && apart(blocker, position, cell.getRadiusSize(), world.getWidth(), world.getHeight())) continue;
            Vector2D corrected = world == null ? blocker.getCellCenterOutside(position, cell.getRadiusSize())
                    : blocker.getCellCenterOutside(position, cell.getRadiusSize(), world.getWidth(), world.getHeight());
            Vector2D correction = corrected.subtract(position);
            if (correction.lengthSquared() <= 1e-24) continue;
            Vector2D normal = correction.normalize();
            cell.setPosition(corrected);
            Vector2D velocity = cell.getVelocity();
            double inwardSpeed = velocity.getX() * normal.getX() + velocity.getY() * normal.getY();
            if (inwardSpeed < 0) {
                velocity = velocity.subtract(normal.multiply(inwardSpeed));
                cell.setVelocity(velocity);
                if (friction > 0 || rolling > 0)
                    applyFriction(cell, normal, -inwardSpeed * friction, -inwardSpeed * rolling * SPIN_SHARE, dt);
            }
        }
    }

    /** Ratio of the contact speed change by spin to that by translation for a force at the rim (see Cell.applyForce). */
    private static final double SPIN_SHARE = 1 / (Cell.FORCE_TO_VELOCITY * SimulationConfig.CELL_ANGULAR_VELOCITY_DIFF);

    /**
     * Contact impulses that stop the contact point (sliding, at most maxSlide of linear speed) and the rolling
     * (a pure torque, at most maxRoll of rim speed). A tangential force F at the contact point changes the linear speed
     * by FORCE_TO_VELOCITY*F and the spin by -F/(CELL_ANGULAR_VELOCITY_DIFF*r) (torque over r squared, as in
     * Cell.applyForce). Both bounds are solved together: a cell pulled at its centre holds while both suffice,
     * otherwise it rolls with resistance or slides.
     */
    static void applyFriction(final Cell cell, final Vector2D normal, final double maxSlide, final double maxRoll,
                              final double dt) {
        final Vector2D velocity = cell.getVelocity();
        final double tx = -normal.getY(), ty = normal.getX();
        final double r = cell.getRadiusSize();
        // Contact point at -r*normal: spin w moves it by w*(-ry, rx), along the tangent that is -r*w.
        final double along = velocity.getX() * tx + velocity.getY() * ty, rim = r * cell.getAngularVelocity();
        // slide: linear change along the tangent; roll: rim speed change by the patch torque. The contact point rests
        // when along + slide = rim', the rolling stops when rim' = rim - SPIN_SHARE*slide + roll = 0. Projected
        // Gauss-Seidel; each sweep shrinks the error by SPIN_SHARE/(1+SPIN_SHARE) < 0.4.
        double slide = 0, roll = 0;
        for (int sweep = 0; sweep < 40; sweep++) {
            final double lastSlide = slide, lastRoll = roll;
            slide = clamp((rim + roll - along) / (1 + SPIN_SHARE), maxSlide);
            roll = clamp(SPIN_SHARE * slide - rim, maxRoll);
            if (Math.abs(slide - lastSlide) + Math.abs(roll - lastRoll) < 1e-13) break;
        }
        final double spin = (roll - SPIN_SHARE * slide) / r;
        cell.setAngularVelocity(cell.getAngularVelocity() + spin);
        cell.setVelocity(new Vector2D(velocity.getX() + slide * tx, velocity.getY() + slide * ty));
        cell.setPosition(cell.getPosition().add(new Vector2D(slide * tx * dt, slide * ty * dt)));
        double rotation = (cell.getRotation() + spin * dt) % (2 * Math.PI);
        if (rotation < 0) rotation += 2 * Math.PI;
        cell.setRotation(rotation >= 2 * Math.PI ? 0 : rotation);
    }

    private static double clamp(final double value, final double bound) { return Math.max(-bound, Math.min(bound, value)); }

    /** Same local offset as Blocker.getCellCenterOutside; the gap on one axis bounds its distance from below. */
    private static boolean apart(Blocker blocker, Vector2D position, double radius, double worldWidth, double worldHeight) {
        Vector2D local = blocker.getPosition().getShortestDirectionWithWrapAround(position, worldWidth, worldHeight);
        double reach = radius + 1e-6;
        return blocker.getWidth() < worldWidth && Math.abs(local.getX()) - blocker.getWidth() / 2 > reach
                || blocker.getHeight() < worldHeight && Math.abs(local.getY()) - blocker.getHeight() / 2 > reach;
    }
}
