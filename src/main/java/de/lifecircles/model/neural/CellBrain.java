package de.lifecircles.model.neural;

import de.lifecircles.model.Cell;
import de.lifecircles.service.SimulationConfig;
import java.io.Serializable;

/**
 * Manages the neural network that controls cell behavior.
 */
public class CellBrain implements CellBrainInterface, Serializable {
    private static final long serialVersionUID = 1L;
    private final NeuralNetwork network;


    /**
     * Erstellt ein neues CellBrain mit vollständiger Synapsen-Konnektivität (100%).
     *
     * @param cell Die Zelle, zu der dieses Gehirn gehört
     */
    public CellBrain(final Cell cell) {
        this(cell, 1.0D, 1.0D, 1.0D);
    }

    /**
     * Erstellt ein neues CellBrain mit angegebener Synapsen-Konnektivität.
     * 
     * @param cell Die Zelle, zu der dieses Gehirn gehört
     * @param synapseConnectivity Prozentsatz der zu erstellenden Synapsen (0.0-1.0)
     */
    public CellBrain(final Cell cell, final double hiddenCountFactor,
                     final double stateHiddenLayerSynapseConnectivity, final double synapseConnectivity) {
        int inputCount = GlobalInputFeature.values().length +
                         (SensorInputFeature.values().length * cell.getSensorActors().size());
        
        int outputCount = GlobalInputFeature.values().length +
                         (ActorOutputFeature.values().length * cell.getSensorActors().size());

        //final int hiddenCount = (inputCount + outputCount) * 2; // Arbitrary hidden layer size
        final int[] hiddenCountArr = {
                (int)(inputCount * hiddenCountFactor),
                outputCount
        };

        this.network = new NeuralNetwork(inputCount, hiddenCountArr, outputCount,
                synapseConnectivity, SimulationConfig.CELL_STATE_ACTIVE_LAYER_COUNT);

        this.network.addHiddenLayer(0, inputCount / 4, stateHiddenLayerSynapseConnectivity); // State 0
        this.network.addHiddenLayer(0, inputCount / 4, stateHiddenLayerSynapseConnectivity); // State 1
        this.network.addHiddenLayer(0, inputCount / 4, stateHiddenLayerSynapseConnectivity); // State 2
    }

    /**
     * Erstellt ein CellBrain mit bestehendem neuronalen Netzwerk.
     * 
     * @param neuralNetwork Das zu verwendende neuronale Netzwerk
     */
    public CellBrain(final NeuralNetwork neuralNetwork) {
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

    @Override
    public void setInputs(final double[] inputs) {
        this.network.setInputs(inputs);
    }

    @Override
    public double[] process() {
        return this.network.process();
    }

    @Override
    public int getInputCount() {
        return this.network.getInputCount();
    }

    @Override
    public NeuralNetwork mutate(double mutationRate, double mutationStrength) {
        return this.network.mutate(
                mutationRate,
                mutationStrength
            );
    }

    @Override
    public double getOutputValue(int outputNeuronPos) {
        return  this.network.getOutputValue(outputNeuronPos);
    }

    public NeuralNetwork getNeuralNetwork() {
        return this.network;
    }
}
