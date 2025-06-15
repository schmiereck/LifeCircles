package de.lifecircles.model.neural;

import java.io.*;

/**
 * Represents a connection between two neurons in the neural network.
 */
public class Synapse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final Neuron sourceNeuron;
    private final int sourceOutputTypePos;
    private final Neuron targetNeuron;
    private final int targetInputTypePos;
    private double weight;

    public Synapse(final Neuron sourceNeuron, final int sourceOutputTypePos,
                   final Neuron targetNeuron, final int targetInputTypePos) {
        this(sourceNeuron, sourceOutputTypePos,
                targetNeuron, targetInputTypePos,
                Math.random() * 2.0D - 1.0D); // Random weight between -1 and 1
    }
    public Synapse(final Neuron sourceNeuron, final int sourceOutputTypePo,
                   final Neuron targetNeuron, final int targetInputTypePos,
                   final double weight) {
        this.sourceNeuron = sourceNeuron;
        this.sourceOutputTypePos = sourceOutputTypePo;
        this.targetNeuron = targetNeuron;
        this.targetInputTypePos = targetInputTypePos;
        this.weight = weight;
        
        sourceNeuron.addOutputSynapse(sourceOutputTypePo, this);
        targetNeuron.addInputSynapse(targetInputTypePos, this);

        //System.out.println("Synapse created: " + sourceNeuron + " -> " + targetNeuron);
    }

    public Neuron getSourceNeuron() {
        return sourceNeuron;
    }

    public Neuron getTargetNeuron() {
        return targetNeuron;
    }

    public double getWeight() {
        return this.weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    /**
     * Benutzerdefinierte Serialisierungsmethode.
     * Da die Neuronen-Referenzen bei der Serialisierung zyklische Abhängigkeiten verursachen können,
     * wird hier die Standard-Serialisierung verwendet. Die Verbindungen werden nach dem
     * Deserialisieren durch NeuralNetwork wiederhergestellt.
     */
    @Serial
    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
    }
    
    /**
     * Benutzerdefinierte Deserialisierungsmethode.
     * Die Verbindungen zu den Neuronen werden nach dem Deserialisieren durch diese Methode wiederhergestellt.
     */
    @Serial
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        // Stelle die Verbindungen wieder her
        restoreConnections();
    }

    /**
     * Methode zur Wiederherstellung der bidirektionalen Referenzen nach der Deserialisierung.
     */
    public void restoreConnections() {
        if (this.sourceNeuron != null) {
            this.sourceNeuron.addOutputSynapse(this.sourceOutputTypePos,this);
        }
        if (this.targetNeuron != null) {
            this.targetNeuron.addInputSynapse(this.targetInputTypePos, this);
        }
    }
}

