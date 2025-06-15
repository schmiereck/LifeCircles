package de.lifecircles.model.neural;

public interface NeuronValueFunction {
    double readValue(final NeuralNetwork neuralNetwork, final Neuron neuron);
    void writeValue(final NeuralNetwork neuralNetwork, final Neuron neuron, final double value);

    int fetchNextFreeId(final NeuralNetwork neuralNetwork);

    void releaseNeuron(final NeuralNetwork neuralNetwork, final Neuron neuron);
}
