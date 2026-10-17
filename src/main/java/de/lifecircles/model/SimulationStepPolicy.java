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
    /** Open NONE worlds: remains plus the switchable durable adhesion (ZELLVERBINDUNGEN.md). */
    SimulationStepPolicy OPEN_WORLD = new SimulationStepPolicy() {
        @Override public boolean keepRemains() { return true; }
        @Override public boolean useCellAdhesion() { return SimulationConfig.getInstance().isCellAdhesion(); }
    };
    /** Training/evaluation episodes retain their former membership semantics unless opted in. */
    default boolean keepRemains() { return false; }
    default boolean useSunlight() { return true; }
    default boolean useEnergyConversion() { return true; }
    /** Innate on-demand 000 -> 001 brain respiration; follows the metabolism switch by default. */
    default boolean useBasalBrainRespiration() { return useEnergyConversion(); }
    /** Force tonus across pause/death and colour/type affinity; off for legacy and training worlds. */
    default boolean useCellAdhesion() { return false; }
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
