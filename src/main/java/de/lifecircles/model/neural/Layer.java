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
    private boolean isActiveLayer;
    private transient double activationCounter = 0.0D;
    private static final int INITIAL_CAPACITY = 8;

    public Layer() {
        this.neurons = new Neuron[0];
        this.isActiveLayer = true; // Default to active
    }

    /**
     * Gibt die Neuronen als Array zurück (Performance-Variante).
     * Gibt das interne Array zurück.
     * ACHTUNG: Änderungen am Array wirken sich direkt auf das Layer aus!
     */
    public Neuron[] getNeuronsArray() {
        return neurons;
    }

    /**
     * Gibt die Neuronen als List<Neuron> zurück (Kompatibilität).
     */
    public List<Neuron> getNeurons() {
        List<Neuron> list = new ArrayList<>(neurons.length);
        for (Neuron neuron : neurons) {
            list.add(neuron);
        }
        return list;
    }

    public int size() {
        return neurons.length;
    }

    public boolean isActiveLayer() {
        return this.isActiveLayer;
    }

    public void setActiveLayer(boolean isActiveLayer) {
        this.isActiveLayer = isActiveLayer;
    }

    public void addNeuron(Neuron neuron) {
        Neuron[] newArr = new Neuron[neurons.length + 1];
        System.arraycopy(neurons, 0, newArr, 0, neurons.length);
        newArr[neurons.length] = neuron;
        neurons = newArr;
    }

    public void removeNeuron(int idx) {
        if (idx < 0 || idx >= neurons.length) throw new IndexOutOfBoundsException();
        Neuron[] newArr = new Neuron[neurons.length - 1];
        System.arraycopy(neurons, 0, newArr, 0, idx);
        System.arraycopy(neurons, idx + 1, newArr, idx, neurons.length - idx - 1);
        neurons = newArr;
    }

    public Neuron getNeuron(int idx) {
        if (idx < 0 || idx >= neurons.length) throw new IndexOutOfBoundsException();
        return neurons[idx];
    }

    public double getActivationCounter() {
        return this.activationCounter;
    }

    public void setActivationCounter(double activationCounter) {
        this.activationCounter = activationCounter;
    }
}
