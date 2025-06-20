package de.lifecircles.model.neural;

import java.io.*;

/**
 * Represents a connection between two neurons in the neural network.
 */
public class Synapse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final NeuronInterface sourceNeuron;
    private final int sourceOutputTypePos;
    private final NeuronInterface targetNeuron;
    private final int targetInputTypePos;
    private double weight;

    public Synapse(final Neuron sourceNeuron, final int sourceOutputTypePos,
                   final Neuron targetNeuron, final int targetInputTypePos) {
        this(sourceNeuron, sourceOutputTypePos,
                targetNeuron, targetInputTypePos,
                NeuralNetwork.getRandom().nextDouble() * 2.0D - 1.0D); // Random weight between -1 and 1
    }
    public Synapse(final NeuronInterface sourceNeuron, final int sourceOutputTypePo,
                   final NeuronInterface targetNeuron, final int targetInputTypePos,
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

    public NeuronInterface getSourceNeuron() {
        return sourceNeuron;
    }

    public NeuronInterface getTargetNeuron() {
        return targetNeuron;
    }

    public double getWeight() {
        return this.weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public int getSourceOutputTypePos() {
        return this.sourceOutputTypePos;
    }

    public int getTargetInputTypePos() {
        return this.targetInputTypePos;
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
        // Die Wiederherstellung der Verbindungen wird von NeuralNet.readObject() übernommen,
        // um zirkuläre Abhängigkeiten bei der Deserialisierung zu vermeiden.
    }
}
