package de.lifecircles.service.brownian;

import de.lifecircles.model.Cell;
import de.lifecircles.model.Environment;
import de.lifecircles.service.SimulationConfig;
import java.util.List;
import java.util.Random;

/** Applies the configured model to all cells of a step, from its own random stream (other draws stay as they were). */
public final class BrownianMotionService {
    private final Random random;

    public BrownianMotionService(long seed) { random = new Random(seed); }

    /** {@code worldHeight}: the cells' height above the ground surface sets the strength of the profile. */
    public void agitate(List<Cell> cells, double dt, double worldHeight) {
        SimulationConfig config = SimulationConfig.getInstance();
        BrownianMotion motion = config.getBrownianModel().motion();
        double density = config.getBrownianDensity(), strength = config.getBrownianStrength();
        if (motion == null || density <= 0 || strength <= 0) return;
        BrownianProfile profile = config.getBrownianProfile();
        double ground = worldHeight - Environment.GroundBlockerHeight, top = ground,
                cold = config.getBrownianColdShare(), knee = config.getBrownianKneeHeight(), share = config.getBrownianKneeShare();
        for (Cell cell : cells) {
            double local = strength * profile.factor(ground - cell.getPosition().getY(), top, cold, knee, share);
            if (local > 0) motion.agitate(cell, dt, density, local, random);
        }
    }
}
