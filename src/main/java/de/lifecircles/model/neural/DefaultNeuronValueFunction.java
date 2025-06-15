package de.lifecircles.model.neural;

public class DefaultNeuronValueFunction implements NeuronValueFunction {
    private int lastFreeID = 0;

    @Override
    public double readValue(NeuralNetwork neuralNetwork, final Neuron neuron) {
        return neuron.getValue();
    }

    @Override
    public void writeValue(NeuralNetwork neuralNetwork, final Neuron neuron, double value) {
        neuron.setValue(value);
    }

    @Override
    public int fetchNextFreeId(NeuralNetwork neuralNetwork) {
        return this.lastFreeID++;
    }
}
