package de.lifecircles.model.reproduction;

import de.lifecircles.model.Cell;
import de.lifecircles.model.CellType;
import de.lifecircles.model.Vector2D;
import de.lifecircles.model.neural.CellBrain;
import de.lifecircles.model.neural.NeuralNetwork;
import de.lifecircles.service.SimulationConfig;

import java.util.Random;

/**
 * Manages cell reproduction and mutation.
 */
public class ReproductionManager {
    private static double typeMutationStrength = 0.1;
    private static double sizeMutationStrength = 0.2;
    private static final Random random = new Random();

    /**
     * Checks if a cell is ready to reproduce.
     */
    public static boolean canReproduce(SimulationConfig config, Cell cell) {
        return cell.getEnergy() >= config.getMinReproductionEnergy() &&
               cell.getAge() >= config.getMinReproductionAge() &&
               cell.getReproductionDesire() >= config.getMinReproductionDesire();
    }

    /**
     * Creates a child cell through reproduction.
     * The child inherits traits from the parent with mutations.
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
        final double initialSize = mutateValue(parentSize, sizeMutationStrength, config.getCellMaxRadius());

        // Create mutated brain for child
        final CellBrain parentBrain = parent.getBrain();
        final NeuralNetwork parentBrainNetwork = parentBrain.getNetwork();
        final NeuralNetwork childBrainNetwork = parentBrainNetwork.mutate(
                config.getMutationRate(),
                config.getMutationStrength()
        );

        // Create child cell
        final Cell child = new Cell(childPosition, initialSize, childBrainNetwork);

        // Inherit and mutate type
        final CellType parentType = parent.getType();
        final CellType childType = new CellType(
            mutateValue(parentType.getRed(), typeMutationStrength),
            mutateValue(parentType.getGreen(), typeMutationStrength),
            mutateValue(parentType.getBlue(), typeMutationStrength)
        );
        child.setType(childType);

        // Share energy equally between parent and child
        final double parentEnergy = parent.getEnergy();
        parent.setEnergy(parentEnergy / 2);
        child.setEnergy(parentEnergy / 2);

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
}
