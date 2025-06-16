package de.lifecircles.model.neural;

import de.lifecircles.service.SimulationConfig;

import java.io.Serial;
import java.util.*;
import java.util.function.Consumer;
import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.IOException;

/**
 * Represents the neural network that controls cell behavior.
 */
public class NeuralNetwork implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Neuron[] inputNeuronList;
    private Layer[] hiddenLayerArr; // Array statt List
    private final Neuron[] outputNeuronArr;
    // Synapsen als Array statt List
    private Synapse[] synapseArray;
    private static Random random = new Random();
    private double[] outputArr;

    private static final int INITIAL_SYNAPSE_CAPACITY = 64;
    private static final double DEFAULT_MUTATION_RATE = 0.1D;
    private int fixedHiddenLayerCount = SimulationConfig.CELL_STATE_ACTIVE_LAYER_COUNT;

    // Flag zum Deaktivieren der Layer-Deaktivierung (z.B. für Tests)
    private transient boolean disableLayerDeactivation = false;

    private long proccessedSynapses = 0L;

    // Learning rate für Backpropagation
    private double learningRate = 0.01D;

    private final NeuronValueFunctionFactory neuronValueFunctionFactory;
    private final NeuronValueFunction neuronValueFunction;

    /**
     * List of Neuron-Types can be used in this Network.
     */
    private List<NeuronTypeInfoData> neuronTypeInfoDataList = new ArrayList<>() {
        {
            add(new NeuronTypeInfoData(1, 1, null)); // Initialisiere mit einem Dummy-Wert
        }
    };

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

        if (makeCopy) {
            this.inputNeuronList = new Neuron[original.inputNeuronList.length];
            this.outputNeuronArr = new Neuron[original.outputNeuronArr.length];
            // Synapsen-Array initialisieren
            this.synapseArray = new Synapse[original.getSynapseList().size()];

            this.fixedHiddenLayerCount = original.fixedHiddenLayerCount;

            // Erstelle eine Map, die die Originalneuronen den neuen Neuronen zuordnet
            final Map<NeuronInterface, NeuronInterface> neuronMap = new HashMap<>();

            // Kopiere Input-Neuronen
            for (int inputNeuronPos = 0; inputNeuronPos < original.inputNeuronList.length; inputNeuronPos++) {
                final Neuron originalNeuron = original.inputNeuronList[inputNeuronPos];
                final Neuron newNeuron = new Neuron(this.neuronValueFunction.fetchNextFreeId(this), originalNeuron.getNeuronTypeInfoData());
                newNeuron.setActivationFunction(originalNeuron.getActivationFunction());
                for (int outputTypePos = 0; outputTypePos < originalNeuron.getNeuronTypeInfoData().getOutputCount(); outputTypePos++) {
                    newNeuron.setBias(outputTypePos, originalNeuron.getBias(outputTypePos));
                    final double value = this.neuronValueFunction.readValue(this, originalNeuron, outputTypePos);
                    this.neuronValueFunction.writeValue(this, newNeuron, outputTypePos, value);
                }
                this.inputNeuronList[inputNeuronPos] = newNeuron;
                neuronMap.put(originalNeuron, newNeuron);
            }

            // Kopiere Hidden Layers
            this.hiddenLayerArr = new Layer[original.hiddenLayerArr.length];
            for (int hiddenLayerPos = 0; hiddenLayerPos < original.hiddenLayerArr.length; hiddenLayerPos++) {
                final Layer originalLayer = original.hiddenLayerArr[hiddenLayerPos];
                final Layer newLayer = new Layer();
                newLayer.setActiveLayer(originalLayer.isActiveLayer());

                for (final NeuronInterface originalNeuron : originalLayer.getNeuronsArr()) {
                    final NeuronInterface newNeuron = originalNeuron.cloneNeuron(this, newLayer.isActiveLayer());
                    newLayer.addNeuron(newNeuron);
                    neuronMap.put(originalNeuron, newNeuron);
                }
                this.hiddenLayerArr[hiddenLayerPos] = newLayer;
            }

            // Kopiere Output-Neuronen
            for (int outputNeuronPos = 0; outputNeuronPos < original.outputNeuronArr.length; outputNeuronPos++) {
                final Neuron originalNeuron = original.outputNeuronArr[outputNeuronPos];
                final Neuron newNeuron = new Neuron(this.neuronValueFunction.fetchNextFreeId(this), originalNeuron.getNeuronTypeInfoData());
                newNeuron.setActivationFunction(originalNeuron.getActivationFunction());
                for (int outputTypePos = 0; outputTypePos < originalNeuron.getNeuronTypeInfoData().getOutputCount(); outputTypePos++) {
                    newNeuron.setBias(outputTypePos, originalNeuron.getBias(outputTypePos));
                    final double value = this.neuronValueFunction.readValue(this, originalNeuron, outputTypePos);
                    this.neuronValueFunction.writeValue(this, newNeuron, outputTypePos, value);
                }
                newNeuron.setOutputNeuron(true); // Markiere als Output-Neuron
                this.outputNeuronArr[outputNeuronPos] = newNeuron;
                neuronMap.put(originalNeuron, newNeuron);
            }

            // Erstelle das Array für die Ausgabewerte
            this.outputArr = new double[this.outputNeuronArr.length];

            // Kopiere alle Synapsen mit den korrekten Verbindungen zwischen den neuen Neuronen
            int synapseIndex = 0;
            for (int i = 0; i < original.getSynapseList().size(); i++) {
                final Synapse originalSynapse = original.getSynapseList().get(i);
                final NeuronInterface sourceNeuron = neuronMap.get(originalSynapse.getSourceNeuron());
                final NeuronInterface targetNeuron = neuronMap.get(originalSynapse.getTargetNeuron());
                if (sourceNeuron != null && targetNeuron != null) {
                    Synapse newSynapse = new Synapse(sourceNeuron, originalSynapse.getSourceOutputTypePos(),
                            targetNeuron, originalSynapse.getTargetInputTypePos(),
                            originalSynapse.getWeight());
                    this.synapseArray[synapseIndex++] = newSynapse;
                }
            }
            if (synapseIndex < this.synapseArray.length) {
                final Synapse[] resized = new Synapse[synapseIndex];
                System.arraycopy(this.synapseArray, 0, resized, 0, synapseIndex);
                this.synapseArray = resized;
            }
        } else {
            this.inputNeuronList = original.inputNeuronList;
            this.outputNeuronArr = original.outputNeuronArr;
            // Synapsen-Array initialisieren
            this.synapseArray = original.synapseArray;

            this.fixedHiddenLayerCount = original.fixedHiddenLayerCount;

            this.hiddenLayerArr = original.hiddenLayerArr;

            this.outputArr = new double[this.outputNeuronArr.length];
        }
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

        this.inputNeuronList = new Neuron[inputCount];
        this.outputNeuronArr = new Neuron[outputCount];
        // Synapsen-Array initialisieren
        this.synapseArray = new Synapse[0];

        this.fixedHiddenLayerCount = fixedHiddenLayerCount;

        // create input neurons
        for (int i = 0; i < this.inputNeuronList.length; i++) {
            this.inputNeuronList[i] = new Neuron(this.neuronValueFunction.fetchNextFreeId(this),
                    this.neuronTypeInfoDataList.get(0));
        }

        // create hidden layers
        this.hiddenLayerArr = new Layer[hiddenCounts.length];
        for (int i = 0; i < hiddenCounts.length; i++) {
            int count = hiddenCounts[i];
            Layer layer = new Layer();
            for (int j = 0; j < count; j++) {
                layer.addNeuron(new Neuron(this.neuronValueFunction.fetchNextFreeId(this),
                        this.neuronTypeInfoDataList.get(0)));
            }
            this.hiddenLayerArr[i] = layer;
        }

        // create output neurons
        for (int i = 0; i < this.outputNeuronArr.length; i++) {
            Neuron outputNeuron = new Neuron(this.neuronValueFunction.fetchNextFreeId(this),
                    this.neuronTypeInfoDataList.get(0));
            outputNeuron.setOutputNeuron(true); // Markiere als Output-Neuron
            this.outputNeuronArr[i] = outputNeuron;
        }
        this.outputArr = new double[this.outputNeuronArr.length];

        // connect layers sequentially: input -> hidden1 -> ... -> hiddenN -> output
        NeuronInterface[] prev = this.inputNeuronList;
        for (Layer layer : this.hiddenLayerArr) {
            this.connectLayers(prev, layer.getNeuronsArr(), synapseConnectivity);
            prev = layer.getNeuronsArr();
        }
        this.connectLayers(prev, this.outputNeuronArr, synapseConnectivity);
    }

    public static Random getRandom() {
        return random;
    }

    private void connectLayers(NeuronInterface[] sourceLayer, NeuronInterface[] targetLayer, double connectivity) {
        connectivity = Math.max(0.0, Math.min(1.0, connectivity));
        for (NeuronInterface source : sourceLayer) {
            for (NeuronInterface target : targetLayer) {
                if (connectivity >= 1.0 || this.random.nextDouble() < connectivity) {
                    double limit = Math.sqrt(6.0 / (this.getInputLayerSize() + this.getOutputLayerSize()));
                    final double weight = this.random.nextDouble() * 2.0D * limit - limit;
                    addSynapse(new Synapse(source, this.random.nextInt(source.getNeuronTypeInfoData().getOutputCount()),
                            target, this.random.nextInt(target.getNeuronTypeInfoData().getInputCount()),
                            weight));
                }
            }
        }
    }

    public static void setRandom(final Random random) {
        NeuralNetwork.random = random;
    }

    // Hilfsmethode zum Hinzufügen einer Synapse zum Array
    private void addSynapse(Synapse synapse) {
        Synapse[] newArr = new Synapse[this.synapseArray.length + 1];
        System.arraycopy(this.synapseArray, 0, newArr, 0, this.synapseArray.length);
        newArr[this.synapseArray.length] = synapse;
        this.synapseArray = newArr;
    }

    /**
     * Sets the input values for the network.
     */
    public void setInputs(final double[] inputArr) {
        if (inputArr.length != this.inputNeuronList.length) {
            throw new IllegalArgumentException("Input size mismatch");
        }
        final int outputTypePos = 0; // Default-Output-Type für Input-Neuronen.
        for (int inputPos = 0; inputPos < inputArr.length; inputPos++) {
            this.neuronValueFunction.writeValue(this, this.inputNeuronList[inputPos], outputTypePos, inputArr[inputPos]);
        }
    }

    /**
     * Processes the inputs through the network and returns the outputs.
     */
    public double[] process() {
        this.proccessedSynapses = 0L;

        // process hidden layers
        for (final Layer layer : this.hiddenLayerArr) {
            final NeuronInterface[] neuronArr = layer.getNeuronsArr();
            if (layer.isActiveLayer() || this.disableLayerDeactivation) {
                for (final NeuronInterface neuron : neuronArr) {
                    this.proccessedSynapses += this.activate(neuron);
                }
            } else {
                if (neuronArr.length > 0) {
                    // For inactive layers aktivete allwayse the first neuron.
                    this.proccessedSynapses += this.activate(layer.getNeuronsArr()[0]);
                }
            }
        }

        // process output layer
        for (final NeuronInterface neuron : this.outputNeuronArr) {
            this.proccessedSynapses += this.activate(neuron);
        }

        // Collect outputs
        for (int outputNeuronPos = 0; outputNeuronPos < this.outputArr.length; outputNeuronPos++) {
            this.outputArr[outputNeuronPos] = this.getOutputValue(outputNeuronPos);
        }

        // process hidden layer activation counters (ohne fixed layers)
        if (!disableLayerDeactivation) {
            for (int layerPos = this.fixedHiddenLayerCount; layerPos < hiddenLayerArr.length; layerPos++) {
                final Layer layer = this.hiddenLayerArr[layerPos];
                final NeuronInterface[] neuronArr = layer.getNeuronsArr();
                if (neuronArr.length > 0) {
                    final NeuronInterface activationNeuron = neuronArr[0];
                    final int activationOutputTypePos = 0; // Default-Output-Type for activation.
                    final double activationValue = Math.max(1.0D / 1000,
                            layer.getActivationCounter() + this.neuronValueFunction.readValue(this, activationNeuron, activationOutputTypePos));
                    if (activationValue >= 1.0D) {
                        layer.setActivationCounter(0.0D);
                        layer.setActiveLayer(true);
                    } else {
                        layer.setActivationCounter(activationValue);
                        layer.setActiveLayer(false);
                    }
                }
            }
        } else {
            // Wenn deaktiviert: alle Layer aktiv lassen
            for (final Layer layer : this.hiddenLayerArr) {
                layer.setActiveLayer(true);
            }
        }

        return this.outputArr;
    }

    /**
     * Calculates the neuron's output value based on its inputs.
     * Optimized version using array iteration instead of ArrayList.
     */
    public long activate(final NeuronInterface neuron) {
        return neuron.activate(this);
    }

    /**
     * Gibt die Größen aller Layer (Input, Hidden, Output) im Netzwerk zurück
     * @return Array mit den Größen der einzelnen Layer
     */
    public int[] getLayerSizes() {
        int totalLayers = 2 + hiddenLayerArr.length; // Input + Hidden + Output
        int[] sizes = new int[totalLayers];

        // Input Layer
        sizes[0] = inputNeuronList.length;

        // Hidden Layers
        for (int i = 0; i < hiddenLayerArr.length; i++) {
            sizes[i + 1] = hiddenLayerArr[i].getNeuronsArr().length;
        }

        // Output Layer
        sizes[totalLayers - 1] = outputNeuronArr.length;

        return sizes;
    }

    public double getOutputValue(final int outputNeuronPos) {
        final int outputTypePos = 0; // Default-Output-Type for output neurons.
        return this.neuronValueFunction.readValue(this, this.outputNeuronArr[outputNeuronPos], outputTypePos);
    }

    /**
     * Returns the number of input neurons in the network.
     * @return the count of input neurons
     */
    public int getInputCount() {
        return this.inputNeuronList.length;
    }

    /**
     * Creates a mutated copy of this neural network.
     * @param mutationRate Probability of each weight/bias being mutated
     * @param mutationStrength Maximum amount of mutation
     */
    public NeuralNetwork mutate(final double mutationRate, final double mutationStrength) {
        // Verwende den Copy-Konstruktor, um eine exakte Kopie des Netzwerks zu erstellen
        NeuralNetwork mutated = new NeuralNetwork(this);

        mutated.disableLayerDeactivation = this.disableLayerDeactivation; // Behalte das Flag bei

        // Mutate weights and biases
        mutated.applyToAllNeurons(neuron -> {
            if (random.nextDouble() < mutationRate) {
                double mutation = (random.nextDouble() * 2.0D - 1.0D) * mutationStrength;
                final int outputTypePos = random.nextInt(neuron.getNeuronTypeInfoData().getOutputCount());
                neuron.setBias(outputTypePos,neuron.getBias(outputTypePos) + mutation);
            }
        });

        for (Synapse synapse : mutated.synapseArray) {
            if (random.nextDouble() < mutationRate) {
                double mutation = (random.nextDouble() * 2.0D - 1.0D) * mutationStrength;
                synapse.setWeight(synapse.getWeight() + mutation);
            }
        }

        // Apply structural mutations
        mutated.applyStructuralMutations(mutationRate);

        return mutated;
    }

    /**
     * Wendet eine Funktion auf alle Neuronen des Netzwerks an.
     * Dies ist effizienter als eine neue Liste aller Neuronen zu erstellen.
     *
     * @param function Die Funktion, die auf jedes Neuron angewendet werden soll
     */
    public void applyToAllNeurons(final Consumer<NeuronInterface> function) {
        // Anwenden auf Input-Neuronen
        for (final NeuronInterface neuron : this.inputNeuronList) {
            function.accept(neuron);
        }

        // Anwenden auf Hidden-Layer-Neuronen
        for (final Layer layer : this.hiddenLayerArr) {
            for (final NeuronInterface neuron : layer.getNeuronsArr()) {
                function.accept(neuron);
            }
        }

        // Anwenden auf Output-Neuronen
        for (final NeuronInterface neuron : this.outputNeuronArr) {
            function.accept(neuron);
        }
    }

    /**
     * Erstellt eine Mapping-Tabelle, die jedem Neuron des Quellnetzwerks
     * das entsprechende Neuron im Zielnetzwerk zuordnet.
     */
    private Map<NeuronInterface, NeuronInterface> createNeuronMapping(NeuralNetwork target) {
        Map<NeuronInterface, NeuronInterface> mapping = new HashMap<>();

        // Input-Neuronen zuordnen
        for (int i = 0; i < this.inputNeuronList.length; i++) {
            mapping.put(this.inputNeuronList[i], target.inputNeuronList[i]);
        }

        // Hidden-Layer-Neuronen zuordnen
        for (int i = 0; i < this.hiddenLayerArr.length; i++) {
            NeuronInterface[] sourceLayer = this.hiddenLayerArr[i].getNeuronsArr();
            NeuronInterface[] targetLayer = target.hiddenLayerArr[i].getNeuronsArr();
            for (int j = 0; j < sourceLayer.length; j++) {
                mapping.put(sourceLayer[j], targetLayer[j]);
            }
        }

        // Output-Neuronen zuordnen
        for (int i = 0; i < this.outputNeuronArr.length; i++) {
            mapping.put(this.outputNeuronArr[i], target.outputNeuronArr[i]);
            // Stelle sicher, dass Output-Neuronen richtig markiert sind
            target.outputNeuronArr[i].setOutputNeuron(true);
        }

        return mapping;
    }

    // Structural mutation helpers
    private void applyStructuralMutations(double mutationRate) {
        // Structural mutations should be much less frequent than weight/bias mutations
        double structuralMutationRate = mutationRate * DEFAULT_MUTATION_RATE; // Only 10% of the regular mutation rate

        // Möglichkeit, ein komplettes Hidden Layer hinzuzufügen
        int addLayerRange = (this.hiddenLayerArr.length - this.fixedHiddenLayerCount) + 1;
        if (addLayerRange > 0 && this.random.nextDouble() < structuralMutationRate) {
            final int pos = random.nextInt(addLayerRange) + this.fixedHiddenLayerCount;
            // Zufällige Neuronenzahl zwischen 1 und 5
            int neuronCount = 1 + this.random.nextInt(5);
            this.addHiddenLayer(pos, neuronCount, structuralMutationRate);
        }

        // Möglichkeit, ein Hidden Layer zu entfernen
        int removeLayerRange = (this.hiddenLayerArr.length - this.fixedHiddenLayerCount);
        if (removeLayerRange > 0 && this.random.nextDouble() < structuralMutationRate * 1.5D) { // Erhöhte Wahrscheinlichkeit
            final int pos = random.nextInt(removeLayerRange) + this.fixedHiddenLayerCount;
            this.removeHiddenLayer(pos);
        }

        // Wahrscheinlichkeit, ein Neuron zu einem bestehenden Layer hinzuzufügen
        int addNeuronLayerCount = this.hiddenLayerArr.length - this.fixedHiddenLayerCount;
        if (addNeuronLayerCount > 0 &&
                this.random.nextDouble() < structuralMutationRate * 1.5D) { // Verdoppelte Wahrscheinlichkeit
            int li = this.random.nextInt(addNeuronLayerCount) + this.fixedHiddenLayerCount;
            this.addNeuronToHiddenLayer(li, structuralMutationRate);
        }

        // Wahrscheinlichkeit, ein Neuron zu entfernen
        if (this.hiddenLayerArr.length > 0 && this.random.nextDouble() < structuralMutationRate) {
            int li = random.nextInt(this.hiddenLayerArr.length);
            this.removeNeuronFromHiddenLayer(li);
        }

        // Wahrscheinlichkeit, eine zufällige Synapse hinzuzufügen
        if (this.random.nextDouble() < (structuralMutationRate * 3.0D)) { // erhöhte Wahrscheinlichkeit
            this.addRandomSynapse();
        }

        // Wahrscheinlichkeit, eine zufällige Synapse zu entfernen
        if (this.random.nextDouble() < structuralMutationRate) {
            this.removeRandomSynapse();
        }

        // Neue Mutation: Aktivierungsfunktion eines zufälligen Neurons ändern
        if (this.hiddenLayerArr.length > 0 &&
                (this.random.nextDouble() < structuralMutationRate)) {
            // Wähle ein zufälliges Hidden Layer
            List<NeuronInterface> layer = this.hiddenLayerArr[random.nextInt(this.hiddenLayerArr.length)].getNeuronList();
            if (!layer.isEmpty()) {
                final NeuronInterface neuron = layer.get(random.nextInt(layer.size()));

                neuron.mutateNeuron(random);
            }
        }
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
        // Begrenze die Konnektivität auf gültige Werte
        connectivity = Math.max(0.0, Math.min(1.0, connectivity));

        final Layer newLayer = new Layer();
        newLayer.setActiveLayer(true); // Set new layer as active by default

        // Erstelle die neuen Neuronen
        for (int i = 0; i < neuronCount; i++) {
            newLayer.addNeuron(new Neuron(this.neuronValueFunction.fetchNextFreeId(this),
                    this.neuronTypeInfoDataList.get(this.random.nextInt(this.neuronTypeInfoDataList.size()))));
        }

        // Bestimme die angrenzenden Layer
        final List<NeuronInterface> prevLayer = index == 0 ? Arrays.asList(this.inputNeuronList) : this.hiddenLayerArr[index - 1].getNeuronList();
        final List<NeuronInterface> nextLayer = index == this.hiddenLayerArr.length ? Arrays.asList(this.outputNeuronArr) : this.hiddenLayerArr[index].getNeuronList();

        // Füge den neuen Layer ein
        this.hiddenLayerArr = insertLayer(this.hiddenLayerArr, index, newLayer);

        // Verbinde den vorherigen Layer mit dem neuen Layer
        for (final NeuronInterface srcNeuron : prevLayer) {
            for (final NeuronInterface tgtNeuron : newLayer.getNeuronsArr()) {
                if (connectivity >= 1.0D || random.nextDouble() < connectivity) {
                    final double weight = random.nextDouble() * 0.002D - 0.001D;
                    addSynapse(new Synapse(srcNeuron, random.nextInt(srcNeuron.getNeuronTypeInfoData().getOutputCount()),
                            tgtNeuron, random.nextInt(tgtNeuron.getNeuronTypeInfoData().getInputCount()),
                            weight));
                }
            }
        }

        // Verbinde den neuen Layer mit dem nächsten Layer
        for (final NeuronInterface srcNeuron : newLayer.getNeuronsArr()) {
            for (final NeuronInterface tgtNeuron : nextLayer) {
                if (connectivity >= 1.0D || random.nextDouble() < connectivity) {
                    final double weight = random.nextDouble() * 0.002D - 0.001D;
                    addSynapse(new Synapse(srcNeuron, random.nextInt(srcNeuron.getNeuronTypeInfoData().getOutputCount()),
                            tgtNeuron, random.nextInt(tgtNeuron.getNeuronTypeInfoData().getInputCount()),
                            weight));
                }
            }
        }
    }

    // Hilfsmethode zum Einfügen eines Layers in ein Array
    private static Layer[] insertLayer(Layer[] arr, int index, Layer layer) {
        Layer[] result = new Layer[arr.length + 1];
        System.arraycopy(arr, 0, result, 0, index);
        result[index] = layer;
        System.arraycopy(arr, index, result, index + 1, arr.length - index);
        return result;
    }

    public void addNeuronToHiddenLayer(final int layerIndex, final double connectivity) {
        final  Neuron newNeuron = new Neuron(this.neuronValueFunction.fetchNextFreeId(this),
                this.neuronTypeInfoDataList.get(random.nextInt(this.neuronTypeInfoDataList.size())));
        this.hiddenLayerArr[layerIndex].addNeuron(newNeuron);
        final List<NeuronInterface> prevNeuronList = layerIndex == 0 ? Arrays.asList(this.inputNeuronList) : this.hiddenLayerArr[layerIndex - 1].getNeuronList();
        for (final NeuronInterface srcNeuron : prevNeuronList) {
            if (connectivity >= 1.0 || random.nextDouble() < connectivity) {
                final double weight = random.nextDouble() * 0.002D - 0.001D;
                this.addSynapse(new Synapse(srcNeuron, random.nextInt(srcNeuron.getNeuronTypeInfoData().getOutputCount()),
                        newNeuron, random.nextInt(newNeuron.getNeuronTypeInfoData().getInputCount()),
                        weight));
            }
        }
        final List<NeuronInterface> nextNeuronList = layerIndex == this.hiddenLayerArr.length - 1 ? Arrays.asList(this.outputNeuronArr) : this.hiddenLayerArr[layerIndex + 1].getNeuronList();
        for (final NeuronInterface tgtNeuron : nextNeuronList) {
            if (connectivity >= 1.0 || random.nextDouble() < connectivity) {
                final double weight = random.nextDouble() * 0.002D - 0.001D;
                this.addSynapse(new Synapse(newNeuron, random.nextInt(newNeuron.getNeuronTypeInfoData().getOutputCount()),
                        tgtNeuron, random.nextInt(tgtNeuron.getNeuronTypeInfoData().getInputCount()),
                        weight));
            }
        }
    }

    private void removeHiddenLayer(final int pos) {
        final Layer layer = this.hiddenLayerArr[pos];
        if (layer.getNeuronList().size() == 1) {
            // Entferne Layer aus Array
            this.hiddenLayerArr = removeLayer(this.hiddenLayerArr, pos);
        }
    }

    // Hilfsmethode zum Entfernen eines Layers aus einem Array
    private static Layer[] removeLayer(final Layer[] arr, final int index) {
        final Layer[] result = new Layer[arr.length - 1];
        System.arraycopy(arr, 0, result, 0, index);
        System.arraycopy(arr, index + 1, result, index, arr.length - index - 1);
        return result;
    }

    public void removeNeuronFromHiddenLayer(final int layerIndex) {
        final Layer layer = this.hiddenLayerArr[layerIndex];
        final List<NeuronInterface> layerNeuronList = layer.getNeuronList();
        if (layerNeuronList.size() > 1) {
            // Skip the first neuron to avoid removing activation neurons
            final int idx = random.nextInt(layerNeuronList.size() - 1) + 1;
            this.removeNeuron(layer, idx);
        }
    }

    private void removeNeuron(final Layer layer, final int neuronIdx) {
        final List<NeuronInterface> layerNeuronList = layer.getNeuronList();
        final NeuronInterface removedNeuron = layerNeuronList.remove(neuronIdx);
        int synapsePos = 0;
        while (synapsePos < this.synapseArray.length) {
            final Synapse synapse = this.synapseArray[synapsePos];
            final NeuronInterface sourceNeuron = synapse.getSourceNeuron();
            final NeuronInterface targetNeuron = synapse.getTargetNeuron();
            if (sourceNeuron == removedNeuron || targetNeuron == removedNeuron) {
                final int outputTypePos = synapse.getSourceOutputTypePos();
                sourceNeuron.removeOutputSynapse(outputTypePos, synapse);
                if (targetNeuron == removedNeuron) {
                    // Entferne Synapse
                    removeSynapseAt(synapsePos);
                    continue;
                }
                int inputTypePos = synapse.getTargetInputTypePos();
                targetNeuron.removeInputSynapse(inputTypePos, synapse);
                removeSynapseAt(synapsePos);
            } else {
                synapsePos++;
            }
        }
        this.neuronValueFunction.releaseNeuron(this, removedNeuron);
    }
    // Hilfsmethode zum Entfernen einer Synapse an Index
    private void removeSynapseAt(int idx) {
        if (this.synapseArray.length == 0) return;
        Synapse[] newArr = new Synapse[this.synapseArray.length - 1];
        System.arraycopy(this.synapseArray, 0, newArr, 0, idx);
        System.arraycopy(this.synapseArray, idx + 1, newArr, idx, this.synapseArray.length - idx - 1);
        this.synapseArray = newArr;
    }

    public void addRandomSynapse() {
        // Wählen Sie zufällige Quelle und Ziel-Layer
        List<List<NeuronInterface>> allLayers = new ArrayList<>();
        allLayers.add(Arrays.asList(this.inputNeuronList));
        for (Layer l : this.hiddenLayerArr) allLayers.add(l.getNeuronList());
        allLayers.add(Arrays.asList(this.outputNeuronArr));

        int sourceLayerIndex = this.random.nextInt(allLayers.size());
        // Exclude the input layer for the target.
        int targetLayerIndex = this.random.nextInt(allLayers.size() - 1) + 1;

        // Make sure we're not connecting within the same layer
        if (sourceLayerIndex == targetLayerIndex) {
            targetLayerIndex = (sourceLayerIndex + 1) % allLayers.size();
        }

        List<NeuronInterface> sourceLayer = allLayers.get(sourceLayerIndex);
        List<NeuronInterface> targetLayer = allLayers.get(targetLayerIndex);

        if (sourceLayer.isEmpty() || targetLayer.isEmpty()) return;

        // Wählen Sie zufällige Neuronen aus den beiden Layern
        NeuronInterface sourceNeuron = sourceLayer.get(this.random.nextInt(sourceLayer.size()));
        NeuronInterface targetNeuron = targetLayer.get(this.random.nextInt(targetLayer.size()));

        // Erstellen Sie die Synapse
        Synapse synapse = new Synapse(sourceNeuron, random.nextInt(sourceNeuron.getNeuronTypeInfoData().getOutputCount()),
                targetNeuron, random.nextInt(targetNeuron.getNeuronTypeInfoData().getInputCount()),
                random.nextDouble() * 0.002D - 0.001D);
        addSynapse(synapse);
    }

    public void removeRandomSynapse() {
        if (this.synapseArray.length == 0) return;
        final int synapseIdx = random.nextInt(this.synapseArray.length);
        final Synapse removedSynapse = this.synapseArray[synapseIdx];
        //removedSynapse.getSourceNeuron().getOutputSynapseList(removedSynapse.getSourceOutputTypePos()).remove(removedSynapse);
        removedSynapse.getSourceNeuron().removeOutputSynapse(removedSynapse.getSourceOutputTypePos(), removedSynapse);
        removedSynapse.getTargetNeuron().removeInputSynapse(removedSynapse.getTargetInputTypePos(), removedSynapse);
        removeSynapseAt(synapseIdx);
    }

    // Expose the number of synapses for energy cost calculation
    public int getSynapseCount() {
        return this.synapseArray.length;
    }

    public List<Synapse> getSynapseList() {
        // Gibt eine unveränderliche Liste der Synapsen zurück
        return Collections.unmodifiableList(Arrays.asList(this.synapseArray));
    }

    public Synapse[] getSynapseArr() {
        return this.synapseArray;
    }

    /**
     * Returns the total count of all neurons in the network.
     * This is more efficient than creating a list of all neurons first.
     *
     * @return the total number of neurons in the network
     */
    public int getAllNeuronsSize() {
        int count = this.inputNeuronList.length + this.outputNeuronArr.length;

        for (Layer layer : this.hiddenLayerArr) {
            count += layer.getNeuronList().size();
        }

        return count;
    }

    /**
     * Creates a mutated copy with more radikalen Änderungen wenn nötig.
     * Diese Methode ermöglicht stärkere Mutationen als die Standardmethode.
     */
    public NeuralNetwork mutateAggressively(double mutationRate, double mutationStrength) {
        NeuralNetwork mutated = this.mutate(mutationRate, mutationStrength);

        // 50% Chance für eine zusätzliche Strukturmutation
        if (random.nextDouble() < 0.5) {
            // Füge ein zusätzliches Hidden Layer hinzu
            int pos = random.nextInt(mutated.hiddenLayerArr.length + 1);
            int neuronCount = 2 + random.nextInt(4); // 2-5 Neuronen
            mutated.addHiddenLayer(pos, neuronCount, 1.0);
        }

        // 50% Chance für mehrere zusätzliche Neuronen
        if (this.random.nextDouble() < 0.5 && !mutated.hiddenLayerArr.equals(null)) {
            int count = 1 + this.random.nextInt(3); // 1-3 neue Neuronen
            for (int i = 0; i < count; i++) {
                int layer = this.random.nextInt(mutated.hiddenLayerArr.length);
                mutated.addNeuronToHiddenLayer(layer, mutationStrength);
            }
        }

        // 70% Chance für mehrere zusätzliche Synapsen
        if (this.random.nextDouble() < 0.7) {
            int count = 2 + this.random.nextInt(4); // 2-5 neue Synapsen
            for (int i = 0; i < count; i++) {
                mutated.addRandomSynapse();
            }
        }

        return mutated;
    }

    public int getInputLayerSize() {
        return this.inputNeuronList.length;
    }

    public int getOutputLayerSize() {
        return this.outputNeuronArr.length;
    }

    /**
     * Gibt das Array der Input-Neuronen zurück
     * @return Array der Input-Neuronen
     */
    public Neuron[] getInputNeuronArr() {
        return this.inputNeuronList;
    }

    /**
     * Gibt die Liste der Hidden-Layer zurück
     * @return Liste der Hidden-Layer
     */
    public Layer[] getHiddenLayerArr() {
        return this.hiddenLayerArr;
    }

    /**
     * Gibt die Liste der Output-Neuronen zurück
     * @return Liste der Output-Neuronen
     */
    public Neuron[] getOutputNeuronArr() {
        return this.outputNeuronArr;
    }

    public long getProccessedSynapses() {
        return this.proccessedSynapses;
    }

    /**
     * Aktiviert oder deaktiviert die Layer-Deaktivierung (z.B. für Tests)
     */
    public void setDisableLayerDeactivation(final boolean disableLayerDeactivation) {
        this.disableLayerDeactivation = disableLayerDeactivation;
    }

    public boolean isDisableLayerDeactivation() {
        return this.disableLayerDeactivation;
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
        if (targetOutput.length != this.outputNeuronArr.length) {
            throw new IllegalArgumentException("Zielausgabe muss die gleiche Größe haben wie die Ausgabeschicht");
        }

        // Berechne den Fehler vor dem Training
        final double error = calculateError(targetOutput);

        // 1. Berechne den Fehler für die Ausgabeneuronen
        for (int outputNeuronPos = 0; outputNeuronPos < this.outputNeuronArr.length; outputNeuronPos++) {
            final Neuron outputNeuron = this.outputNeuronArr[outputNeuronPos];
            for (int outputTypePos = 0; outputTypePos < outputNeuron.getNeuronTypeInfoData().getOutputCount(); outputTypePos++) {
                final double output = this.neuronValueFunction.readValue(this, outputNeuron, outputTypePos);
                final double target = targetOutput[outputNeuronPos];

                // Delta = (Ausgabe - Ziel) * Ableitung der Aktivierungsfunktion
                final double delta = (output - target) *
                        outputNeuron.getActivationFunction().derivative(outputNeuron.getInputSum(outputTypePos));
                outputNeuron.setDelta(outputTypePos, delta);
            }
        }

        // 2. Backpropagiere den Fehler durch alle versteckten Schichten (von hinten nach vorne)
        for (int hiddenLayerPos = this.hiddenLayerArr.length - 1; hiddenLayerPos >= 0; hiddenLayerPos--) {
            final Layer layer = this.hiddenLayerArr[hiddenLayerPos];
            if (!layer.isActiveLayer() && !disableLayerDeactivation) continue; // Überspringe inaktive Layer

            final NeuronInterface[] neurons = layer.getNeuronsArr();
            for (final NeuronInterface neuron : neurons) {
                neuron.backpropagateDelta();
            }
        }

        // 3. Aktualisiere Gewichte und Bias-Werte
        this.updateWeights();

        return error;
    }

    /**
     * Berechnet den quadratischen Fehler des Netzwerks
     *
     * @param targetOutput Die erwarteten Ausgabewerte
     * @return Der quadratische Fehler
     */
    private double calculateError(double[] targetOutput) {
        double error = 0.0D;
        final int outputTypePos = 0; // Default-Output-Type für Output-Neuronen.
        for (int outputNeuronPos = 0; outputNeuronPos < this.outputNeuronArr.length; outputNeuronPos++) {
            double output = this.neuronValueFunction.readValue(this, this.outputNeuronArr[outputNeuronPos], outputTypePos);
            double target = targetOutput[outputNeuronPos];
            double diff = output - target;
            error += diff * diff; // Quadratischer Fehler
        }
        return error / 2.0D; // Division durch 2 ist übliche Konvention für MSE
    }

    /**
     * Aktualisiert die Gewichte und Bias-Werte basierend auf den berechneten Deltas.
     * Wird als Teil des Backpropagation-Algorithmus aufgerufen.
     */
    private void updateWeights() {
        // Aktualisiere alle Bias-Werte und Gewichte

        // 1. Aktualisiere die Bias-Werte in allen Neuronen
        // Input-Neuronen haben normalerweise keinen Bias

        // Hidden Layer Neuronen
        for (final Layer layer : this.hiddenLayerArr) {
            if (!layer.isActiveLayer() && !disableLayerDeactivation) continue; // Überspringe inaktive Layer

            final NeuronInterface[] neurons = layer.getNeuronsArr();
            for (final NeuronInterface neuron : neurons) {
                for (int outputTypePos = 0; outputTypePos < neuron.getNeuronTypeInfoData().getOutputCount(); outputTypePos++) {
                    neuron.setBias(outputTypePos, neuron.getBias(outputTypePos) - this.learningRate * neuron.getDelta(outputTypePos));
                }
            }
        }

        // Output Neuronen
        for (final Neuron neuron : this.outputNeuronArr) {
            for (int outputTypePos = 0; outputTypePos < neuron.getNeuronTypeInfoData().getOutputCount(); outputTypePos++) {
                neuron.setBias(outputTypePos, neuron.getBias(outputTypePos) - this.learningRate * neuron.getDelta(outputTypePos));
            }
        }

        // 2. Aktualisiere alle Synapsengewichte
        for (final Synapse synapse : this.synapseArray) {
            final NeuronInterface targetNeuron = synapse.getTargetNeuron();
            final NeuronInterface sourceNeuron = synapse.getSourceNeuron();

            // Überspringe Synapsen zu deaktivierten Layern
            if (!this.disableLayerDeactivation) {
                if (targetNeuron instanceof Neuron && !((Neuron) targetNeuron).isOutputNeuron()) {
                    final Layer targetLayer = this.findLayerForNeuron(targetNeuron);
                    if (targetLayer != null && !targetLayer.isActiveLayer()) {
                        continue;
                    }
                }
            }

            // Berechne die Gewichtsänderung: delta * Ausgabe der Quelle * Lernrate
            for (int outputTypePos = 0; outputTypePos < sourceNeuron.getNeuronTypeInfoData().getOutputCount(); outputTypePos++) {
                final double value = this.neuronValueFunction.readValue(this, sourceNeuron, outputTypePos);
                final double weightChange = targetNeuron.getDelta(outputTypePos) * value * this.learningRate;
                synapse.setWeight(synapse.getWeight() - weightChange);
            }
        }
    }

    /**
     * Findet das Layer, zu dem ein bestimmtes Neuron gehört.
     *
     * @param searchedNeuron ist das gesuchte Neuron.
     * @return den Layer oder null, wenn das Neuron nicht gefunden wurde.
     */
    private Layer findLayerForNeuron(final NeuronInterface searchedNeuron) {
        for (final Layer layer : this.hiddenLayerArr) {
            for (final NeuronInterface neuron : layer.getNeuronsArr()) {
                if (neuron == searchedNeuron) {
                    return layer;
                }
            }
        }
        return null;
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

    /**
     * Gibt die Aktivierungswerte aller Neuronen im Netzwerk zurück.
     * Dies ist nützlich für die Analyse und das Debugging.
     *
     * @return Eine Map mit den Neuronennamen (IDs) und ihren Aktivierungswerten
     */
    public Map<String, Double> getNeuronActivations() {
        final Map<String, Double> activations = new HashMap<>();

        // Füge Input-Neuronen hinzu
        for (int inputNeuronPos = 0; inputNeuronPos < this.inputNeuronList.length; inputNeuronPos++) {
            final Neuron neuron = this.inputNeuronList[inputNeuronPos];
            for (int outputTypePos = 0; outputTypePos < neuron.getNeuronTypeInfoData().getOutputCount(); outputTypePos++) {
                activations.put("input_" + inputNeuronPos + "_" + outputTypePos, this.neuronValueFunction.readValue(this, neuron, outputTypePos));
            }
        }

        // Füge Hidden-Layer-Neuronen hinzu
        for (int hiddenLayerPos = 0; hiddenLayerPos < this.hiddenLayerArr.length; hiddenLayerPos++) {
            final Layer layer = this.hiddenLayerArr[hiddenLayerPos];
            for (int neuronPos = 0; neuronPos < layer.getNeuronsArr().length; neuronPos++) {
                final NeuronInterface neuron = layer.getNeuronsArr()[neuronPos];
                for (int outputTypePos = 0; outputTypePos < neuron.getNeuronTypeInfoData().getOutputCount(); outputTypePos++) {
                    activations.put("hidden_" + hiddenLayerPos + "_" + neuronPos + "_" + outputTypePos, this.neuronValueFunction.readValue(this, neuron, outputTypePos));
                }
            }
        }

        // Füge Output-Neuronen hinzu
        for (int outputNeuronPos = 0; outputNeuronPos < this.outputNeuronArr.length; outputNeuronPos++) {
            final Neuron neuron = this.outputNeuronArr[outputNeuronPos];
            for (int outputTypePos = 0; outputTypePos < neuron.getNeuronTypeInfoData().getOutputCount(); outputTypePos++) {
                activations.put("output_" + outputNeuronPos + "_" + outputTypePos, this.neuronValueFunction.readValue(this, neuron, outputTypePos));
            }
        }
        return activations;
    }

    // Deserialisierung: Output-Synapsen wiederherstellen
    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        for (Synapse synapse : this.synapseArray) {
            synapse.restoreConnections();
        }
        // Nach der Wiederherstellung der Synapsen: Input-Synapsen-Array und Zähler für alle Neuronen korrekt setzen
        // Alle Neuronen einsammeln
        List<NeuronInterface> allNeurons = new ArrayList<>();
        Collections.addAll(allNeurons, inputNeuronList);
        for (Layer layer : hiddenLayerArr) {
            Collections.addAll(allNeurons, layer.getNeuronsArr());
        }
        Collections.addAll(allNeurons, outputNeuronArr);
        // Für jedes Neuron: Input-Synapsen-Array neu aufbauen
        for (final NeuronInterface neuron : allNeurons) {
            for (int inputTypePos = 0; inputTypePos < neuron.getNeuronTypeInfoData().getInputCount(); inputTypePos++) {
                final List<Synapse> inputSynapseList = new ArrayList<>();
                for (final Synapse synapse : this.synapseArray) {
                    if (synapse.getTargetNeuron() == neuron) {
                        inputSynapseList.add(synapse);
                    }
                }
                neuron.setInputSynapseArr(inputTypePos, inputSynapseList.toArray(new Synapse[0]));
            }
        }
    }

    public void rnnClearPreviousState() {
        // Setze alle Neuronen in den Hidden-Layern zurück
        for (final Layer layer : this.hiddenLayerArr) {
            for (final NeuronInterface neuron : layer.getNeuronsArr()) {
                for (int outputTypePos = 0; outputTypePos < neuron.getNeuronTypeInfoData().getOutputCount(); outputTypePos++) {
                    this.neuronValueFunction.writeValue(this, neuron, outputTypePos, 0.0D);
                }
            }
        }
        // Setze auch die Output-Neuronen zurück
        for (final Neuron neuron : this.outputNeuronArr) {
            for (int outputTypePos = 0; outputTypePos < neuron.getNeuronTypeInfoData().getOutputCount(); outputTypePos++) {
                this.neuronValueFunction.writeValue(this, neuron, outputTypePos, 0.0D);
            }
        }
    }

    public double readNeuronValue(final NeuronInterface neuron, final int outputTypePos) {
        return this.neuronValueFunction.readValue(this, neuron, outputTypePos);
    }

    public void writeNeuronValue(final NeuronInterface neuron, final int outputTypePos, final double value) {
        this.neuronValueFunction.writeValue(this, neuron, outputTypePos, value);
    }

    public NeuronValueFunctionFactory getNeuronValueFunctionFactory() {
        return this.neuronValueFunctionFactory;
    }

    public NeuronValueFunction getNeuronValueFunction() {
        return this.neuronValueFunction;
    }
}
