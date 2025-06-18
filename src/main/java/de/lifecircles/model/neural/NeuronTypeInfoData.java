package de.lifecircles.model.neural;

import java.io.Serializable;

public class NeuronTypeInfoData implements Serializable {
    private int inputCount;
    private int outputCount;
    private NeuralNet neuralNet;

    public NeuronTypeInfoData(final int inputCount, final int outputCount, final NeuralNet neuralNet) {
        this.inputCount = inputCount;
        this.outputCount = outputCount;
        this.neuralNet = neuralNet;
    }

    public int getInputCount() {
        return this.inputCount;
    }

    public int getOutputCount() {
        return this.outputCount;
    }

    public NeuralNet getNeuralNet() {
        return this.neuralNet;
    }
}
