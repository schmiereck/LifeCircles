package de.lifecircles.model.neural;

public class DefaultNeuronValueFunction implements NeuronValueFunction {
    private int lastFreeID = 0;

    @Override
    public double readValue(NeuralNet neuralNet, final NeuronInterface neuron, int outputTypePos) {
        return neuron.getValue(outputTypePos);
    }

    @Override
    public void writeValue(NeuralNet neuralNet, final NeuronInterface neuron, int outputTypePos, double value) {
        neuron.setValue(outputTypePos, value);
    }

    @Override
    public double readDelta(final NeuralNet neuralNet, final NeuronInterface neuron, final int outputTypePos) {
        return neuron.getDelta(outputTypePos);
    }

    @Override
    public void writeDelta(final NeuralNet neuralNet, final NeuronInterface neuron, final int outputTypePos, final double delta) {
        neuron.setDelta(outputTypePos, delta);
    }

    @Override
    public double readInputSum(final NeuralNet neuralNet, final NeuronInterface neuron, final int outputTypePos) {
        return neuron.getInputSum(outputTypePos);
    }

    @Override
    public void writeInputSum(final NeuralNet neuralNet, final NeuronInterface neuron, final int outputTypePos, final double inputSum) {
        neuron.setInputSum(outputTypePos, inputSum);
    }

    @Override
    public int fetchNextFreeId(NeuralNet neuralNet) {
        return this.lastFreeID++;
    }

    @Override
    public void releaseNeuron(NeuralNet neuralNet, NeuronInterface neuron) {
        // nothing to do here in this implementation
    }
}
