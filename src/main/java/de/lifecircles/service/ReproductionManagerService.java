package de.lifecircles.service;

import de.lifecircles.model.Cell;
import de.lifecircles.model.CellType;
import de.lifecircles.model.SensorActor;
import de.lifecircles.model.Vector2D;
import de.lifecircles.model.neural.CellBrain;
import de.lifecircles.model.neural.CellBrainService;
import de.lifecircles.model.neural.NeuralNetwork;

import java.util.Comparator;
import java.util.Objects;
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
     * The child inherits traits from the parent with mutations.
     */
    public static Cell reproduce(SimulationConfig config, Cell parent) {
        final Cell child;

        // Calculate child position slightly offset from parent
        //double angle = random.nextDouble() * 2 * Math.PI;
        //Vector2D offset = new Vector2D(
        //    Math.cos(angle) * (parent.getRadiusSize() * 0.3D),
        //    Math.sin(angle) * (parent.getRadiusSize() * 0.3D)
        //);
        //Vector2D childPosition = parent.getPosition().add(offset);

        SensorActor chosenActor = parent.getSensorActors().stream()
                .filter(sensorActor -> sensorActor.getReproductionDesire() >= config.getReproductionDesireThreshold())
                .max(Comparator.comparingDouble(SensorActor::getReproductionDesire))
                .orElse(null);

        if (Objects.nonNull(chosenActor)) {
            Vector2D direction = chosenActorDirection(parent, chosenActor);
            if (Objects.nonNull(direction)) {
                Vector2D childPosition = parent.getPosition().add(direction.multiply(parent.getRadiusSize()));

                // Set initial size (slightly mutated from parent's initial size)
                final double parentSize = parent.getRadiusSize();

                // Create mutated brain for child
                final CellBrain parentBrain = parent.getBrain();
                final NeuralNetwork parentBrainNetwork = parentBrain.getNetwork();
                final NeuralNetwork childBrainNetwork = parentBrainNetwork.mutate(
                        config.getMutationRate(),
                        config.getMutationStrength()
                );

                // Create child cell
                child = new Cell(childPosition, parentSize, childBrainNetwork);

                // Übernehme die Rotation der Mutter-Zelle
                //child.setAngularVelocity(parent.getAngularVelocity());
                child.setRotation(parent.getRotation());

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

                // Ensure child's neural network outputs are set by running think once
                CellBrainService.think(child);
            } else {
                child = null;
            }
        } else {
            child = null;
        }
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

    private static Vector2D chosenActorDirection(Cell parent, SensorActor chosenActor) {
        if (Objects.nonNull(chosenActor.getCachedPosition())) {
            return chosenActor.getCachedPosition().subtract(parent.getPosition()).normalize();
        } else {
            return null;
        }
    }
}
