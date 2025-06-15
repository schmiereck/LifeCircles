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
}
