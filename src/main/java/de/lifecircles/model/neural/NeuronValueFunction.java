package de.lifecircles.model.neural;

import java.io.Serializable;

public interface NeuronValueFunction extends Serializable {
    double readValue(final NeuralNetwork neuralNetwork, final Neuron neuron, final int outputTypePos);
    void writeValue(final NeuralNetwork neuralNetwork, final Neuron neuron, int outputTypePos, final double value);

    int fetchNextFreeId(final NeuralNetwork neuralNetwork);

    void releaseNeuron(final NeuralNetwork neuralNetwork, final Neuron neuron);
}
