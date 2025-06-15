package de.lifecircles.model.neural;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a neuron in the neural network.
 * Uses a sigmoid activation function.
 */
public class Neuron implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private double value;
    private double bias;
    private transient Synapse[] inputSynapses; // Als transient markiert
    private transient List<Synapse> outputSynapses;
    private ActivationFunction activationFunction;
    private boolean isOutputNeuron; // Flag für Output-Neuronen
    
    // Für Backpropagation benötigte Felder
    private transient double inputSum; // Eingangssumme vor Aktivierung
    private transient double delta; // Delta für Backpropagation

    public Neuron(final int id) {
        this.id = id;
        this.value = 0.0;
        this.bias = Math.random() * 2 - 1; // Random bias between -1 and 1
        this.inputSynapses = new Synapse[0];
        this.outputSynapses = new ArrayList<>();
        this.activationFunction = ActivationFunction.Sigmoid;
        this.isOutputNeuron = false; // Standardmäßig kein Output-Neuron
        this.inputSum = 0.0;
        this.delta = 0.0;
    }

    public void addInputSynapse(Synapse synapse) {
        // Array vergrößern, falls erforderlich
        Synapse[] newInputSynapses = new Synapse[this.inputSynapses.length + 1];
        System.arraycopy(this.inputSynapses, 0, newInputSynapses, 0, this.inputSynapses.length);
        newInputSynapses[this.inputSynapses.length] = synapse;
        this.inputSynapses = newInputSynapses;
    }

    public void addOutputSynapse(Synapse synapse) {
        this.outputSynapses.add(synapse);
    }

    public double getValue() {
        return this.value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public double getBias() {
        return this.bias;
    }

    public void setBias(double bias) {
        this.bias = bias;
    }

    /**
     * Gibt ein Array mit allen Input-Synapsen zurück.
     * Hinweis: Array kann größer sein als die tatsächliche Anzahl der Synapsen.
     * Verwende inputSynapseCount für die tatsächliche Anzahl.
     */
    public Synapse[] getInputSynapses() {
        return this.inputSynapses;
    }
    
    /**
     * Gibt die Anzahl der tatsächlich vorhandenen Input-Synapsen zurück.
     */
    public int getInputSynapseCount() {
        return this.inputSynapses.length;
    }

    public List<Synapse> getOutputSynapses() {
        return this.outputSynapses;
    }

    /**
     * Entfernt eine Input-Synapse.
     * Diese Operation ist langsamer als Zugriffe.
     */
    public void removeInputSynapse(Synapse synapse) {
        int idx = -1;
        for (int i = 0; i < this.inputSynapses.length; i++) {
            if (this.inputSynapses[i] == synapse) {
                idx = i;
                break;
            }
        }
        if (idx != -1) {
            Synapse[] newInputSynapses = new Synapse[this.inputSynapses.length - 1];
            System.arraycopy(this.inputSynapses, 0, newInputSynapses, 0, idx);
            System.arraycopy(this.inputSynapses, idx + 1, newInputSynapses, idx, this.inputSynapses.length - idx - 1);
            this.inputSynapses = newInputSynapses;
        }
    }

    public void setActivationFunction(ActivationFunction activationFunction) {
        this.activationFunction = activationFunction;
    }

    /**
     * Sets whether this neuron is an output neuron.
     * Output neurons will not apply activation functions.
     * 
     * @param isOutputNeuron true if this neuron is an output neuron
     */
    public void setOutputNeuron(boolean isOutputNeuron) {
        this.isOutputNeuron = isOutputNeuron;
    }
    
    /**
     * Checks if this neuron is an output neuron.
     * 
     * @return true if this neuron is an output neuron
     */
    public boolean isOutputNeuron() {
        return this.isOutputNeuron;
    }

    public ActivationFunction getActivationFunction() {
        return this.activationFunction;
    }

    /**
     * Gibt den Eingangswert zurück (vor Anwendung der Aktivierungsfunktion)
     * @return Die Eingangssumme vor Aktivierung
     */
    public double getInputSum() {
        return this.inputSum;
    }

    /**
     * Setzt den Eingangswert der Eingangssumme vor Aktivierung.
     */
    public void setInputSum(final double inputSum) {
        this.inputSum = inputSum;
    }

    /**
     * Setzt den Delta-Wert für Backpropagation
     * @param delta Der Delta-Wert
     */
    public void setDelta(double delta) {
        this.delta = delta;
    }

    /**
     * Gibt den Delta-Wert für Backpropagation zurück
     * @return Der Delta-Wert
     */
    public double getDelta() {
        return this.delta;
    }

    /**
     * Benutzerdefinierte Serialisierungsmethode.
     */
    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        // Speichere die Anzahl der Output-Synapsen
        oos.writeInt(outputSynapses.size());
        for (Synapse synapse : outputSynapses) {
            oos.writeObject(synapse);
        }
    }

    /**
     * Benutzerdefinierte Deserialisierungsmethode.
     */
    @Serial
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        // Initialisiere die transienten Felder
        this.inputSynapses = new Synapse[0];
        this.outputSynapses = new ArrayList<>(); // Initialisiere die Liste der Output-Synapsen
        this.inputSum = 0.0;
        this.delta = 0.0;
        // Stelle die Output-Synapsen wieder her
        int outputSynapseCount = ois.readInt();
        for (int i = 0; i < outputSynapseCount; i++) {
            Synapse synapse = (Synapse) ois.readObject();
            this.outputSynapses.add(synapse);
        }
    }

    public void setInputSynapses(Synapse[] inputSynapses) {
        this.inputSynapses = inputSynapses;
    }

    public int getId() {
        return this.id;
    }
}
