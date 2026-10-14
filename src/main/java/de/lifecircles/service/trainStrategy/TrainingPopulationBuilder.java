package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.CellBrainService;
import de.lifecircles.service.*;
import java.util.*;

/** Plans a complete generation before changing any retained winner. */
final class TrainingPopulationBuilder {
    static final int MAX_PLACEMENT_ATTEMPTS = 64;
    private static final System.Logger LOG = System.getLogger(TrainingPopulationBuilder.class.getName());

    static Optional<List<Cell>> build(Environment world, List<Cell> winners, int targetCount,
                                     boolean sections, Random random) {
        if (winners.isEmpty()) {
            LOG.log(System.Logger.Level.WARNING, "Training generation skipped: no winners available");
            return Optional.empty();
        }
        SimulationConfig config = SimulationConfig.getInstance();
        int eliteCount = Math.min(targetCount, winners.size());
        List<Cell> parents = new ArrayList<>(targetCount);
        List<Vector2D> positions = new ArrayList<>(targetCount);
        for (int slot = 0; slot < targetCount; slot++) {
            Cell parent = slot < eliteCount ? winners.get(slot) : winners.get(random.nextInt(winners.size()));
            double radius = sections ? config.getCellMaxRadiusSize()
                    : slot < eliteCount ? parent.getRadiusSize() : config.getCellMinGrowRadiusSize();
            double sectionWidth = world.getWidth() / targetCount;
            Vector2D preferred;
            if (sections) {
                preferred = new Vector2D(sectionWidth * (slot + 0.5),
                        world.getHeight() - Environment.GroundBlockerHeight - config.getCellMaxRadiusSize());
            } else if (slot < eliteCount) {
                preferred = parent.getPosition();
            } else {
                double angle = random.nextDouble() * 2 * Math.PI;
                double offset = parent.getRadiusSize() + 2 * config.getCellMinGrowRadiusSize();
                preferred = new Vector2D(parent.getPosition().getX() + Math.cos(angle) * offset,
                        parent.getPosition().getY() + Math.sin(angle) * offset);
            }
            Vector2D chosen = null;
            for (int attempt = 0; attempt < MAX_PLACEMENT_ATTEMPTS; attempt++) {
                Vector2D candidate = attempt == 0 ? preferred : new Vector2D(
                        sections ? sectionWidth * (slot + random.nextDouble()) : random.nextDouble() * world.getWidth(),
                        random.nextDouble() * world.getHeight());
                candidate = new Vector2D(((candidate.getX() % world.getWidth()) + world.getWidth()) % world.getWidth(),
                        ((candidate.getY() % world.getHeight()) + world.getHeight()) % world.getHeight());
                if (ReproductionManagerService.isFreePosition(world, candidate, radius)) {
                    chosen = candidate;
                    break;
                }
            }
            if (chosen == null) {
                LOG.log(System.Logger.Level.WARNING, "Training generation skipped: slot {0} could not be placed in {1} attempts", slot, MAX_PLACEMENT_ATTEMPTS);
                return Optional.empty();
            }
            parents.add(parent);
            positions.add(chosen);
        }

        List<Cell> generation = new ArrayList<>(targetCount);
        generation.addAll(winners.subList(0, eliteCount));
        // No repeated biological reproduction attempts, and no energy taken from winners.
        for (int slot = eliteCount; slot < targetCount; slot++) {
            Cell parent = parents.get(slot);
            Cell child = ReproductionManagerService.createTrainingOffspring(config, world, parent,
                    positions.get(slot), sections ? 0 : parent.getCellState());
            if (child == null) return Optional.empty();
            generation.add(child);
        }
        for (int slot = 0; slot < targetCount; slot++) {
            Cell cell = generation.get(slot);
            cell.setPosition(positions.get(slot));
            cell.setEnergy(SimulationConfig.CELL_MAX_ENERGY);
            if (sections) {
                cell.setCellState(0);
                cell.setAge(0);
                ReproductionManagerService.calcActiveLayersByState(cell);
            }
            for (SensorActor actor : cell.getSensorActors()) actor.updateCachedPosition();
            if (sections && slot < eliteCount) CellBrainService.think(cell);
        }
        return Optional.of(generation);
    }
}
