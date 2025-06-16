package de.lifecircles.model.neural;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
    private transient List<Synapse[]> outputSynapsesList;
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
    public NeuronTypeInfoData getNeuronTypeInfoData() {
        return this.neuronTypeInfoData;
    }

    @Override
    public NeuronInterface cloneNeuron(final NeuralNetwork neuralNetwork, final boolean isActiveLayer) {
        final int newId = neuralNetwork.getNeuronValueFunction().fetchNextFreeId(neuralNetwork);
        NeuronNetwork newNeuron = new NeuronNetwork(newId, this.neuronTypeInfoData);

        return newNeuron;
    }

    @Override
    public void addOutputSynapse(final int outputTypePos, final Synapse synapse) {
        final Synapse[] outputSynapseArr = this.outputSynapsesList.get(outputTypePos);

        Synapse[] newOutputSynapses = new Synapse[outputSynapseArr.length + 1];
        System.arraycopy(outputSynapseArr, 0, newOutputSynapses, 0, outputSynapseArr.length);
        newOutputSynapses[outputSynapseArr.length] = synapse;

        this.outputSynapsesList.set(outputTypePos, newOutputSynapses);
    }

    @Override
    public void addInputSynapse(final int inputTypePos, final Synapse synapse) {
        final Synapse[] inputSynapseArr = this.inputSynapsesList.get(inputTypePos);

        Synapse[] newInputSynapses = new Synapse[inputSynapseArr.length + 1];
        System.arraycopy(inputSynapseArr, 0, newInputSynapses, 0, inputSynapseArr.length);
        newInputSynapses[inputSynapseArr.length] = synapse;

        this.inputSynapsesList.set(inputTypePos, newInputSynapses);
    }

    @Override
    public void removeInputSynapse(final int inputTypePos, final Synapse synapse) {
        final Synapse[] inputSynapseArr = this.inputSynapsesList.get(inputTypePos);

        int foundInputSynapsePos = -1;
        for (int inputSynapsePos = 0; inputSynapsePos < inputSynapseArr.length; inputSynapsePos++) {
            if (inputSynapseArr[inputSynapsePos] == synapse) {
                foundInputSynapsePos = inputSynapsePos;
                break;
            }
        }
        if (foundInputSynapsePos != -1) {
            Synapse[] newInputSynapses = new Synapse[inputSynapseArr.length - 1];
            System.arraycopy(inputSynapseArr, 0, newInputSynapses, 0, foundInputSynapsePos);
            System.arraycopy(inputSynapseArr, foundInputSynapsePos + 1, newInputSynapses, foundInputSynapsePos, inputSynapseArr.length - foundInputSynapsePos - 1);

            this.inputSynapsesList.set(inputTypePos, newInputSynapses);
        }
    }

    @Override
    public void removeOutputSynapse(final int outputTypePos, final Synapse synapse) {
        final Synapse[] outputSynapseArr = this.outputSynapsesList.get(outputTypePos);

        int foundOutputSynapsePos = -1;
        for (int outputSynapsePos = 0; outputSynapsePos < outputSynapseArr.length; outputSynapsePos++) {
            if (outputSynapseArr[outputSynapsePos] == synapse) {
                foundOutputSynapsePos = outputSynapsePos;
                break;
            }
        }
        if (foundOutputSynapsePos != -1) {
            Synapse[] newOutputSynapses = new Synapse[outputSynapseArr.length - 1];
            System.arraycopy(outputSynapseArr, 0, newOutputSynapses, 0, foundOutputSynapsePos);
            System.arraycopy(outputSynapseArr, foundOutputSynapsePos + 1, newOutputSynapses, foundOutputSynapsePos, outputSynapseArr.length - foundOutputSynapsePos - 1);

            this.outputSynapsesList.set(outputTypePos, newOutputSynapses);
        }
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

    @Override
    public void mutateNeuron(final Random random) {
        // Anpassung der Bias-Werte.
        for (int outputTypePos = 0; outputTypePos < this.neuronTypeInfoData.getOutputCount(); outputTypePos++) {
            double mutationFactor = random.nextDouble() * 0.1 - 0.05; // Zufällige Mutation zwischen -0.05 und +0.05
            double newBias = this.getBias(outputTypePos) + mutationFactor;
            this.setBias(outputTypePos, newBias);
        }
    }

    @Override
    public double getDelta(final int outputTypePos) {
        return this.deltaArr[outputTypePos];
    }

    public void setDelta(final int outputTypePos, final double delta) {
        this.deltaArr[outputTypePos] = delta;
    }

    @Override
    public void backpropagateDelta() {
        for (int outputTypePos = 0; outputTypePos < this.getNeuronTypeInfoData().getOutputCount(); outputTypePos++) {
            double errorSum = 0.0;

            // Sammle Fehler von allen ausgehenden Verbindungen
            for (final Synapse synapse : this.getOutputSynapseList(outputTypePos)) {
                final NeuronInterface targetNeuron = synapse.getTargetNeuron();
                errorSum += targetNeuron.getDelta(outputTypePos) * synapse.getWeight();
            }

            // Berechne Delta für dieses Neuron
            final double delta = errorSum * this.getActivationFunction().derivative(this.getInputSum(outputTypePos));
            this.setDelta(outputTypePos, delta);
        }
    }

    private Synapse[] getOutputSynapseList(final int outputTypePos) {
        return null;
    }
}
