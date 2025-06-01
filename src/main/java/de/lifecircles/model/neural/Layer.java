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

    private Neuron[] neurons;
    private int neuronCount;
    private boolean isActiveLayer;
    private transient double activationCounter = 0.0D;
    private static final int INITIAL_CAPACITY = 8;

    public Layer() {
        this.neurons = new Neuron[INITIAL_CAPACITY];
        this.neuronCount = 0;
        this.isActiveLayer = true; // Default to active
    }

    /**
     * Gibt die Neuronen als Array zurück (Performance-Variante).
     */
    public Neuron[] getNeuronsArray() {
        if (neuronCount == neurons.length) return neurons;
        Neuron[] arr = new Neuron[neuronCount];
        System.arraycopy(neurons, 0, arr, 0, neuronCount);
        return arr;
    }

    /**
     * Gibt die Neuronen als List<Neuron> zurück (Kompatibilität).
     */
    public List<Neuron> getNeurons() {
        List<Neuron> list = new ArrayList<>(neuronCount);
        for (int i = 0; i < neuronCount; i++) {
            list.add(neurons[i]);
        }
        return list;
    }

    public int size() {
        return neuronCount;
    }

    public boolean isActiveLayer() {
        return this.isActiveLayer;
    }

    public void setActiveLayer(boolean isActiveLayer) {
        this.isActiveLayer = isActiveLayer;
    }

    public void addNeuron(Neuron neuron) {
        if (neuronCount >= neurons.length) {
            Neuron[] newArr = new Neuron[neurons.length * 2];
            System.arraycopy(neurons, 0, newArr, 0, neurons.length);
            neurons = newArr;
        }
        neurons[neuronCount++] = neuron;
    }

    public Neuron getNeuron(int idx) {
        if (idx < 0 || idx >= neuronCount) throw new IndexOutOfBoundsException();
        return neurons[idx];
    }

    public double getActivationCounter() {
        return this.activationCounter;
    }

    public void setActivationCounter(double activationCounter) {
        this.activationCounter = activationCounter;
    }
}
