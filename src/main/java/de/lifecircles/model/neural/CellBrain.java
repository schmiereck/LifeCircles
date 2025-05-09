package de.lifecircles.model.neural;

import de.lifecircles.model.Cell;
import de.lifecircles.service.SimulationConfig;

/**
 * Manages the neural network that controls cell behavior.
 */
public class CellBrain {
    private final Cell cell;
    private final NeuralNetwork network;

    public CellBrain(final Cell cell) {
        this.cell = cell;
        
        int inputCount = GlobalInputFeature.values().length +
                         (SensorInputFeature.values().length * cell.getSensorActors().size());
        
        int outputCount = GlobalInputFeature.values().length +
                         (ActorOutputFeature.values().length * cell.getSensorActors().size());
        
        int hiddenCount = (inputCount + outputCount) * 2; // Arbitrary hidden layer size
        
        this.network = new NeuralNetwork(inputCount, hiddenCount, outputCount);
    }

    public CellBrain(final Cell cell, final NeuralNetwork neuralNetwork) {
        this.cell = cell;
        this.network = neuralNetwork;
    }

    public int getSynapseCount() {
        return this.network.getSynapseCount();
    }

    public boolean[] determineActiveHiddenLayers(int cellState) {
        boolean[] layers = new boolean[SimulationConfig.CELL_STATE_ACTIVE_LAYER_COUNT];
        for (int i = 0; i < layers.length; i++) {
            layers[i] = (cellState & (1 << i)) != 0; // Aktivierung basierend auf Bitmasken
        }
        return layers;
    }

    public NeuralNetwork getNeuralNetwork() {
        return this.network;
    }
}
