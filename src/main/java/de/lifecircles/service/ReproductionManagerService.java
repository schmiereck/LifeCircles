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
        return reproduce(config, environment, parentCell, config.getDivisionMutationRateMultiplier());
    }

    /** Controlled development experiments can disable birth mutation without changing global settings. */
    public static Cell reproduceClonally(SimulationConfig config, Environment environment, Cell parent) {
        return reproduce(config, environment, parent, 0);
    }

    private static Cell reproduce(SimulationConfig config, Environment environment, Cell parentCell, double divisionMultiplier) {
        return reproduce(config, environment, parentCell, divisionMultiplier, null);
    }

    /** Reproducible birth traits and meta-mutation; brain RNG is supplied through NeuralNetwork. */
    public static Cell reproduce(SimulationConfig config, Environment world, Cell parent, Random birthRandom) {
        return reproduce(config, world, parent, config.getDivisionMutationRateMultiplier(), birthRandom);
    }

    private static Cell reproduce(SimulationConfig config, Environment environment, Cell parentCell,
                                  double divisionMultiplier, Random birthRandom) {
        SensorActor chosenActor = parentCell.getSensorActors().stream()
                .filter(actor -> actor.getReproductionDesire() >= config.getReproductionDesireThreshold())
                .max(Comparator.comparingDouble(SensorActor::getReproductionDesire)).orElse(null);
        if (chosenActor == null) return null;
        Vector2D direction = chosenActor.calcPosition().subtract(parentCell.getPosition()).normalize();
        Vector2D childPosition = wrap(parentCell.getPosition().add(direction.multiply(
                parentCell.getRadiusSize() + config.getCellMinGrowRadiusSize() * 2)), environment);
        if (!isFreePosition(environment, childPosition, config.getCellMinGrowRadiusSize())) return null;
        double energy = parentCell.getEnergy() * chosenActor.getReproductionEnergyShareOutput();
        Cell child = createMutatedChild(config, environment, parentCell, childPosition, chosenActor.getReproductionState(), energy, divisionMultiplier, birthRandom);
        parentCell.setEnergy(parentCell.getEnergy() - energy);
        return child;
    }

    /** Training selection creates offspring independently of biological reproduction decisions. */
    public static Cell createTrainingOffspring(final SimulationConfig config, final Environment environment,
                                               final Cell parent, final Vector2D position, final int state) {
        Vector2D spawn = wrap(position, environment);
        if (!isFreePosition(environment, spawn, config.getCellMinGrowRadiusSize())) return null;
        return createTrainingFounder(config, environment, parent, spawn, state, true);
    }

    /** Fresh, equally initialized evaluation episode; elites copy genes without mutation. */
    public static Cell createTrainingFounder(SimulationConfig config, Environment environment,
                                             Cell parent, Vector2D position, int state, boolean mutate) {
        double rate = mutate ? Math.min(1, config.getMutationRate() * parent.getMutationRateFactor()) : 0;
        double strength = mutate ? config.getMutationStrength() * parent.getMutationStrengthFactor() : 0;
        long start = environment.getProfiler().start();
        NeuralNetwork network = parent.getBrain().mutate(rate, strength);
        environment.getProfiler().finish(SimulationProfiler.Phase.MUTATION, start);
        Cell founder = new Cell(position, config.getCellMaxRadiusSize() / 2, new CellBrain(network));
        founder.startGrowthProcess();
        founder.setType(parent.getType());
        founder.setGeneration(parent.getGeneration() + (mutate ? 1 : 0));
        founder.setMutationRateFactor(parent.getMutationRateFactor());
        founder.setMutationStrengthFactor(parent.getMutationStrengthFactor());
        if (mutate) founder.mutateMutationFactors(config.getMutationRate(), config.getMutationStrength());
        founder.setCellState(state);
        network.rnnClearPreviousState();
        for (SensorActor actor : founder.getSensorActors()) actor.updateCachedPosition();
        CellBrainService.think(founder);
        return founder;
    }

    public static boolean isFreePosition(Environment environment, Vector2D position, double radius) {
        if (position == null || !Double.isFinite(position.getX()) || !Double.isFinite(position.getY())) return false;
        return environment.getBlockerList().stream().noneMatch(blocker ->
                blocker.overlapsCircle(position, radius, environment.getWidth(), environment.getHeight()));
    }

    private static Vector2D wrap(Vector2D position, Environment environment) {
        return position.wrapped(environment.getWidth(), environment.getHeight());
    }

    private static Cell createMutatedChild(SimulationConfig config, Environment environment, Cell parent, Vector2D position, int state, double energy, double divisionMultiplier, Random birthRandom) {
        // The parent's factors control this birth's genetic mutation, for both
        // biological reproduction and training. Changes to the child's own
        // factors below affect its future descendants, not its current genome.
        double divisionRate = config.getMutationRate() * divisionMultiplier;
        double effectiveMutationRate = Math.min(1.0D, divisionRate * parent.getMutationRateFactor());
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
        double colorStrength = typeMutationStrength / 10 * divisionMultiplier;
        Random traitRandom = birthRandom == null ? random : birthRandom;
        child.setType(new CellType(mutateValue(type.getRed(), colorStrength, traitRandom),
                mutateValue(type.getGreen(), colorStrength, traitRandom), mutateValue(type.getBlue(), colorStrength, traitRandom)));
        child.setEnergy(energy);
        child.setGeneration(parent.getGeneration() + 1);
        child.setCellState(state);
        calcActiveLayersByState(child);
        for (SensorActor actor : child.getSensorActors()) actor.updateCachedPosition();
        CellBrainService.think(child);
        child.setMutationRateFactor(parent.getMutationRateFactor());
        child.setMutationStrengthFactor(parent.getMutationStrengthFactor());
        // Meta-mutation deliberately uses global settings, avoiding self-amplification.
        if (birthRandom == null) child.mutateMutationFactors(divisionRate, config.getMutationStrength());
        else child.mutateMutationFactors(divisionRate, config.getMutationStrength(), birthRandom);
        return child;
    }

    public static void calcActiveLayersByState(final Cell cell) {
        if (cell.getBrain() instanceof CellBrain brain) brain.applyCellState(cell.getCellState());
    }

    private static double mutateValue(double value, double strength, Random random) {
        double mutation = (random.nextDouble() * 2 - 1) * strength;
        return Math.max(0.0, Math.min(1.0, value + mutation));
    }

}
