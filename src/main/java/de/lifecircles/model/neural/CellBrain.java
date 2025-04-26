package de.lifecircles.model.neural;

import de.lifecircles.model.Cell;
import de.lifecircles.model.CellType;
import de.lifecircles.model.SensorActor;
import de.lifecircles.model.reproduction.ReproductionManager;

import java.util.List;
import java.util.Random;

/**
 * Manages the neural network that controls cell behavior.
 */
public class CellBrain {
    // Neural network input counts
    private static final int SENSOR_INPUTS_PER_ACTOR = 4; // type(R,G,B) + field strength
    private static final int CELL_TYPE_INPUTS = 3; // R,G,B
    private static final int ENVIRONMENT_TYPE_INPUTS = 3; // R,G,B for surrounding cells

    // Neural network output counts
    private static final int SIZE_OUTPUT = 1;
    private static final int OUTPUTS_PER_ACTOR = 5; // type(R,G,B) + force + beam trigger
    private static final int REPRODUCTION_OUTPUT = 1; // reproduction desire

    /** Threshold for energy beam trigger output */
    private static final double ENERGY_BEAM_THRESHOLD = 0.9;

    private final Cell cell;
    private final NeuralNetwork network;

    public CellBrain(Cell cell) {
        this.cell = cell;
        
        int inputCount = (SENSOR_INPUTS_PER_ACTOR * cell.getSensorActors().size()) +
                        CELL_TYPE_INPUTS +
                        ENVIRONMENT_TYPE_INPUTS;
        
        int outputCount = SIZE_OUTPUT +
                         (OUTPUTS_PER_ACTOR * cell.getSensorActors().size()) +
                         REPRODUCTION_OUTPUT;
        
        int hiddenCount = (inputCount + outputCount) * 2; // Arbitrary hidden layer size
        
        this.network = new NeuralNetwork(inputCount, hiddenCount, outputCount);
    }

    /**
     * Updates the cell's behavior based on its current state and environment.
     * @param surroundingCellTypes List of cell types in the vicinity
     */
    public void think(List<CellType> surroundingCellTypes) {
        double[] inputs = generateInputs(surroundingCellTypes);
        network.setInputs(inputs);
        double[] outputs = network.process();
        applyOutputs(outputs);
    }

    /**
     * Creates a mutated copy of this brain for a new cell.
     */
    public CellBrain mutate(Cell newCell) {
        CellBrain childBrain = new CellBrain(newCell);
        
        // Copy and mutate network weights and biases
        childBrain.network.copyFrom(this.network);
        childBrain.network.mutate(
            ReproductionManager.getMutationRate(),
            ReproductionManager.getMutationStrength()
        );
        
        return childBrain;
    }


    private double[] generateInputs(List<CellType> surroundingCellTypes) {
        List<SensorActor> actors = cell.getSensorActors();
        double[] inputs = new double[network.getInputCount()];
        int index = 0;

        // Sensor inputs
        for (SensorActor actor : actors) {
            CellType type = actor.getType();
            inputs[index++] = type.getRed();
            inputs[index++] = type.getGreen();
            inputs[index++] = type.getBlue();
            inputs[index++] = actor.getForceStrength();
        }

        // Cell type inputs
        CellType cellType = cell.getType();
        inputs[index++] = cellType.getRed();
        inputs[index++] = cellType.getGreen();
        inputs[index++] = cellType.getBlue();

        // Environment type inputs (average of surrounding cells)
        CellType avgSurroundingType = CellType.mix(
            surroundingCellTypes.toArray(new CellType[0])
        );
        inputs[index++] = avgSurroundingType.getRed();
        inputs[index++] = avgSurroundingType.getGreen();
        inputs[index++] = avgSurroundingType.getBlue();

        return inputs;
    }

    private void applyOutputs(double[] outputs) {
        int index = 0;

        // Apply size output
        cell.setSize(outputs[index++] * 40 + 10); // Scale to 10-50 range

        // Apply actor outputs
        List<SensorActor> actors = cell.getSensorActors();
        for (SensorActor actor : actors) {
            // Set actor type
            CellType newType = new CellType(
                outputs[index++],
                outputs[index++],
                outputs[index++]  
            );
            actor.setType(newType);

            // Set force strength (range -1 to 1)
            actor.setForceStrength(outputs[index++] * 2 - 1);
            // Set energy beam trigger
            actor.setFireEnergyBeam(outputs[index++] > ENERGY_BEAM_THRESHOLD);
        }
        // Set reproduction desire output
        cell.setReproductionDesire(outputs[index++]);
    }

    private int getInputCount() {
        return (SENSOR_INPUTS_PER_ACTOR * cell.getSensorActors().size()) +
               CELL_TYPE_INPUTS +
               ENVIRONMENT_TYPE_INPUTS;
    }

    public int getSynapseCount() {
        return this.network.getSynapseCount();
    }
}
