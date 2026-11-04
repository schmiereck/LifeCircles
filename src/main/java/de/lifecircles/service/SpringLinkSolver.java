package de.lifecircles.service;

import de.lifecircles.model.Cell;
import de.lifecircles.model.Vector2D;

import java.util.List;

/**
 * Experimental XPBD solver for spring pairs (FEDERFELD.md „XPBD“).
 * <p>
 * The force phase records every pulling cell pair as one hinge instead of applying the spring forces of the
 * pulling actors: a point on each rim in the direction of the stiffness-weighted pulling actors, satisfied when the
 * cells touch there. After all cells moved, Gauss-Seidel sweeps project the hinge points together with compliance
 * 1/k, moving and turning both cells by the inverse masses of the explicit integrator (Δv = 0.08·60·F·dt,
 * Δω = 60·τ/(20·r²)·dt). The pull of a hinge is capped at the summed tear force of its springs, so it slips instead
 * of holding without limit. Velocities follow the position corrections; momentum is conserved.
 */
public final class SpringLinkSolver {
    /**
     * A pulling cell pair as one hinge: point (ax, ay) on the rim of {@code a} and (bx, by) on the rim of {@code b},
     * both in the unrotated cell frame (direction of the stiffness-weighted pulling actors), with summed stiffness.
     */
    public record Link(Cell a, double ax, double ay, Cell b, double bx, double by, double stiffness, double maxForce) {}

    /** Inverse mass of the explicit integrator: Δx = W_LINEAR·F·dt². */
    static final double W_LINEAR = Cell.FORCE_TO_VELOCITY * SimulationConfig.initialCalcFps;

    private SpringLinkSolver() {}

    static double angularWeight(Cell cell) {
        return SimulationConfig.initialCalcFps / SimulationConfig.CELL_ANGULAR_VELOCITY_DIFF
                / (cell.getRadiusSize() * cell.getRadiusSize());
    }

    public static void solve(List<Link> links, double width, double height, double dt,
                             double stiffnessFactor, int iterations) {
        if (links == null || links.isEmpty() || dt <= 0) return;
        final int count = links.size();
        // Per link: centre offset a→b at the start, compliance/dt², accumulated λ (F·dt² units).
        final double[] offX = new double[count], offY = new double[count], alpha = new double[count], lambda = new double[count];
        final java.util.IdentityHashMap<Cell, Integer> slot = new java.util.IdentityHashMap<>();
        final int[] ia = new int[count], ib = new int[count];
        for (int i = 0; i < count; i++) {
            Link link = links.get(i);
            ia[i] = slot.computeIfAbsent(link.a(), c -> slot.size());
            ib[i] = slot.computeIfAbsent(link.b(), c -> slot.size());
            Vector2D offset = link.a().getPosition().getShortestDirectionWithWrapAround(link.b().getPosition(), width, height);
            offX[i] = offset.getX();
            offY[i] = offset.getY();
            double k = link.stiffness() * stiffnessFactor;
            alpha[i] = Double.isInfinite(k) ? 0 : 1 / (k * dt * dt);
        }
        final Cell[] cells = new Cell[slot.size()];
        slot.forEach((cell, index) -> cells[index] = cell);
        final double[] dx = new double[cells.length], dy = new double[cells.length], dRot = new double[cells.length];
        final double[] rot = new double[cells.length], wAng = new double[cells.length];
        for (int c = 0; c < cells.length; c++) {
            rot[c] = cells[c].getRotation();
            wAng[c] = angularWeight(cells[c]);
        }
        for (int iteration = 0; iteration < iterations; iteration++) {
            for (int i = 0; i < count; i++) {
                final Link link = links.get(i);
                final int a = ia[i], b = ib[i];
                final double cosA = Math.cos(rot[a] + dRot[a]), sinA = Math.sin(rot[a] + dRot[a]);
                final double cosB = Math.cos(rot[b] + dRot[b]), sinB = Math.sin(rot[b] + dRot[b]);
                final double rax = link.ax() * cosA - link.ay() * sinA, ray = link.ax() * sinA + link.ay() * cosA;
                final double rbx = link.bx() * cosB - link.by() * sinB, rby = link.bx() * sinB + link.by() * cosB;
                final double cx = offX[i] + dx[b] - dx[a] + rbx - rax, cy = offY[i] + dy[b] - dy[a] + rby - ray;
                final double d = Math.sqrt(cx * cx + cy * cy);
                if (d <= 1e-12) continue;
                final double nx = cx / d, ny = cy / d;
                final double crossA = rax * ny - ray * nx, crossB = rbx * ny - rby * nx;
                final double w = 2 * W_LINEAR + wAng[a] * crossA * crossA + wAng[b] * crossB * crossB;
                double next = lambda[i] + (-d - alpha[i] * lambda[i]) / (w + alpha[i]);
                // Pull only, and never more than the tear force.
                next = Math.min(0, Math.max(next, -link.maxForce() * dt * dt));
                final double step = next - lambda[i];
                lambda[i] = next;
                if (step == 0) continue;
                dx[b] += W_LINEAR * step * nx; dy[b] += W_LINEAR * step * ny; dRot[b] += wAng[b] * crossB * step;
                dx[a] -= W_LINEAR * step * nx; dy[a] -= W_LINEAR * step * ny; dRot[a] -= wAng[a] * crossA * step;
            }
        }
        for (int c = 0; c < cells.length; c++) {
            if (dx[c] == 0 && dy[c] == 0 && dRot[c] == 0) continue;
            Cell cell = cells[c];
            cell.setPosition(cell.getPosition().add(new Vector2D(dx[c], dy[c])));
            cell.setVelocity(cell.getVelocity().add(new Vector2D(dx[c] / dt, dy[c] / dt)));
            double rotation = (rot[c] + dRot[c]) % (2 * Math.PI);
            if (rotation < 0) rotation += 2 * Math.PI;
            if (rotation >= 2 * Math.PI) rotation = 0;
            cell.setRotation(rotation);
            cell.setAngularVelocity(cell.getAngularVelocity() + dRot[c] / dt);
        }
    }
}
