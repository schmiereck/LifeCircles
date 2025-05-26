package de.lifecircles.model.neural;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a layer in the neural network.
 * Contains a list of neurons and an active flag.
 */
public class Layer implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<Neuron> neurons;
    private boolean isActiveLayer;
    private transient double activationCounter = 0.0D;

    public Layer() {
        this.neurons = new ArrayList<>();
        this.isActiveLayer = true; // Default to active
    }

    public List<Neuron> getNeurons() {
        return this.neurons;
    }

    public boolean isActiveLayer() {
        return this.isActiveLayer;
    }

    public void setActiveLayer(boolean isActiveLayer) {
        this.isActiveLayer = isActiveLayer;
    }

    public void addNeuron(Neuron neuron) {
        this.neurons.add(neuron);
    }

    public double getActivationCounter() {
        return this.activationCounter;
    }

    public void setActivationCounter(double activationCounter) {
        this.activationCounter = activationCounter;
    }
}
