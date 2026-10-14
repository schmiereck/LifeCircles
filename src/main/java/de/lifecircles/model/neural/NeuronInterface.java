package de.lifecircles.model.neural;

import java.io.Serializable;
import java.util.List;
import java.util.Random;

public interface NeuronInterface extends Serializable {

    NeuronTypeInfoData getNeuronTypeInfoData();

    double getBias(final int inputTypePos);
    void setBias(final int inputTypePos, final double bias);

    long activate(NeuronValueFunction neuronValueFunction, final NeuralNet neuralNet);

    NeuronInterface cloneNeuron(final NeuralNet neuralNet, final NeuronValueFunction neuronValueFunction, final boolean isActiveLayer);

    void addOutputSynapse(final int outputTypePos, final Synapse synapse);

    void addInputSynapse(final int inputTypePos, final Synapse synapse);

    void mutateNeuron(final Random random, double mutationRate, double mutationStrength);

    void removeOutputSynapse(final int outputTypePos, final Synapse synapse);
    void removeInputSynapse(final int inputTypePos, final Synapse synapse);

    void backpropagateDelta(final NeuralNet neuralNet, final NeuronValueFunction neuronValueFunction);

    /** Derivative of the loss with respect to the weighted sum at one input. */
    default double getInputDelta(final NeuralNet neuralNet, final NeuronValueFunction values, final int inputTypePos) {
        return values.readDelta(neuralNet, this, inputTypePos);
    }

    void setInputSynapseArr(final int inputTypePos, final Synapse[] array);

    int getId();

    List<Synapse> getOutputSynapseList(final int outputTypePos);

    Synapse[] getInputSynapseArr(final int outputTypePos);

    void backpropagateExtra(final double learningRate);
}
