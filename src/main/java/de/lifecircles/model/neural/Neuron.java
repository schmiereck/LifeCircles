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
    
    private double value;
    private double bias;
    transient Synapse[] inputSynapses; // Als transient markiert
    private transient List<Synapse> outputSynapses;
    private ActivationFunction activationFunction;
    private boolean isOutputNeuron; // Flag für Output-Neuronen
    
    static final int INITIAL_SYNAPSE_CAPACITY = 8;

    public Neuron() {
        this.value = 0.0;
        this.bias = Math.random() * 2 - 1; // Random bias between -1 and 1
        this.inputSynapses = new Synapse[0];
        this.outputSynapses = new ArrayList<>();
        this.activationFunction = ActivationFunction.Sigmoid;
        this.isOutputNeuron = false; // Standardmäßig kein Output-Neuron
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

    /**
     * Calculates the neuron's output value based on its inputs.
     * Optimized version using array iteration instead of ArrayList.
     * For output neurons, no activation function is applied.
     */
    public long activate() {
        double sum = this.bias;
        // Direkte Array-Iteration für bessere Performance
        for (int i = 0; i < this.inputSynapses.length; i++) {
            Synapse synapse = this.inputSynapses[i];
            sum += synapse.getSourceNeuron().getValue() * synapse.getWeight();
        }
        
         this.value = this.activationFunction.apply(sum);

        return this.inputSynapses.length;
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

    /**
     * Creates a copy of this neuron with the same bias.
     */
    public Neuron copy() {
        Neuron copy = new Neuron();
        copy.bias = this.bias;
        copy.isOutputNeuron = this.isOutputNeuron; // Übertrage Output-Neuron-Status
        copy.activationFunction = this.activationFunction;
        return copy;
    }

    public ActivationFunction getActivationFunction() {
        return this.activationFunction;
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
        // Stelle die Output-Synapsen wieder her
        int outputSynapseCount = ois.readInt();
        for (int i = 0; i < outputSynapseCount; i++) {
            Synapse synapse = (Synapse) ois.readObject();
            outputSynapses.add(synapse);
        }
    }
}
