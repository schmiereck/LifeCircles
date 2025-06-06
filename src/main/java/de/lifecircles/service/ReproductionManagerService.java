package de.lifecircles.service;

import de.lifecircles.model.*;
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
     * The child inherits traits from the parentCell with mutations.
     */
    public static Cell reproduce(final SimulationConfig config, final Environment environment, final Cell parentCell) {
        final Cell child;

        // Calculate child position slightly offset from parentCell
        //double angle = random.nextDouble() * 2 * Math.PI;
        //Vector2D offset = new Vector2D(
        //    Math.cos(angle) * (parentCell.getRadiusSize() * 0.3D),
        //    Math.sin(angle) * (parentCell.getRadiusSize() * 0.3D)
        //);
        //Vector2D childPosition = parentCell.getPosition().add(offset);

        SensorActor chosenActor = parentCell.getSensorActors().stream()
                .filter(sensorActor -> sensorActor.getReproductionDesire() >= config.getReproductionDesireThreshold())
                .max(Comparator.comparingDouble(SensorActor::getReproductionDesire))
                .orElse(null);

        if (Objects.nonNull(chosenActor)) {
            Vector2D direction = chosenActorDirection(parentCell, chosenActor);
            if (Objects.nonNull(direction)) {
                Vector2D childPosition = parentCell.getPosition().
                        add(direction.multiply(parentCell.getRadiusSize() +
                                (SimulationConfig.getInstance().getCellMinGrowRadiusSize() * 2.0D)));

                if (!BlockerCellCalcService.checkCellIsInsideBlocker(childPosition, environment.getBlockers())) {
                    // Set initial size (slightly mutated from parentCell's initial size)
                    final double parentSize = parentCell.getRadiusSize();

                    // Create mutated brain for child
                    final CellBrainInterface parentBrain = parentCell.getBrain();
                    final NeuralNetwork childBrainNetwork = parentBrain.mutate(
                            config.getMutationRate(),
                            config.getMutationStrength()
                    );

                    final CellBrain childCellBrain = new CellBrain(childBrainNetwork);

                    // Create child cell
                    child = new Cell(childPosition, parentSize, childCellBrain);

                    // Starte den Wachstumsprozess der neuen Zelle
                    child.startGrowthProcess();

                    // Übernehme die Rotation der Mutter-Zelle
                    child.setRotation(parentCell.getRotation());

                    // Inherit and mutate type
                    final CellType parentType = parentCell.getType();
                    final CellType childType = new CellType(
                            mutateValue(parentType.getRed(), typeMutationStrength),
                            mutateValue(parentType.getGreen(), typeMutationStrength),
                            mutateValue(parentType.getBlue(), typeMutationStrength)
                    );
                    child.setType(childType);

                    // Share energy based on the actor's energy share percentage
                    final double energySharePercentage = chosenActor.getReproductionEnergyShareOutput();
                    final double energyForChild = parentCell.getEnergy() * energySharePercentage;
                    parentCell.setEnergy(parentCell.getEnergy() - energyForChild);
                    child.setEnergy(energyForChild);

                    // Inherit and increment generation counter
                    child.setGeneration(parentCell.getGeneration() + 1);

                    // Ensure child's neural network outputs are set by running think once
                    CellBrainService.think(child);

                    final int childState = chosenActor.getReproductionState();

                    child.setCellState(childState);

                    // Set active layers in the child's brain based on the cell state
                    boolean[] activeLayers = child.getBrain().determineActiveHiddenLayers(child.getCellState());
                    Layer[] hiddenLayerList = childBrainNetwork.getHiddenLayerList();
                    for (int i = 0; i < Math.min(hiddenLayerList.length, SimulationConfig.CELL_STATE_ACTIVE_LAYER_COUNT); i++) {
                        final Layer layer = hiddenLayerList[i];
                        layer.setActiveLayer(activeLayers[i]);
                    }

                    // Mutate mutationRateFactor and mutationStrengthFactor
                    child.setMutationRateFactor(parentCell.getMutationRateFactor());
                    child.setMutationStrengthFactor(parentCell.getMutationStrengthFactor());
                    child.mutateMutationFactors(config.getMutationRate(), config.getMutationStrength());
                } else {
                    child = null;
                }
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
