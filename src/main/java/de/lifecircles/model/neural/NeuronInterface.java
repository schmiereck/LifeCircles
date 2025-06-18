package de.lifecircles.model.neural;

import java.io.Serializable;
import java.util.List;
import java.util.Random;

public interface NeuronInterface extends Serializable {

    NeuronTypeInfoData getNeuronTypeInfoData();

    double getBias(final int outputTypePos);
    void setBias(final int outputTypePos, final double bias);

    long activate(NeuronValueFunction neuronValueFunction, final NeuralNet neuralNet);

    NeuronInterface cloneNeuron(final NeuralNet neuralNet, final NeuronValueFunction neuronValueFunction, final boolean isActiveLayer);

    void addOutputSynapse(final int outputTypePos, final Synapse synapse);

    void addInputSynapse(final int inputTypePos, final Synapse synapse);

    void mutateNeuron(final Random random, double mutationRate, double mutationStrength);

    void removeOutputSynapse(final int outputTypePos, final Synapse synapse);
    void removeInputSynapse(final int inputTypePos, final Synapse synapse);

    void backpropagateDelta();

    double getDelta(final int outputTypePos);

    void setInputSynapseArr(final int inputTypePos, final Synapse[] array);

    double getValue(final int outputTypePos);

    void setValue(final int outputTypePos, final double value);

    int getId();

    List<Synapse> getOutputSynapseList(final int outputTypePos);

    Synapse[] getInputSynapseArr(final int outputTypePos);
}
