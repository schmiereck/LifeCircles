package de.lifecircles.service;

import de.lifecircles.model.Cell;
import de.lifecircles.model.SensorActor;

import java.util.List;

/** Force tonus after thinking: commands are approached gradually, paused brains hold, remains fade. */
public final class CellAdhesionService {
    private CellAdhesionService() {}

    public static void settleForces(final List<Cell> cells, final double dt) {
        final SimulationConfig config = SimulationConfig.getInstance();
        final double tau = config.getActorForceTonusSeconds();
        final double alpha = tau == 0 ? 1 : 1 - Math.exp(-dt / tau);
        final double fade = Math.exp(-Math.log(2) * dt / config.getRemainsForceHalfLife());
        // Each worker writes only its own cell's actors.
        cells.parallelStream().forEach(cell -> {
            for (SensorActor actor : cell.getSensorActors()) {
                if (cell.isRemains()) actor.decayForce(fade);
                else actor.settleForce(alpha);
            }
        });
    }
}
