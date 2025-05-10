package de.lifecircles.model.neural;

import de.lifecircles.model.Cell;
import de.lifecircles.service.SimulationConfig;
import java.io.Serializable;

/**
 * Manages the neural network that controls cell behavior.
 */
public class CellBrain implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Cell cell;
    private final NeuralNetwork network;

    /**
     * Erstellt ein neues CellBrain mit angegebener Synapsen-Konnektivität.
     * 
     * @param cell Die Zelle, zu der dieses Gehirn gehört
     * @param synapseConnectivity Prozentsatz der zu erstellenden Synapsen (0.0-1.0)
     */
    public CellBrain(final Cell cell, double synapseConnectivity) {
        this.cell = cell;
        
        int inputCount = GlobalInputFeature.values().length +
                         (SensorInputFeature.values().length * cell.getSensorActors().size());
        
        int outputCount = GlobalInputFeature.values().length +
                         (ActorOutputFeature.values().length * cell.getSensorActors().size());

        //final int hiddenCount = (inputCount + outputCount) * 2; // Arbitrary hidden layer size
        final int[] hiddenCountArr = {
                (int)(inputCount * 1.4D),
                outputCount
        };

        this.network = new NeuralNetwork(inputCount, hiddenCountArr, outputCount,
                synapseConnectivity, SimulationConfig.CELL_STATE_ACTIVE_LAYER_COUNT);

        this.network.addHiddenLayer(0, inputCount / 4, 0.1D); // State 0
        this.network.addHiddenLayer(0, inputCount / 4, 0.1D); // State 1
        this.network.addHiddenLayer(0, inputCount / 4, 0.1D); // State 2
    }

    /**
     * Erstellt ein neues CellBrain mit vollständiger Synapsen-Konnektivität (100%).
     * 
     * @param cell Die Zelle, zu der dieses Gehirn gehört
     */
    public CellBrain(final Cell cell) {
        this(cell, 1.0);
    }

    /**
     * Erstellt ein CellBrain mit bestehendem neuronalen Netzwerk.
     * 
     * @param cell Die Zelle, zu der dieses Gehirn gehört
     * @param neuralNetwork Das zu verwendende neuronale Netzwerk
     */
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
