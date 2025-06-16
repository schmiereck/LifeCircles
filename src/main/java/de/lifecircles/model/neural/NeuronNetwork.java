package de.lifecircles.model.neural;

import java.util.ArrayList;
import java.util.List;

public class NeuronNetwork implements NeuronInterface {
    private static final long serialVersionUID = 1L;

    private final int id;

    /**
     * Input-Synapes for every Input of the Network.
     */
    private transient List<Synapse[]> inputSynapsesList; // Als transient markiert
    /**
     * Output-Synapes for every Input of the Network.
     */
    private transient List<List<Synapse>> outputSynapsesList;
    private boolean isOutputNeuron; // Flag für Output-Neuronen

    private final NeuronTypeInfoData neuronTypeInfoData;

    private final NeuralNetwork network;

    public NeuronNetwork(final int id, final NeuronTypeInfoData neuronTypeInfoData) {
        this.id = id;
        this.neuronTypeInfoData = neuronTypeInfoData;
        this.inputSynapsesList = new ArrayList<>();
        this.outputSynapsesList = new ArrayList<>();
        this.isOutputNeuron = false; // Standardmäßig kein Output-Neuron

        // Dass NN direkt verwenden, nicht als Kopie.
        this.network = new NeuralNetwork(neuronTypeInfoData.getNetwork(), false);
    }

    @Override
    public long activate(final NeuralNetwork neuralNetwork) {
        final double[] inputArr = new double[this.neuronTypeInfoData.getInputCount()];
        for (int inputTypePos = 0; inputTypePos < this.neuronTypeInfoData.getInputCount(); inputTypePos++) {
            final Synapse[] inputSynapseArr = this.inputSynapsesList.get(inputTypePos);
            for (int inputSynapsePos = 0; inputSynapsePos < inputSynapseArr.length; inputSynapsePos++) {
                final Synapse synapse = inputSynapseArr[inputSynapsePos];
                int sourceOutputTypePos = synapse.getSourceOutputTypePos();
                inputArr[inputTypePos] += neuralNetwork.getNeuronValueFunction().readValue(neuralNetwork, synapse.getSourceNeuron(), sourceOutputTypePos) * synapse.getWeight();
            }
        }
        final int outputTypePos = 0;
        neuralNetwork.setInputs(inputArr);
        neuralNetwork.process();
        return neuralNetwork.getProccessedSynapses();
    }

    @Override
    public double getBias(final int outputTypePos) {
        final Neuron neuron = this.network.getOutputNeuronArr()[outputTypePos];
        final int neuronOutputTypePos = 0; // Default-Output-Type für Output-Neuronen.
        return neuron.getBias(neuronOutputTypePos);
    }

    @Override
    public void setBias(final int outputTypePos, final double bias) {
        final Neuron neuron = this.network.getOutputNeuronArr()[outputTypePos];
        final int neuronOutputTypePos = 0; // Default-Output-Type für Output-Neuronen.
        neuron.setBias(neuronOutputTypePos, bias);
    }
}
