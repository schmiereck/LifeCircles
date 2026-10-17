package de.lifecircles.model.neural;

import de.lifecircles.service.SimulationConfig;
import java.io.Serializable;

/**
 * Manages the neural network that controls cell behavior.
 */
public class CellBrain implements CellBrainInterface, Serializable {
    private static final long serialVersionUID = 1L;
    private final NeuralNetwork network;

    /** Compact founding genotype; interfaces do not determine the hidden network size. */
    public static CellBrain createDefault() {
        var factory = new ValuesNeuronValueFunctionFactory();
        var network = new NeuralNetwork(factory,
                de.lifecircles.model.CellFactory.calcInputCount(SimulationConfig.CELL_SENSOR_ACTOR_COUNT),
                new int[]{SimulationConfig.DEFAULT_BRAIN_TRUNK_NEURONS},
                de.lifecircles.model.CellFactory.calcOutputCount(SimulationConfig.CELL_SENSOR_ACTOR_COUNT),
                SimulationConfig.DEFAULT_BRAIN_CONNECTIVITY, SimulationConfig.CELL_STATE_ACTIVE_LAYER_COUNT);
        network.setEnableNeuronType(true);
        network.getNeuralNet().addParallelStateLayers(factory, network.getNeuronValueFunction(),
                SimulationConfig.DEFAULT_BRAIN_STATE_NEURONS, SimulationConfig.DEFAULT_BRAIN_STATE_CONNECTIVITY);
        return new CellBrain(network);
    }

    /**
     * Erstellt ein neues CellBrain mit angegebener Synapsen-Konnektivität.
     *
     * @param hiddenLayerSynapseConnectivity Prozentsatz der zu erstellenden Synapsen (0.0-1.0)
     */
    public CellBrain(final NeuronValueFunctionFactory neuronValueFunctionFactory,
                     final int inputCount, final int outputCount, final double hiddenCountFactor,
                     final double stateHiddenLayerSynapseConnectivity, final double hiddenLayerSynapseConnectivity) {

        //final int hiddenCount = (inputCount + outputCount) * 2; // Arbitrary hidden layer size
        int hiddenSizingInputs = inputCount == EnergyInputLayout.inputCount(SimulationConfig.CELL_SENSOR_ACTOR_COUNT)
                ? EnergyInputLayout.legacyInputCount(SimulationConfig.CELL_SENSOR_ACTOR_COUNT) : inputCount;
        final int[] hiddenCountArr = {
                (int)(hiddenSizingInputs * hiddenCountFactor),
                outputCount
        };

        this.network = new NeuralNetwork(neuronValueFunctionFactory,
                inputCount, hiddenCountArr, outputCount,
                hiddenLayerSynapseConnectivity, SimulationConfig.CELL_STATE_ACTIVE_LAYER_COUNT);
        this.network.setEnableNeuronType(true);

        this.network.getNeuralNet().addParallelStateLayers(neuronValueFunctionFactory,
                this.network.getNeuronValueFunction(), hiddenSizingInputs / 4, stateHiddenLayerSynapseConnectivity);
    }

    /** Old cell outputs get the appended enzyme throttle, disconnected so full activity is kept. */
    public void ensureEnzymeOutput(int actors) {
        if (network.getNeuralNet().getOutputNeuronArr().length == OutputLayout.legacyOutputCount(actors))
            network.getNeuralNet().ensureOutputCount(network.getNeuronValueFunction(), OutputLayout.outputCount(actors));
    }

    /** Old cell interfaces get new disconnected channels; unrelated small lab networks stay intact. */
    public void ensureEnergyInputs(int actors) {
        if (network.getNeuralNet().getInputCount() == EnergyInputLayout.legacyInputCount(actors)
                || network.getNeuralNet().getInputCount() == EnergyInputLayout.energyOnlyInputCount(actors))
            network.getNeuralNet().ensureInputCount(network.getNeuronValueFunction(), EnergyInputLayout.inputCount(actors));
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
        return this.network.getNeuralNet().getStoredSynapseCount();
    }

    public boolean[] determineActiveHiddenLayers(int cellState) {
        boolean[] layers = new boolean[SimulationConfig.CELL_STATE_ACTIVE_LAYER_COUNT];
        for (int i = 0; i < layers.length; i++) {
            layers[i] = (cellState & (1 << i)) != 0; // Aktivierung basierend auf Bitmasken
        }
        return layers;
    }

    /** Apply state bits immediately, including clearing disabled outputs for recurrent edges. */
    public void applyCellState(int cellState) {
        boolean[] active = determineActiveHiddenLayers(cellState);
        Layer[] layers = network.getHiddenLayerArr();
        for (int i = 0; i < Math.min(active.length, Math.min(layers.length,
                network.getNeuralNet().getFixedHiddenLayerCount())); i++) {
            layers[i].setActiveLayer(active[i]);
            if (!active[i]) for (NeuronInterface neuron : layers[i].getNeuronsArr())
                for (int port = 0; port < neuron.getNeuronTypeInfoData().getOutputCount(); port++)
                    network.writeNeuronValue(neuron, port, 0);
        }
    }

    @Override
    public void setInputs(final double[] inputs) {
        this.network.setInputs(inputs);
    }

    @Override
    public double[] process() {
        return this.network.getNeuralNet().process(this.network.getNeuronValueFunction(), false);
    }

    @Override
    public int getInputCount() {
        return this.network.getNeuralNet().getInputCount();
    }

    @Override
    public long getProccessedSynapses() {
        return this.network.getProccessedSynapses();
    }
    @Override public long getMaximumProcessedSynapses() { return network.getNeuralNet().getMaximumProcessedSynapses(); }

    @Override
    public NeuralNetwork mutate(final double mutationRate, final double mutationStrength) {
        return this.network.mutate(
                mutationRate,
                mutationStrength
            );
    }

    @Override
    public double getOutputValue(final int outputNeuronPos) {
        return  this.network.getNeuralNet().getOutputValue(this.network.getNeuronValueFunction(), outputNeuronPos);
    }

    @Override
    public double getInputValue(final int inputNeuronPos) {
        final int outputTypePos = 0; // Default-Output-Type für Input-Neuronen.
        return this.network.readNeuronValue(this.network.getNeuralNet().getInputNeuronArr()[inputNeuronPos], outputTypePos);
    }

    public NeuralNetwork getNeuralNetwork() {
        return this.network;
    }
}
