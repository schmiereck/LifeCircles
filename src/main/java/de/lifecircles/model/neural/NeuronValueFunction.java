package de.lifecircles.model.neural;

import java.io.Serializable;

public interface NeuronValueFunction extends Serializable {
    double readValue(final NeuralNet neuralNet, final NeuronInterface neuron, final int outputTypePos);
    void writeValue(final NeuralNet neuralNet, final NeuronInterface neuron, int outputTypePos, final double value);

    int fetchNextFreeId(final NeuralNet neuralNet);

    void releaseNeuron(final NeuralNet neuralNet, final NeuronInterface neuron);
}
