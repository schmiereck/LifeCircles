package de.lifecircles.model.neural;

import java.io.Serializable;
import java.util.Random;

public interface NeuronInterface extends Serializable {

    NeuronTypeInfoData getNeuronTypeInfoData();

    double getBias(final int outputTypePos);
    void setBias(final int outputTypePos, final double bias);

    long activate(final NeuralNetwork neuralNetwork);

    NeuronInterface cloneNeuron(NeuralNetwork neuralNetwork, final boolean isActiveLayer);

    void addOutputSynapse(final int outputTypePos, final Synapse synapse);

    void addInputSynapse(final int inputTypePos, final Synapse synapse);

    void mutateNeuron(final Random random);

    void removeOutputSynapse(final int outputTypePos, final Synapse synapse);
    void removeInputSynapse(final int inputTypePos, final Synapse synapse);

    void backpropagateDelta();

    double getDelta(final int outputTypePos);
}
