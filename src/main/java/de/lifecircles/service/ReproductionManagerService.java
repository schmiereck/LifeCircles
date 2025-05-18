package de.lifecircles.service;

import de.lifecircles.model.Cell;
import de.lifecircles.model.CellType;
import de.lifecircles.model.SensorActor;
import de.lifecircles.model.Vector2D;
import de.lifecircles.model.neural.*;

import java.util.Comparator;
import java.util.List;
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
                final CellBrainInterface parentBrain = parent.getBrain();
                final NeuralNetwork childBrainNetwork = parentBrain.mutate(
                        config.getMutationRate(),
                        config.getMutationStrength()
                );

                // Create child cell
                child = new Cell(childPosition, parentSize, childBrainNetwork);
                
                // Starte den Wachstumsprozess der neuen Zelle
                child.startGrowthProcess();

                // Übernehme die Rotation der Mutter-Zelle
                child.setRotation(parent.getRotation());

                // Inherit and mutate type
                final CellType parentType = parent.getType();
                final CellType childType = new CellType(
                        mutateValue(parentType.getRed(), typeMutationStrength),
                        mutateValue(parentType.getGreen(), typeMutationStrength),
                        mutateValue(parentType.getBlue(), typeMutationStrength)
                );
                child.setType(childType);

                // Share energy based on the actor's energy share percentage
                final double energySharePercentage = chosenActor.getReproductionEnergyShareOutput();
                final double energyForChild = parent.getEnergy() * energySharePercentage;
                parent.setEnergy(parent.getEnergy() - energyForChild);
                child.setEnergy(energyForChild);

                // Inherit and increment generation counter
                child.setGeneration(parent.getGeneration() + 1);

                // Ensure child's neural network outputs are set by running think once
                CellBrainService.think(child);

                // Abrufen der Ausgaben des neuronalen Netzwerks der Elternzelle
                double state0Output = parentBrain.getOutputValue(GlobalOutputFeature.STATE_0.ordinal());
                double state1Output = parentBrain.getOutputValue(GlobalOutputFeature.STATE_1.ordinal());
                double state2Output = parentBrain.getOutputValue(GlobalOutputFeature.STATE_2.ordinal());

                // Berechnung des Zell-Zustands der Kind-Zelle basierend auf Thresholds
                int childState = 0;
                if (state0Output >= config.getCellStateOutputThreshold()) {
                    childState |= 1; // Setze Bit 0
                }
                if (state1Output >= config.getCellStateOutputThreshold()) {
                    childState |= 2; // Setze Bit 1
                }
                if (state2Output >= config.getCellStateOutputThreshold()) {
                    childState |= 4; // Setze Bit 2
                }
                child.setCellState(childState);

                // Set active layers in the child's brain based on the cell state
                boolean[] activeLayers = child.getBrain().determineActiveHiddenLayers(child.getCellState());
                List<Layer> hiddenLayerList = childBrainNetwork.getHiddenLayerList();
                for (int i = 0; i < Math.min(hiddenLayerList.size(), SimulationConfig.CELL_STATE_ACTIVE_LAYER_COUNT); i++) {
                    final Layer layer = hiddenLayerList.get(i);
                    layer.setActiveLayer(activeLayers[i]);
                }

                // Mutate mutationRateFactor and mutationStrengthFactor
                child.setMutationRateFactor(parent.getMutationRateFactor());
                child.setMutationStrengthFactor(parent.getMutationStrengthFactor());
                child.mutateMutationFactors(config.getMutationRate(), config.getMutationStrength());
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
