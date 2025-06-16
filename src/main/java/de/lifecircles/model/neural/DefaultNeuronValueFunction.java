package de.lifecircles.model.neural;

public class DefaultNeuronValueFunction implements NeuronValueFunction {
    private int lastFreeID = 0;

    @Override
    public double readValue(NeuralNetwork neuralNetwork, final Neuron neuron, int outputTypePos) {
        return neuron.getValue(outputTypePos);
    }

    @Override
    public void writeValue(NeuralNetwork neuralNetwork, final Neuron neuron, int outputTypePos, double value) {
        neuron.setValue(outputTypePos, value);
    }

    @Override
    public int fetchNextFreeId(NeuralNetwork neuralNetwork) {
        return this.lastFreeID++;
    }

    @Override
    public void releaseNeuron(NeuralNetwork neuralNetwork, Neuron neuron) {
        // nothing to do here in this implementation
    }
}
