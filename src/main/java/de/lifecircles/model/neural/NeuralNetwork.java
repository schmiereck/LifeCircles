package de.lifecircles.model.neural;

import java.util.*;
import java.io.Serializable;

/**
 * Represents the neural network that controls cell behavior.
 */
public class NeuralNetwork implements Serializable {
    private static final long serialVersionUID = 1L;
    private static Random random = new Random();

    private static final int INITIAL_SYNAPSE_CAPACITY = 64;
    public static final double DEFAULT_MUTATION_RATE = 0.1D;

    // Learning rate für Backpropagation
    private double learningRate = 0.01D;

    private final NeuronValueFunctionFactory neuronValueFunctionFactory;
    private final NeuronValueFunction neuronValueFunction;

    private final NeuralNet neuralNet;

    /**
     * Copy-Konstruktor: Erstellt eine exakte Kopie des übergebenen neuronalen Netzwerks
     *
     * @param original Das zu kopierende neuronale Netzwerk
     */
    public NeuralNetwork(final NeuralNetwork original) {
        this(original, true);
    }

    /**
     * Copy-Konstruktor: Erstellt eine exakte Kopie des übergebenen neuronalen Netzwerks
     *
     * @param original Das zu kopierende neuronale Netzwerk
     */
    public NeuralNetwork(final NeuralNetwork original, final boolean makeCopy) {
        this.neuronValueFunctionFactory = original.neuronValueFunctionFactory;
        this.neuronValueFunction = neuronValueFunctionFactory.create();

        this.neuralNet = new NeuralNet(original.neuralNet, makeCopy, this.neuronValueFunctionFactory, this.neuronValueFunction);
    }

    /**
     * Erstellt ein NeuronalNetwork mit dem übergebenen neuronalen NeuralNet.
     */
    public NeuralNetwork(final NeuralNet neuralNet, final NeuronValueFunctionFactory neuronValueFunctionFactory) {
        this.neuronValueFunctionFactory = neuronValueFunctionFactory;
        this.neuronValueFunction = this.neuronValueFunctionFactory.create();

        this.neuralNet = neuralNet;
    }

    /**
     * Constructs a network with a single hidden layer.
     *
     * @param inputCount Anzahl der Eingangsneuronen
     * @param hiddenCount Anzahl der Neuronen in der versteckten Schicht
     * @param outputCount Anzahl der Ausgangsneuronen
     * @param synapseConnectivity Prozentsatz der zu erstellenden Synapsen (0.0-1.0)
     */
    public NeuralNetwork(final NeuronValueFunctionFactory neuronValueFunctionFactory, int inputCount, int hiddenCount, int outputCount, double synapseConnectivity, final int fixedHiddenLayerCount) {
        this(neuronValueFunctionFactory, inputCount, new int[]{hiddenCount}, outputCount, synapseConnectivity, fixedHiddenLayerCount);
    }

    /**
     * Constructs a network with a single hidden layer with full connectivity.
     */
    public NeuralNetwork(final NeuronValueFunctionFactory neuronValueFunctionFactory, int inputCount, int hiddenCount, int outputCount) {
        this(neuronValueFunctionFactory, inputCount, new int[]{hiddenCount}, outputCount, 1.0D, 0);
    }

    /**
     * Constructs a network with multiple hidden layers.
     *
     * @param inputCount number of input neurons
     * @param hiddenCounts sizes of each hidden layer
     * @param outputCount number of output neurons
     * @param synapseConnectivity Prozentsatz der zu erstellenden Synapsen (0.0-1.0)
     */
    public NeuralNetwork(final NeuronValueFunctionFactory neuronValueFunctionFactory, final int inputCount,
                         final int[] hiddenCounts, final int outputCount, final double synapseConnectivity,
                         final int fixedHiddenLayerCount) {
        this.neuronValueFunctionFactory = neuronValueFunctionFactory;
        this.neuronValueFunction = neuronValueFunctionFactory.create();

        this.neuralNet = new NeuralNet(neuronValueFunctionFactory, this.neuronValueFunction, inputCount,
                hiddenCounts, outputCount, synapseConnectivity,
                fixedHiddenLayerCount);
    }

    public static Random getRandom() {
        return random;
    }

    public static void setRandom(final Random random) {
        NeuralNetwork.random = random;
    }

    public NeuralNet getNeuralNet() {
        return this.neuralNet;
    }

    /**
     * Sets the input values for the network.
     */
    public void setInputs(final double[] inputArr) {
        this.neuralNet.setInputs(this.neuronValueFunction, inputArr);
    }

    /**
     * Gibt die Größen aller Layer (Input, Hidden, Output) im Netzwerk zurück
     * @return Array mit den Größen der einzelnen Layer
     */
    public int[] getLayerSizes() {
        return this.neuralNet.getLayerSizes();
    }

    /**
     * Creates a mutated copy of this neural network.
     * @param mutationRate Probability of each weight/bias being mutated
     * @param mutationStrength Maximum amount of mutation
     */
    public NeuralNetwork mutate(final double mutationRate, final double mutationStrength) {
        // Verwende den Copy-Konstruktor, um eine exakte Kopie des Netzwerks zu erstellen
        NeuralNetwork mutated = new NeuralNetwork(this);

        mutated.neuralNet.mutate(this.getNeuronValueFunctionFactory(), mutated.getNeuronValueFunction(), mutationRate, mutationStrength);

        return mutated;
    }

    /**
     * Adds a hidden layer and ensures it is active by default.
     * Existing connections between adjacent layers are preserved.
     *
     * @param index Position, an der der neue Layer eingefügt werden soll
     * @param neuronCount Anzahl der Neuronen im neuen Layer
     * @param connectivity Prozentsatz der zu erstellenden Synapsen (0.0-1.0)
     */
    public void addHiddenLayer(final int index, final int neuronCount, double connectivity) {
        this.neuralNet.addHiddenLayer(neuronValueFunctionFactory, this.neuronValueFunction, index, neuronCount, connectivity);
    }

    // Expose the number of synapses for energy cost calculation
    public int getSynapseCount() {
        return this.neuralNet.getSynapseCount();
    }

    public List<Synapse> getSynapseList() {
        // Gibt eine unveränderliche Liste der Synapsen zurück
        return this.neuralNet.getSynapseList();
    }

    /**
     * Creates a mutated copy with more radikalen Änderungen wenn nötig.
     * Diese Methode ermöglicht stärkere Mutationen als die Standardmethode.
     */
    public NeuralNetwork mutateAggressively(final NeuronValueFunction neuronValueFunction, double mutationRate, double mutationStrength) {
        final NeuralNetwork mutated = this.mutate(mutationRate, mutationStrength);

        mutated.neuralNet.mutateAggressively(neuronValueFunctionFactory, neuronValueFunction, mutationRate, mutationStrength);

        return mutated;
    }

    public long getProccessedSynapses() {
        return this.neuralNet.getProccessedSynapses();
    }

    /**
     * Aktiviert oder deaktiviert die Layer-Deaktivierung (z.B. für Tests)
     */
    public void setDisableLayerDeactivation(final boolean disableLayerDeactivation) {
        this.neuralNet.setDisableLayerDeactivation(disableLayerDeactivation);
    }

    public boolean isDisableLayerDeactivation() {
        return this.neuralNet.isDisableLayerDeactivation();
    }

    /**
     * Sets the learning rate for backpropagation.
     *
     * @param learningRate Die Lernrate für den Gradientenabstieg (0.0 - 1.0)
     */
    public void setLearningRate(double learningRate) {
        this.learningRate = Math.max(0.0, Math.min(1.0, learningRate));
    }

    /**
     * Gets the current learning rate.
     *
     * @return Die aktuelle Lernrate
     */
    public double getLearningRate() {
        return this.learningRate;
    }

    /**
     * Trainiert das neuronale Netzwerk mittels Backpropagation.
     * Diese Methode berechnet den Fehler und aktualisiert die Gewichte und Bias-Werte.
     *
     * @param targetOutput Die erwarteten Ausgabewerte
     * @return Der quadratische Fehler des Netzwerks vor dem Training
     */
    public double backpropagate(final double[] targetOutput) {
        return this.neuralNet.backpropagate(this.neuronValueFunction, targetOutput, this.learningRate);
    }

    /**
     * Trainiert das Netzwerk mit einem Satz von Trainingsdaten.
     *
     * @param trainingInputs Die Eingaben für das Training
     * @param trainingTargets Die erwarteten Ausgaben
     * @param epochs Die Anzahl der Trainingsdurchläufe
     * @return Der durchschnittliche Fehler nach dem Training
     */
    public double train(double[][] trainingInputs, double[][] trainingTargets, int epochs) {
        if (trainingInputs.length != trainingTargets.length) {
            throw new IllegalArgumentException("Anzahl der Eingaben und Ziele muss übereinstimmen");
        }

        double totalError = 0.0;

        for (int epoch = 0; epoch < epochs; epoch++) {
            totalError = 0.0;

            for (int i = 0; i < trainingInputs.length; i++) {
                double error = this.calcTrainError(trainingInputs[i], trainingTargets[i]);
                totalError += error;
            }

            // Durchschnittlicher Fehler pro Trainingssatz
            totalError /= trainingInputs.length;
        }

        return totalError;
    }

    public double[] calcTrain(double[] trainingInputs, double[] trainingTargets) {
        // Forward pass
        final int outputTypePos = 0; // Default-Output-Type für Input-Neuronen.
        this.setInputs(trainingInputs);
        double[] outputArray = this.process();

        // Backward pass (Backpropagation)
        this.backpropagate(trainingTargets);

        return outputArray;
    }

    public double calcTrainError(double[] trainingInputs, double[] trainingTargets) {
        // Forward pass
        final int outputTypePos = 0; // Default-Output-Type für Input-Neuronen.
        this.setInputs(trainingInputs);
        this.process();

        // Backward pass (Backpropagation)
        return this.backpropagate(trainingTargets);
    }

    public void rnnClearPreviousState() {
        this.neuralNet.rnnClearPreviousState(this.neuronValueFunction);
    }

    public double readNeuronValue(final NeuronInterface neuron, final int outputTypePos) {
        return this.neuronValueFunction.readValue(this.getNeuralNet(), neuron, outputTypePos);
    }

    public void writeNeuronValue(final NeuronInterface neuron, final int outputTypePos, final double value) {
        this.neuronValueFunction.writeValue(this.getNeuralNet(), neuron, outputTypePos, value);
    }

    public NeuronValueFunctionFactory getNeuronValueFunctionFactory() {
        return this.neuronValueFunctionFactory;
    }

    public NeuronValueFunction getNeuronValueFunction() {
        return this.neuronValueFunction;
    }

    public double[] process() {
        return this.neuralNet.process(this.neuronValueFunction);
    }

    public Layer[] getHiddenLayerArr() {
        return this.neuralNet.getHiddenLayerArr();
    }

    public Neuron[] getInputNeuronArr() {
        return this.neuralNet.getInputNeuronArr();
    }

    public NeuronInterface[] getOutputNeuronArr() {
        return this.neuralNet.getOutputNeuronArr();
    }

    public double getInputLayerSize() {
        return this.neuralNet.getInputLayerSize();
    }

    public double getOutputLayerSize() {
        return this.neuralNet.getOutputLayerSize();
    }

    public void setEnableNeuronType(final boolean enableNeuronType) {
        this.neuralNet.setEnableNeuronType(enableNeuronType);
    }
}
