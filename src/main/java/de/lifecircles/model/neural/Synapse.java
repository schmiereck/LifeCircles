package de.lifecircles.model.neural;

/**
 * Represents a connection between two neurons in the neural network.
 */
public class Synapse {
    private final Neuron sourceNeuron;
    private final Neuron targetNeuron;
    private double weight;

    public Synapse(Neuron sourceNeuron, Neuron targetNeuron) {
        this.sourceNeuron = sourceNeuron;
        this.targetNeuron = targetNeuron;
        this.weight = Math.random() * 2 - 1; // Random weight between -1 and 1
        
        sourceNeuron.addOutputSynapse(this);
        targetNeuron.addInputSynapse(this);
    }

    public Neuron getSourceNeuron() {
        return sourceNeuron;
    }

    public Neuron getTargetNeuron() {
        return targetNeuron;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    /**
     * Creates a copy of this synapse between two new neurons.
     */
    public Synapse copy(Neuron newSource, Neuron newTarget) {
        Synapse copy = new Synapse(newSource, newTarget);
        copy.weight = this.weight;
        return copy;
    }
}
