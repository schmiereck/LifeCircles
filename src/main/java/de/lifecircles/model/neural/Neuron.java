package de.lifecircles.model.neural;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a neuron in the neural network.
 * Uses a sigmoid activation function.
 */
public class Neuron implements NeuronInterface {
    private static final long serialVersionUID = 1L;

    private final int id;
    private double value;
    private double bias;
    private transient Synapse[] inputSynapseArr; // Als transient markiert
    private transient List<Synapse> outputSynapseList;
    private ActivationFunction activationFunction;
    private boolean isOutputNeuron; // Flag für Output-Neuronen
    
    // Für Backpropagation benötigte Felder
    private transient double inputSum; // Eingangssumme vor Aktivierung
    private transient double delta; // Delta für Backpropagation

    private final NeuronTypeInfoData neuronTypeInfoData;

    public Neuron(final int id, final NeuronTypeInfoData neuronTypeInfoData) {
        this.id = id;
        this.neuronTypeInfoData = neuronTypeInfoData;
        this.value = 0.0;
        this.bias = Math.random() * 2 - 1; // Random bias between -1 and 1
        this.inputSynapseArr = new Synapse[0];
        this.outputSynapseList = new ArrayList<>();
        this.activationFunction = ActivationFunction.Sigmoid;
        this.isOutputNeuron = false; // Standardmäßig kein Output-Neuron
        this.inputSum = 0.0;
        this.delta = 0.0;
    }

    public void addInputSynapse(final int inputTypePos, final Synapse synapse) {
        // Array vergrößern, falls erforderlich
        Synapse[] newInputSynapses = new Synapse[this.inputSynapseArr.length + 1];
        System.arraycopy(this.inputSynapseArr, 0, newInputSynapses, 0, this.inputSynapseArr.length);
        newInputSynapses[this.inputSynapseArr.length] = synapse;
        this.inputSynapseArr = newInputSynapses;
    }

    public void addOutputSynapse(final int outputTypePos, final Synapse synapse) {
        this.outputSynapseList.add(synapse);
    }

    public double getValue(final int outputTypePos) {
        return this.value;
    }

    public void setValue(final int outputTypePos, final double value) {
        this.value = value;
    }

    public double getBias(final int outputTypePos) {
        return this.bias;
    }

    public void setBias(final int outputTypePos, final double bias) {
        this.bias = bias;
    }

    /**
     * Gibt ein Array mit allen Input-Synapsen zurück.
     */
    public Synapse[] getInputSynapseArr(final int inputTypePos) {
        return this.inputSynapseArr;
    }
    
    /**
     * Gibt die Anzahl der tatsächlich vorhandenen Input-Synapsen zurück.
     */
    public int getInputSynapseCount(final int inputTypePos) {
        return this.inputSynapseArr.length;
    }

    public List<Synapse> getOutputSynapseList(final int outputTypePos) {
        return this.outputSynapseList;
    }

    /**
     * Entfernt eine Input-Synapse.
     * Diese Operation ist langsamer als Zugriffe.
     */
    public void removeInputSynapse(final int inputTypePos, final Synapse synapse) {
        int foundInputSynapsePos = -1;
        for (int inputSynapsePos = 0; inputSynapsePos < this.inputSynapseArr.length; inputSynapsePos++) {
            if (this.inputSynapseArr[inputSynapsePos] == synapse) {
                foundInputSynapsePos = inputSynapsePos;
                break;
            }
        }
        if (foundInputSynapsePos != -1) {
            Synapse[] newInputSynapses = new Synapse[this.inputSynapseArr.length - 1];
            System.arraycopy(this.inputSynapseArr, 0, newInputSynapses, 0, foundInputSynapsePos);
            System.arraycopy(this.inputSynapseArr, foundInputSynapsePos + 1, newInputSynapses, foundInputSynapsePos, this.inputSynapseArr.length - foundInputSynapsePos - 1);
            this.inputSynapseArr = newInputSynapses;
        }
    }

    public void removeOutputSynapse(final int outputTypePos, final Synapse synapse) {
        this.getOutputSynapseList(outputTypePos).remove(synapse);
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
    public double getInputSum(final int outputTypePos) {
        return this.inputSum;
    }

    /**
     * Setzt den Eingangswert der Eingangssumme vor Aktivierung.
     */
    public void setInputSum(final int outputTypePos, final double inputSum) {
        this.inputSum = inputSum;
    }

    /**
     * Setzt den Delta-Wert für Backpropagation.
     * @param delta Der Delta-Wert
     */
    public void setDelta(final int outputTypePos, double delta) {
        this.delta = delta;
    }

    /**
     * Gibt den Delta-Wert für Backpropagation zurück
     * @return Der Delta-Wert
     */
    public double getDelta(final int outputTypePos) {
        return this.delta;
    }

    /**
     * Benutzerdefinierte Serialisierungsmethode.
     */
    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        // Speichere die Anzahl der Output-Synapsen
        oos.writeInt(outputSynapseList.size());
        for (Synapse synapse : outputSynapseList) {
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
        this.inputSynapseArr = new Synapse[0];
        this.outputSynapseList = new ArrayList<>(); // Initialisiere die Liste der Output-Synapsen
        this.inputSum = 0.0;
        this.delta = 0.0;
        // Stelle die Output-Synapsen wieder her
        int outputSynapseCount = ois.readInt();
        for (int i = 0; i < outputSynapseCount; i++) {
            Synapse synapse = (Synapse) ois.readObject();
            this.outputSynapseList.add(synapse);
        }
    }

    public void setInputSynapseArr(final int inputTypePos, Synapse[] inputSynapses) {
        this.inputSynapseArr = inputSynapses;
    }

    public int getId() {
        return this.id;
    }

    public NeuronTypeInfoData getNeuronTypeInfoData() {
        return this.neuronTypeInfoData;
    }

    public long activate(final NeuralNetwork neuralNetwork) {
        long synapseCount = 0L;
        final int outputTypePos = 0; // This Neuron supports only one output.
        for (int inputTypePos = 0; inputTypePos < this.getNeuronTypeInfoData().getInputCount(); inputTypePos++) {
            double sum = this.getBias(outputTypePos);
            // Direkte Array-Iteration für bessere Performance
            final Synapse[] inputSynapseArr = this.getInputSynapseArr(inputTypePos);
            for (int inputSynapsePos = 0; inputSynapsePos < inputSynapseArr.length; inputSynapsePos++) {
                final Synapse synapse = inputSynapseArr[inputSynapsePos];
                sum += neuralNetwork.getNeuronValueFunction().readValue(neuralNetwork, synapse.getSourceNeuron(), outputTypePos) * synapse.getWeight();
            }

            this.setInputSum(inputTypePos, sum); // Speichere die Summe vor Aktivierung für Backpropagation
            double value = this.getActivationFunction().apply(sum);
            //this.writeNeuronValue(neuron, outputTypePos, value);
            neuralNetwork.getNeuronValueFunction().writeValue(neuralNetwork, this, outputTypePos, value);

            synapseCount += inputSynapseArr.length;
        }
        return synapseCount;
    }
}
