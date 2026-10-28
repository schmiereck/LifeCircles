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
        return !cell.isRemains() && cell.getEnergy() >= config.getReproductionEnergyThreshold() &&
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
        if (parentCell.isRemains()) return null;
        SensorActor chosenActor = parentCell.getSensorActors().stream()
                .filter(actor -> actor.getReproductionDesire() >= config.getReproductionDesireThreshold())
                .max(Comparator.comparingDouble(SensorActor::getReproductionDesire)).orElse(null);
        if (chosenActor == null) return null;
        Vector2D direction = chosenActor.calcPosition().subtract(parentCell.getPosition()).normalize();
        // TEILUNG.md: division splits the area. The mother keeps half of it at once; the child starts small at her
        // new rim and grows to the other half (or what its brain wants) no faster than CELL_MAX_GROWTH_SPEED.
        double half = parentCell.getRadiusSize() / Math.sqrt(2);
        Vector2D childPosition = wrap(parentCell.getPosition().add(direction.multiply(
                half + config.getCellMinGrowRadiusSize() * 2)), environment);
        if (!isFreePosition(environment, childPosition, config.getCellMinGrowRadiusSize())) return null;
        double share = chosenActor.getReproductionEnergyShareOutput();
        Cell child = createMutatedChild(config, environment, parentCell, childPosition, chosenActor.getReproductionState(), share, divisionMultiplier, birthRandom);
        child.setRadiusSize(half);
        parentCell.setRealGrowRadiusSize(half);
        // Every cell has the same linear mass, so copying the velocity would double the momentum: share it.
        Vector2D shared = parentCell.getVelocity().multiply(.5);
        parentCell.setVelocity(shared); child.setVelocity(shared);
        for (SensorActor actor : parentCell.getSensorActors()) actor.updateCachedPosition();
        for (EnergyType type : EnergyType.ALL)
            parentCell.setEnergy(type, parentCell.getEnergy(type) - child.getEnergy(type));
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
        if (parent.isRemains()) throw new IllegalArgumentException("Remains contain no living genome");
        double rate = mutate ? Math.min(1, config.getMutationRate() * parent.getMutationRateFactor()) : 0;
        double strength = mutate ? config.getMutationStrength() * parent.getMutationStrengthFactor() : 0;
        long start = environment.getProfiler().start();
        NeuralNetwork network = parent.getBrain().mutate(rate, strength);
        environment.getProfiler().finish(SimulationProfiler.Phase.MUTATION, start);
        Cell founder = new Cell(position, config.getCellMaxRadiusSize() / 2, new CellBrain(network));
        EnergyAllocationService.initializeFounder(founder,1);
        founder.startGrowthProcess();
        founder.setType(parent.getType());
        founder.setGeneration(parent.getGeneration() + (mutate ? 1 : 0));
        founder.setMutationRateFactor(parent.getMutationRateFactor());
        founder.setMutationStrengthFactor(parent.getMutationStrengthFactor());
        if (mutate) founder.mutateMutationFactors(config.getMutationRate(), config.getMutationStrength());
        founder.setEnergyConversionProgram(parent.getEnergyConversionProgram().mutate(NeuralNetwork.getRandom(),rate,strength));
        founder.setClockPeriods(parent.getClockPeriods());
        if (mutate) founder.mutateClockPeriods(rate, strength, NeuralNetwork.getRandom());
        founder.setCellState(state);
        network.rnnClearPreviousState();
        for (SensorActor actor : founder.getSensorActors()) actor.updateCachedPosition();
        // First decision is funded by the normal world step, after episode reserves are assigned.
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

    private static Cell createMutatedChild(SimulationConfig config, Environment environment, Cell parent, Vector2D position, int state, double energyShare, double divisionMultiplier, Random birthRandom) {
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
        // Biological division partitions every reserve; training founders remain fresh episodes.
        for (EnergyType energyType : EnergyType.ALL) child.setEnergy(energyType, parent.getEnergy(energyType) * energyShare);
        child.setGeneration(parent.getGeneration() + 1);
        child.setCellState(state);
        calcActiveLayersByState(child);
        for (SensorActor actor : child.getSensorActors()) actor.updateCachedPosition();
        // No free newborn decision; keep the resource split exact and think on its first world step.
        child.setMutationRateFactor(parent.getMutationRateFactor());
        child.setMutationStrengthFactor(parent.getMutationStrengthFactor());
        // Meta-mutation deliberately uses global settings, avoiding self-amplification.
        if (birthRandom == null) child.mutateMutationFactors(divisionRate, config.getMutationStrength());
        else child.mutateMutationFactors(divisionRate, config.getMutationStrength(), birthRandom);
        child.setEnergyConversionProgram(parent.getEnergyConversionProgram().mutate(traitRandom,effectiveMutationRate,effectiveMutationStrength));
        child.setClockPeriods(parent.getClockPeriods());
        child.mutateClockPeriods(effectiveMutationRate, effectiveMutationStrength, traitRandom);
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
