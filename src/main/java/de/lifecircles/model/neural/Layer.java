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

    private NeuronInterface[] neuronArr;
    private boolean isActiveLayer;
    private transient double activationCounter = 0.0D;

    public Layer() {
        this.neuronArr = new Neuron[0];
        this.isActiveLayer = true; // Default to active
    }

    /**
     * Gibt die Neuronen als Array zurück (Performance-Variante).
     * Gibt das interne Array zurück.
     * ACHTUNG: Änderungen am Array wirken sich direkt auf das Layer aus!
     */
    public NeuronInterface[] getNeuronsArr() {
        return this.neuronArr;
    }

    /**
     * Gibt die Neuronen als List<Neuron> zurück (Kompatibilität).
     */
    public List<NeuronInterface> getNeuronList() {
        List<NeuronInterface> list = new ArrayList<>(this.neuronArr.length);
        for (NeuronInterface neuron : this.neuronArr) {
            list.add(neuron);
        }
        return list;
    }

    public int size() {
        return neuronArr.length;
    }

    public boolean isActiveLayer() {
        return this.isActiveLayer;
    }

    public void setActiveLayer(boolean isActiveLayer) {
        this.isActiveLayer = isActiveLayer;
    }

    public void addNeuron(final NeuronInterface neuron) {
        final NeuronInterface[] newArr = new NeuronInterface[this.neuronArr.length + 1];
        System.arraycopy(this.neuronArr, 0, newArr, 0, this.neuronArr.length);
        newArr[this.neuronArr.length] = neuron;
        this.neuronArr = newArr;
    }

    public void removeNeuron(final int idx) {
        if (idx < 0 || idx >= this.neuronArr.length) throw new IndexOutOfBoundsException();
        final NeuronInterface[] newArr = new NeuronInterface[this.neuronArr.length - 1];
        System.arraycopy(this.neuronArr, 0, newArr, 0, idx);
        System.arraycopy(this.neuronArr, idx + 1, newArr, idx, neuronArr.length - idx - 1);
        neuronArr = newArr;
    }

    public NeuronInterface getNeuron(final int idx) {
        if (idx < 0 || idx >= this.neuronArr.length) throw new IndexOutOfBoundsException();
        return this.neuronArr[idx];
    }

    public double getActivationCounter() {
        return this.activationCounter;
    }

    public void setActivationCounter(double activationCounter) {
        this.activationCounter = activationCounter;
    }
}
