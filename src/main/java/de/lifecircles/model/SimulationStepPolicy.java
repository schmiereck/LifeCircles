package de.lifecircles.model;

import de.lifecircles.service.ReproductionManagerService;
import de.lifecircles.service.SimulationConfig;
import de.lifecircles.service.BrainEnergyCosts;

/** Optional episode constraints. afterThinking runs concurrently on separate cells. */
public interface SimulationStepPolicy {
    SimulationStepPolicy NONE = new SimulationStepPolicy() {
        @Override public boolean keepRemains() { return true; }
    };
    SimulationStepPolicy DISCARD_REMAINS = new SimulationStepPolicy() {};
    /** Training/evaluation episodes retain their former membership semantics unless opted in. */
    default boolean keepRemains() { return false; }
    default boolean useSunlight() { return true; }
    default BrainEnergyCosts brainEnergyCosts() { return SimulationConfig.getInstance().getBrainEnergyCosts(); }
    default void afterThinking(Cell cell) {}
    default boolean isDead(Cell cell) {
        return cell.getAge() >= SimulationConfig.getInstance().getCellDeathAge() || cell.getEnergy() <= 0;
    }
    default boolean allowReproduction(Cell cell) { return true; }
    default Cell reproduce(Environment world, Cell parent) {
        return ReproductionManagerService.reproduce(SimulationConfig.getInstance(), world, parent);
    }
    /** Sequential, immediately after creating each child and before the next parent. */
    default void onBirth(Cell parent, Cell child) {}
}
