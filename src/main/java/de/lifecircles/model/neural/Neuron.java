package de.lifecircles.model.neural;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a neuron in the neural network.
 * Uses a sigmoid activation function.
 */
public class Neuron {
    private double value;
    private double bias;
    private final List<Synapse> inputSynapses;
    private final List<Synapse> outputSynapses;

    public Neuron() {
        this.value = 0.0;
        this.bias = Math.random() * 2 - 1; // Random bias between -1 and 1
        this.inputSynapses = new ArrayList<>();
        this.outputSynapses = new ArrayList<>();
    }

    public void addInputSynapse(Synapse synapse) {
        inputSynapses.add(synapse);
    }

    public void addOutputSynapse(Synapse synapse) {
        outputSynapses.add(synapse);
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public double getBias() {
        return bias;
    }

    public void setBias(double bias) {
        this.bias = bias;
    }

    public List<Synapse> getInputSynapses() {
        return inputSynapses;
    }

    public List<Synapse> getOutputSynapses() {
        return outputSynapses;
    }

    /**
     * Calculates the neuron's output value based on its inputs.
     */
    public void activate() {
        double sum = bias;
        for (Synapse synapse : inputSynapses) {
            sum += synapse.getSourceNeuron().getValue() * synapse.getWeight();
        }
        value = sigmoid(sum);
    }

    /**
     * Sigmoid activation function.
     */
    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    /**
     * Creates a copy of this neuron with the same bias.
     */
    public Neuron copy() {
        Neuron copy = new Neuron();
        copy.bias = this.bias;
        return copy;
    }
}
