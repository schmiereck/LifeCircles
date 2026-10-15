package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;

import java.util.Comparator;
import java.util.Random;

/**
 * Manages cell reproduction and mutation.
 */
public class ReproductionManagerService {
    private static double typeMutationStrength = 0.1;
    private static final Random random = new Random();

    /**
     * Checks if a cell is ready to reproduce.
     */
    public static boolean canReproduce(SimulationConfig config, Cell cell) {
        return cell.getEnergy() >= config.getReproductionEnergyThreshold() &&
                cell.getAge() >= config.getReproductionAgeThreshold() &&
                cell.getMaxReproductionDesire() >= config.getReproductionDesireThreshold();
    }

    /**
     * Creates a child cell through reproduction.
     * The child inherits traits from the parentCell with mutations.
     */
    public static Cell reproduce(final SimulationConfig config, final Environment environment, final Cell parentCell) {
        SensorActor chosenActor = parentCell.getSensorActors().stream()
                .filter(actor -> actor.getReproductionDesire() >= config.getReproductionDesireThreshold())
                .max(Comparator.comparingDouble(SensorActor::getReproductionDesire)).orElse(null);
        if (chosenActor == null) return null;
        Vector2D direction = chosenActor.calcPosition().subtract(parentCell.getPosition()).normalize();
        Vector2D childPosition = wrap(parentCell.getPosition().add(direction.multiply(
                parentCell.getRadiusSize() + config.getCellMinGrowRadiusSize() * 2)), environment);
        if (!isFreePosition(environment, childPosition, config.getCellMinGrowRadiusSize())) return null;
        double energy = parentCell.getEnergy() * chosenActor.getReproductionEnergyShareOutput();
        Cell child = createMutatedChild(config, environment, parentCell, childPosition, chosenActor.getReproductionState(), energy);
        parentCell.setEnergy(parentCell.getEnergy() - energy);
        return child;
    }

    /** Training selection creates offspring independently of biological reproduction decisions. */
    public static Cell createTrainingOffspring(final SimulationConfig config, final Environment environment,
                                               final Cell parent, final Vector2D position, final int state) {
        Vector2D spawn = wrap(position, environment);
        if (!isFreePosition(environment, spawn, config.getCellMinGrowRadiusSize())) return null;
        return createMutatedChild(config, environment, parent, spawn, state, SimulationConfig.CELL_MAX_ENERGY);
    }

    public static boolean isFreePosition(Environment environment, Vector2D position, double radius) {
        if (position == null || !Double.isFinite(position.getX()) || !Double.isFinite(position.getY())) return false;
        return environment.getBlockerList().stream().noneMatch(blocker ->
                blocker.overlapsCircle(position, radius, environment.getWidth(), environment.getHeight()));
    }

    private static Vector2D wrap(Vector2D position, Environment environment) {
        return position.wrapped(environment.getWidth(), environment.getHeight());
    }

    private static Cell createMutatedChild(SimulationConfig config, Environment environment, Cell parent, Vector2D position, int state, double energy) {
        // The parent's factors control this birth's genetic mutation, for both
        // biological reproduction and training. Changes to the child's own
        // factors below affect its future descendants, not its current genome.
        double effectiveMutationRate = Math.min(1.0D, config.getMutationRate() * parent.getMutationRateFactor());
        double effectiveMutationStrength = config.getMutationStrength() * parent.getMutationStrengthFactor();
        long mutationStart = environment.getProfiler().start();
        NeuralNetwork network = parent.getBrain().mutate(effectiveMutationRate, effectiveMutationStrength);
        environment.getProfiler().finish(SimulationProfiler.Phase.MUTATION, mutationStart);
        Cell child = new Cell(position, parent.getRadiusSize(), new CellBrain(network));
        child.startGrowthProcess();
        child.setRotation(parent.getRotation());
        child.setAngularVelocity(parent.getAngularVelocity());
        child.setVelocity(new Vector2D(parent.getVelocity().getX(), parent.getVelocity().getY()));
        CellType type = parent.getType();
        child.setType(new CellType(mutateValue(type.getRed(), typeMutationStrength / 10),
                mutateValue(type.getGreen(), typeMutationStrength / 10), mutateValue(type.getBlue(), typeMutationStrength / 10)));
        child.setEnergy(energy);
        child.setGeneration(parent.getGeneration() + 1);
        child.setCellState(state);
        calcActiveLayersByState(child);
        for (SensorActor actor : child.getSensorActors()) actor.updateCachedPosition();
        CellBrainService.think(child);
        child.setMutationRateFactor(parent.getMutationRateFactor());
        child.setMutationStrengthFactor(parent.getMutationStrengthFactor());
        // Meta-mutation deliberately uses global settings, avoiding self-amplification.
        child.mutateMutationFactors(config.getMutationRate(), config.getMutationStrength());
        return child;
    }

    public static void calcActiveLayersByState(final Cell cell) {
        if (cell.getBrain() instanceof CellBrain brain) brain.applyCellState(cell.getCellState());
    }

    private static double mutateValue(double value, double strength) {
        double mutation = (random.nextDouble() * 2 - 1) * strength;
        return Math.max(0.0, Math.min(1.0, value + mutation));
    }

}
