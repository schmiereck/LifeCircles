package de.lifecircles.model.neural;

public class NeuronTypeInfoData {
    private int inputCount;
    private int outputCount;
    private NeuralNetwork network;

    public NeuronTypeInfoData(final int inputCount, final int outputCount, final NeuralNetwork network) {
        this.inputCount = inputCount;
        this.outputCount = outputCount;
        this.network = network;
    }

    public int getInputCount() {
        return this.inputCount;
    }

    public int getOutputCount() {
        return this.outputCount;
    }

    public NeuralNetwork getNetwork() {
        return this.network;
    }
}
