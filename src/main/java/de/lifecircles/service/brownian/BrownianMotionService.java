package de.lifecircles.service.brownian;

import de.lifecircles.model.Cell;
import de.lifecircles.service.SimulationConfig;
import java.util.List;
import java.util.Random;

/** Applies the configured model to all cells of a step, from its own random stream (other draws stay as they were). */
public final class BrownianMotionService {
    private final Random random;

    public BrownianMotionService(long seed) { random = new Random(seed); }

    public void agitate(List<Cell> cells, double dt) {
        SimulationConfig config = SimulationConfig.getInstance();
        BrownianMotion motion = config.getBrownianModel().motion();
        double density = config.getBrownianDensity(), strength = config.getBrownianStrength();
        if (motion == null || density <= 0 || strength <= 0) return;
        for (Cell cell : cells) motion.agitate(cell, dt, density, strength, random);
    }
}
