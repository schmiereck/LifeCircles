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
    private transient Synapse[] inputSynapses; // Als transient markiert
    private int inputSynapseCount;   // Aktuelle Anzahl von Synapsen im Array
    private transient List<Synapse> outputSynapses;
    private ActivationFunction activationFunction;
    private boolean isOutputNeuron; // Flag für Output-Neuronen
    
    private static final int INITIAL_SYNAPSE_CAPACITY = 8;

    public Neuron() {
        this.value = 0.0;
        this.bias = Math.random() * 2 - 1; // Random bias between -1 and 1
        this.inputSynapses = new Synapse[INITIAL_SYNAPSE_CAPACITY];
        this.inputSynapseCount = 0;
        this.outputSynapses = new ArrayList<>();
        this.activationFunction = ActivationFunction.Sigmoid;
        this.isOutputNeuron = false; // Standardmäßig kein Output-Neuron
    }

    public void addInputSynapse(Synapse synapse) {
        // Array vergrößern, falls erforderlich
        if (this.inputSynapseCount >= this.inputSynapses.length) {
            Synapse[] newInputSynapses = new Synapse[this.inputSynapses.length * 2];
            System.arraycopy(this.inputSynapses, 0, newInputSynapses, 0, this.inputSynapses.length);
            this.inputSynapses = newInputSynapses;
        }

        this.inputSynapses[inputSynapseCount++] = synapse;
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
        return this.inputSynapseCount;
    }

    public List<Synapse> getOutputSynapses() {
        return this.outputSynapses;
    }

    /**
     * Entfernt eine Input-Synapse.
     * Diese Operation ist langsamer als Zugriffe.
     */
    public void removeInputSynapse(Synapse synapse) {
        for (int i = 0; i < this.inputSynapseCount; i++) {
            if (this.inputSynapses[i] == synapse) {
                // Verschiebe alle Elemente nach dem zu löschenden um eine Position nach vorne
                if (i < this.inputSynapseCount - 1) {
                    System.arraycopy(inputSynapses, i + 1, inputSynapses, i, inputSynapseCount - i - 1);
                }
                this.inputSynapseCount--;
                this.inputSynapses[inputSynapseCount] = null; // Verhindere memory leak
                return;
            }
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
        for (int i = 0; i < this.inputSynapseCount; i++) {
            Synapse synapse = this.inputSynapses[i];
            sum += synapse.getSourceNeuron().getValue() * synapse.getWeight();
        }
        
         this.value = this.activationFunction.apply(sum);

        return this.inputSynapseCount;
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
        this.inputSynapses = new Synapse[INITIAL_SYNAPSE_CAPACITY];
        this.inputSynapseCount = 0;
        this.outputSynapses = new ArrayList<>(); // Initialisiere die Liste der Output-Synapsen
        // Stelle die Output-Synapsen wieder her
        int outputSynapseCount = ois.readInt();
        for (int i = 0; i < outputSynapseCount; i++) {
            Synapse synapse = (Synapse) ois.readObject();
            outputSynapses.add(synapse);
        }
    }
}
