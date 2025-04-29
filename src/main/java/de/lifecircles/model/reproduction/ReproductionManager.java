package de.lifecircles.model.reproduction;

import de.lifecircles.model.Cell;
import de.lifecircles.model.CellType;
import de.lifecircles.model.Vector2D;
import de.lifecircles.service.SimulationConfig;

import java.util.Random;

/**
 * Manages cell reproduction and mutation.
 */
public class ReproductionManager {
    private static double energyThreshold = 0.2;
    private static double minReproductionAge = 2.0; // seconds
    private static double typeMutationStrength = 0.1;
    private static double sizeMutationStrength = 0.2;
    private static double mutationRate = 0.1;
    private static double mutationStrength = 0.2;
    private static double reproductionDesireThreshold = 0.5;
    private static final Random random = new Random();

    /**
     * Checks if a cell is ready to reproduce.
     */
    public static boolean canReproduce(Cell cell) {
        return cell.getEnergy() >= energyThreshold &&
               cell.getAge() >= minReproductionAge &&
               cell.getReproductionDesire() >= reproductionDesireThreshold;
    }

    /**
     * Creates a child cell through reproduction.
     * The child inherits traits from the parent with mutations.
     * @param config 
     */
    public static Cell reproduce(SimulationConfig config, Cell parent) {
        // Calculate child position slightly offset from parent
        double angle = random.nextDouble() * 2 * Math.PI;
        Vector2D offset = new Vector2D(
            Math.cos(angle) * (parent.getSize() * 0.3D),
            Math.sin(angle) * (parent.getSize() * 0.3D)
        );
        Vector2D childPosition = parent.getPosition().add(offset);

        // Set initial size (slightly mutated from parent's initial size)
        final double parentSize = parent.getSize();
        double initialSize = mutateValue(parentSize, sizeMutationStrength, config.getCellMaxRadius());

        // Create child cell
        Cell child = new Cell(childPosition, initialSize);

        // Inherit and mutate type
        CellType parentType = parent.getType();
        CellType childType = new CellType(
            mutateValue(parentType.getRed(), typeMutationStrength),
            mutateValue(parentType.getGreen(), typeMutationStrength),
            mutateValue(parentType.getBlue(), typeMutationStrength)
        );
        child.setType(childType);

        // Share energy equally between parent and child
        double parentEnergy = parent.getEnergy();
        parent.setEnergy(parentEnergy / 2);
        child.setEnergy(parentEnergy / 2);

        // Create mutated brain for child
        child.setBrain(parent.getBrain().mutate(child));

        // Inherit and increment generation counter
        child.setGeneration(parent.getGeneration() + 1);

        return child;
    }

    private static double mutateValue(double value, double strength) {
        double mutation = (random.nextDouble() * 2 - 1) * strength;
        return Math.max(0.0, Math.min(1.0, value + mutation));
    }

    private static double mutateValue(double value, double strength, double max) {
        double mutation = (random.nextDouble() * 2 - 1) * strength;
        return Math.max(Math.max(0.0, Math.min(max, value + mutation)), max);
    }

    // Getters and setters for configuration
    public static double getEnergyThreshold() { return energyThreshold; }
    public static void setEnergyThreshold(double threshold) { energyThreshold = threshold; }

    public static double getMutationRate() { return mutationRate; }
    public static void setMutationRate(double rate) { mutationRate = rate; }

    public static double getMutationStrength() { return mutationStrength; }
    public static void setMutationStrength(double strength) { mutationStrength = strength; }

    /**
     * Threshold for neural network reproduction output.
     */
    public static double getReproductionDesireThreshold() { return reproductionDesireThreshold; }
    public static void setReproductionDesireThreshold(double threshold) { reproductionDesireThreshold = threshold; }
}
