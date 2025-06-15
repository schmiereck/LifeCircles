package de.lifecircles.model.neural;

public interface NeuronValueFunction {
    double readValue(final Neuron neuron);
    void writeValue(final Neuron neuron, final double value);

    int fetchNextFreeId();
}
