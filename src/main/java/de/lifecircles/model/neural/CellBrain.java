package de.lifecircles.model.neural;

import de.lifecircles.service.SimulationConfig;
import java.io.Serializable;

/**
 * Manages the neural network that controls cell behavior.
 */
public class CellBrain implements CellBrainInterface, Serializable {
    private static final long serialVersionUID = 1L;
    private final NeuralNetwork network;

    /**
     * Erstellt ein neues CellBrain mit angegebener Synapsen-Konnektivität.
     *
     * @param hiddenLayerSynapseConnectivity Prozentsatz der zu erstellenden Synapsen (0.0-1.0)
     */
    public CellBrain(final int inputCount, final int outputCount, final double hiddenCountFactor,
                     final double stateHiddenLayerSynapseConnectivity, final double hiddenLayerSynapseConnectivity) {

        //final int hiddenCount = (inputCount + outputCount) * 2; // Arbitrary hidden layer size
        final int[] hiddenCountArr = {
                (int)(inputCount * hiddenCountFactor),
                outputCount
        };

        this.network = new NeuralNetwork(inputCount, hiddenCountArr, outputCount,
                hiddenLayerSynapseConnectivity, SimulationConfig.CELL_STATE_ACTIVE_LAYER_COUNT);

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
    public long getProccessedSynapses() {
        return this.network.getProccessedSynapses();
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

    @Override
    public double getInputValue(int inputNeuronPos) {
        return this.network.readNeuronValue(this.network.getInputNeuronArr()[inputNeuronPos]);
    }

    public NeuralNetwork getNeuralNetwork() {
        return this.network;
    }
}
