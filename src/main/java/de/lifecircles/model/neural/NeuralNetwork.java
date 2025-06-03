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
    private Layer[] hiddenLayerList; // Array statt List
    private final Neuron[] outputNeuronList;
    // Synapsen als Array statt List
    private Synapse[] synapseArray;
    private final Random random = new Random();
    private double[] outputArr;

    private static final int INITIAL_SYNAPSE_CAPACITY = 64;
    private static final double DEFAULT_MUTATION_RATE = 0.1D;
    private int fixedHiddenLayerCount = SimulationConfig.CELL_STATE_ACTIVE_LAYER_COUNT;

    // Flag zum Deaktivieren der Layer-Deaktivierung (z.B. für Tests)
    private transient boolean disableLayerDeactivation = false;

    private long proccessedSynapses = 0L;

    /**
     * Copy-Konstruktor: Erstellt eine exakte Kopie des übergebenen neuronalen Netzwerks
     * 
     * @param original Das zu kopierende neuronale Netzwerk
     */
    public NeuralNetwork(NeuralNetwork original) {
        this.inputNeuronList = new Neuron[original.inputNeuronList.length];
        this.outputNeuronList = new Neuron[original.outputNeuronList.length];
        // Synapsen-Array initialisieren
        this.synapseArray = new Synapse[original.getSynapseList().size()];

        this.fixedHiddenLayerCount = original.fixedHiddenLayerCount;
        
        // Erstelle eine Map, die die Originalneuronen den neuen Neuronen zuordnet
        Map<Neuron, Neuron> neuronMap = new HashMap<>();
        
        // Kopiere Input-Neuronen
        for (int i = 0; i < original.inputNeuronList.length; i++) {
            Neuron originalNeuron = original.inputNeuronList[i];
            Neuron newNeuron = new Neuron();
            newNeuron.setBias(originalNeuron.getBias());
            newNeuron.setActivationFunction(originalNeuron.getActivationFunction());
            newNeuron.setValue(originalNeuron.getValue());
            this.inputNeuronList[i] = newNeuron;
            neuronMap.put(originalNeuron, newNeuron);
        }
        
        // Kopiere Hidden Layers
        this.hiddenLayerList = new Layer[original.hiddenLayerList.length];
        for (int i = 0; i < original.hiddenLayerList.length; i++) {
            Layer originalLayer = original.hiddenLayerList[i];
            Layer newLayer = new Layer();
            newLayer.setActiveLayer(originalLayer.isActiveLayer());

            for (Neuron originalNeuron : originalLayer.getNeuronsArray()) {
                Neuron newNeuron = new Neuron();
                newNeuron.setBias(originalNeuron.getBias());
                newNeuron.setActivationFunction(originalNeuron.getActivationFunction());
                if (newLayer.isActiveLayer()) {
                    newNeuron.setValue(originalNeuron.getValue());
                } else {
                    newNeuron.setValue(0.0D);
                }
                newLayer.addNeuron(newNeuron);
                neuronMap.put(originalNeuron, newNeuron);
            }
            this.hiddenLayerList[i] = newLayer;
        }
        
        // Kopiere Output-Neuronen
        for (int i = 0; i < original.outputNeuronList.length; i++) {
            Neuron originalNeuron = original.outputNeuronList[i];
            Neuron newNeuron = new Neuron();
            newNeuron.setBias(originalNeuron.getBias());
            newNeuron.setActivationFunction(originalNeuron.getActivationFunction());
            newNeuron.setValue(originalNeuron.getValue());
            newNeuron.setOutputNeuron(true); // Markiere als Output-Neuron
            this.outputNeuronList[i] = newNeuron;
            neuronMap.put(originalNeuron, newNeuron);
        }
        
        // Erstelle das Array für die Ausgabewerte
        this.outputArr = new double[this.outputNeuronList.length];

        // Kopiere alle Synapsen mit den korrekten Verbindungen zwischen den neuen Neuronen
        int synapseIndex = 0;
        for (int i = 0; i < original.getSynapseList().size(); i++) {
            Synapse originalSynapse = original.getSynapseList().get(i);
            Neuron sourceNeuron = neuronMap.get(originalSynapse.getSourceNeuron());
            Neuron targetNeuron = neuronMap.get(originalSynapse.getTargetNeuron());
            if (sourceNeuron != null && targetNeuron != null) {
                Synapse newSynapse = new Synapse(sourceNeuron, targetNeuron, originalSynapse.getWeight());
                this.synapseArray[synapseIndex++] = newSynapse;
            }
        }
        if (synapseIndex < this.synapseArray.length) {
            Synapse[] resized = new Synapse[synapseIndex];
            System.arraycopy(this.synapseArray, 0, resized, 0, synapseIndex);
            this.synapseArray = resized;
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
    public NeuralNetwork(int inputCount, int hiddenCount, int outputCount, double synapseConnectivity, final int fixedHiddenLayerCount) {
        this(inputCount, new int[]{hiddenCount}, outputCount, synapseConnectivity, fixedHiddenLayerCount);
    }

    /**
     * Constructs a network with a single hidden layer with full connectivity.
     */
    public NeuralNetwork(int inputCount, int hiddenCount, int outputCount) {
        this(inputCount, new int[]{hiddenCount}, outputCount, 1.0, 0);
    }

    /**
     * Constructs a network with multiple hidden layers.
     *
     * @param inputCount number of input neurons
     * @param hiddenCounts sizes of each hidden layer
     * @param outputCount number of output neurons
     * @param synapseConnectivity Prozentsatz der zu erstellenden Synapsen (0.0-1.0)
     */
    public NeuralNetwork(int inputCount, int[] hiddenCounts, int outputCount, double synapseConnectivity, final int fixedHiddenLayerCount) {
        this.inputNeuronList = new Neuron[inputCount];
        this.outputNeuronList = new Neuron[outputCount];
        // Synapsen-Array initialisieren
        this.synapseArray = new Synapse[0];

        this.fixedHiddenLayerCount = fixedHiddenLayerCount;

        // create input neurons
        for (int i = 0; i < inputCount; i++) {
            this.inputNeuronList[i] = new Neuron();
        }

        // create hidden layers
        this.hiddenLayerList = new Layer[hiddenCounts.length];
        for (int i = 0; i < hiddenCounts.length; i++) {
            int count = hiddenCounts[i];
            Layer layer = new Layer();
            for (int j = 0; j < count; j++) {
                layer.addNeuron(new Neuron());
            }
            this.hiddenLayerList[i] = layer;
        }

        // create output neurons
        for (int i = 0; i < outputCount; i++) {
            Neuron outputNeuron = new Neuron();
            outputNeuron.setOutputNeuron(true); // Markiere als Output-Neuron
            this.outputNeuronList[i] = outputNeuron;
        }
        this.outputArr = new double[this.outputNeuronList.length];

        // connect layers sequentially: input -> hidden1 -> ... -> hiddenN -> output
        Neuron[] prev = this.inputNeuronList;
        for (Layer layer : this.hiddenLayerList) {
            this.connectLayers(prev, layer.getNeuronsArray(), synapseConnectivity);
            prev = layer.getNeuronsArray();
        }
        this.connectLayers(prev, this.outputNeuronList, synapseConnectivity);
    }

    private void connectLayers(Neuron[] sourceLayer, Neuron[] targetLayer, double connectivity) {
        connectivity = Math.max(0.0, Math.min(1.0, connectivity));
        for (Neuron source : sourceLayer) {
            for (Neuron target : targetLayer) {
                if (connectivity >= 1.0 || this.random.nextDouble() < connectivity) {
                    double limit = Math.sqrt(6.0 / (this.getInputLayerSize() + this.getOutputLayerSize()));
                    final double weight = this.random.nextDouble() * 2.0D * limit - limit;
                    addSynapse(new Synapse(source, target, weight));
                }
            }
        }
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
    public void setInputs(double[] inputs) {
        if (inputs.length != this.inputNeuronList.length) {
            throw new IllegalArgumentException("Input size mismatch");
        }
        for (int inputPos = 0; inputPos < inputs.length; inputPos++) {
            this.inputNeuronList[inputPos].setValue(inputs[inputPos]);
        }
    }

    /**
     * Processes the inputs through the network and returns the outputs.
     */
    public double[] process() {
        this.proccessedSynapses = 0L;

        // process hidden layers
        for (final Layer layer : this.hiddenLayerList) {
            if (layer.isActiveLayer()) {
                for (final Neuron neuron : layer.getNeuronsArray()) {
                    this.proccessedSynapses += neuron.activate();
                }
            }
        }

        // process output layer
        for (Neuron neuron : this.outputNeuronList) {
            this.proccessedSynapses += neuron.activate();
        }

        // Collect outputs
        for (int outputNeuronPos = 0; outputNeuronPos < this.outputArr.length; outputNeuronPos++) {
            this.outputArr[outputNeuronPos] = this.getOutputValue(outputNeuronPos);
        }

        // process hidden layer activation counters (ohne fixed layers)
        if (!disableLayerDeactivation) {
            for (int layerPos = this.fixedHiddenLayerCount; layerPos < hiddenLayerList.length; layerPos++) {
                final Layer layer = this.hiddenLayerList[layerPos];
                final List<Neuron> neuronList = layer.getNeurons();
                if (neuronList.size() > 0) {
                    final Neuron neuron = neuronList.get(0);
                    final double actValue = Math.max(1.0D / 1000,
                            layer.getActivationCounter() + neuron.getValue());
                    if (actValue >= 1.0D) {
                        layer.setActivationCounter(0.0D);
                        layer.setActiveLayer(true);
                    } else {
                        layer.setActivationCounter(actValue);
                        layer.setActiveLayer(false);
                    }
                }
            }
        } else {
            // Wenn deaktiviert: alle Layer aktiv lassen
            for (Layer layer : this.hiddenLayerList) {
                layer.setActiveLayer(true);
            }
        }

        return this.outputArr;
    }

    /**
     * Gibt die Größen aller Layer (Input, Hidden, Output) im Netzwerk zurück
     * @return Array mit den Größen der einzelnen Layer
     */
    public int[] getLayerSizes() {
        int totalLayers = 2 + hiddenLayerList.length; // Input + Hidden + Output
        int[] sizes = new int[totalLayers];
        
        // Input Layer
        sizes[0] = inputNeuronList.length;

        // Hidden Layers
        for (int i = 0; i < hiddenLayerList.length; i++) {
            sizes[i + 1] = hiddenLayerList[i].getNeuronsArray().length;
        }
        
        // Output Layer
        sizes[totalLayers - 1] = outputNeuronList.length;

        return sizes;
    }

    public double getOutputValue(final int outputNeuronPos) {
        return this.outputNeuronList[outputNeuronPos].getValue();
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
    public NeuralNetwork mutate(double mutationRate, double mutationStrength) {
        // Verwende den Copy-Konstruktor, um eine exakte Kopie des Netzwerks zu erstellen
        NeuralNetwork mutated = new NeuralNetwork(this);

        // Mutate weights and biases
        mutated.applyToAllNeurons(neuron -> {
            if (Math.random() < mutationRate) {
                double mutation = (Math.random() * 2.0D - 1.0D) * mutationStrength;
                neuron.setBias(neuron.getBias() + mutation);
            }
        });
        
        for (Synapse synapse : mutated.synapseArray) {
            if (Math.random() < mutationRate) {
                double mutation = (Math.random() * 2.0D - 1.0D) * mutationStrength;
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
    public void applyToAllNeurons(Consumer<Neuron> function) {
        // Anwenden auf Input-Neuronen
        for (Neuron neuron : this.inputNeuronList) {
            function.accept(neuron);
        }
        
        // Anwenden auf Hidden-Layer-Neuronen
        for (Layer layer : this.hiddenLayerList) {
            for (Neuron neuron : layer.getNeuronsArray()) {
                function.accept(neuron);
            }
        }
        
        // Anwenden auf Output-Neuronen
        for (Neuron neuron : this.outputNeuronList) {
            function.accept(neuron);
        }
    }

    /**
     * Erstellt eine Mapping-Tabelle, die jedem Neuron des Quellnetzwerks
     * das entsprechende Neuron im Zielnetzwerk zuordnet.
     */
    private Map<Neuron, Neuron> createNeuronMapping(NeuralNetwork target) {
        Map<Neuron, Neuron> mapping = new HashMap<>();
        
        // Input-Neuronen zuordnen
        for (int i = 0; i < this.inputNeuronList.length; i++) {
            mapping.put(this.inputNeuronList[i], target.inputNeuronList[i]);
        }
        
        // Hidden-Layer-Neuronen zuordnen
        for (int i = 0; i < this.hiddenLayerList.length; i++) {
            Neuron[] sourceLayer = this.hiddenLayerList[i].getNeuronsArray();
            Neuron[] targetLayer = target.hiddenLayerList[i].getNeuronsArray();
            for (int j = 0; j < sourceLayer.length; j++) {
                mapping.put(sourceLayer[j], targetLayer[j]);
            }
        }
        
        // Output-Neuronen zuordnen
        for (int i = 0; i < this.outputNeuronList.length; i++) {
            mapping.put(this.outputNeuronList[i], target.outputNeuronList[i]);
            // Stelle sicher, dass Output-Neuronen richtig markiert sind
            target.outputNeuronList[i].setOutputNeuron(true);
        }
        
        return mapping;
    }

    // Structural mutation helpers
    private void applyStructuralMutations(double mutationRate) {
        // Structural mutations should be much less frequent than weight/bias mutations
        double structuralMutationRate = mutationRate * DEFAULT_MUTATION_RATE; // Only 10% of the regular mutation rate
        
        // Möglichkeit, ein komplettes Hidden Layer hinzuzufügen
        int addLayerRange = (this.hiddenLayerList.length - this.fixedHiddenLayerCount) + 1;
        if (addLayerRange > 0 && this.random.nextDouble() < structuralMutationRate * 1.5) { // Erhöhte Wahrscheinlichkeit
            final int pos = random.nextInt(addLayerRange) + this.fixedHiddenLayerCount;
            // Zufällige Neuronenzahl zwischen 1 und 5
            int neuronCount = 1 + this.random.nextInt(5);
            this.addHiddenLayer(pos, neuronCount, structuralMutationRate);
        }

        // Möglichkeit, ein Hidden Layer zu entfernen
        int removeLayerRange = (this.hiddenLayerList.length - this.fixedHiddenLayerCount);
        if (removeLayerRange > 0 && this.random.nextDouble() < structuralMutationRate * 1.5) { // Erhöhte Wahrscheinlichkeit
            final int pos = random.nextInt(removeLayerRange) + this.fixedHiddenLayerCount;
            this.removeHiddenLayer(pos);
        }

        // Wahrscheinlichkeit, ein Neuron zu einem bestehenden Layer hinzuzufügen
        int addNeuronLayerCount = this.hiddenLayerList.length - this.fixedHiddenLayerCount;
        if (addNeuronLayerCount > 0 &&
                this.random.nextDouble() < structuralMutationRate * 2.0D) { // Verdoppelte Wahrscheinlichkeit
            int li = this.random.nextInt(addNeuronLayerCount) + this.fixedHiddenLayerCount;
            this.addNeuronToHiddenLayer(li, structuralMutationRate);
        }

        // Wahrscheinlichkeit, ein Neuron zu entfernen
        if (this.hiddenLayerList.length > 0 && this.random.nextDouble() < structuralMutationRate) {
            int li = random.nextInt(this.hiddenLayerList.length);
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
        if (this.hiddenLayerList.length > 0 &&
                (this.random.nextDouble() < structuralMutationRate)) {
            // Wähle ein zufälliges Hidden Layer
            List<Neuron> layer = this.hiddenLayerList[random.nextInt(this.hiddenLayerList.length)].getNeurons();
            if (!layer.isEmpty()) {
                Neuron neuron = layer.get(random.nextInt(layer.size()));
                // Wähle eine zufällige Aktivierungsfunktion
                ActivationFunction[] functions = ActivationFunction.values();
                neuron.setActivationFunction(functions[random.nextInt(functions.length)]);
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
    public void addHiddenLayer(int index, int neuronCount, double connectivity) {
        // Begrenze die Konnektivität auf gültige Werte
        connectivity = Math.max(0.0, Math.min(1.0, connectivity));
        
        final Layer newLayer = new Layer();
        newLayer.setActiveLayer(true); // Set new layer as active by default
        
        // Erstelle die neuen Neuronen
        for (int i = 0; i < neuronCount; i++) {
            newLayer.addNeuron(new Neuron());
        }
        
        // Bestimme die angrenzenden Layer
        List<Neuron> prevLayer = index == 0 ? Arrays.asList(this.inputNeuronList) : this.hiddenLayerList[index - 1].getNeurons();
        List<Neuron> nextLayer = index == this.hiddenLayerList.length ? Arrays.asList(this.outputNeuronList) : this.hiddenLayerList[index].getNeurons();

        // Füge den neuen Layer ein
        this.hiddenLayerList = insertLayer(this.hiddenLayerList, index, newLayer);

        // Verbinde den vorherigen Layer mit dem neuen Layer
        for (Neuron srcNeuron : prevLayer) {
            for (Neuron tgtNeuron : newLayer.getNeuronsArray()) {
                if (connectivity >= 1.0D || this.random.nextDouble() < connectivity) {
                    double weight = this.random.nextDouble() * 0.002D - 0.001D;
                    addSynapse(new Synapse(srcNeuron, tgtNeuron, weight));
                }
            }
        }
        
        // Verbinde den neuen Layer mit dem nächsten Layer
        for (Neuron srcNeuron : newLayer.getNeuronsArray()) {
            for (Neuron tgtNeuron : nextLayer) {
                if (connectivity >= 1.0D || this.random.nextDouble() < connectivity) {
                    double weight = this.random.nextDouble() * 0.002D - 0.001D;
                    addSynapse(new Synapse(srcNeuron, tgtNeuron, weight));
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
        Neuron newN = new Neuron();
        this.hiddenLayerList[layerIndex].addNeuron(newN);
        List<Neuron> prev = layerIndex == 0 ? Arrays.asList(this.inputNeuronList) : this.hiddenLayerList[layerIndex - 1].getNeurons();
        for (Neuron srcNeuron : prev) {
            if (connectivity >= 1.0 || this.random.nextDouble() < connectivity) {
                double weight = this.random.nextDouble() * 0.002D - 0.001D;
                addSynapse(new Synapse(srcNeuron, newN, weight));
            }
        }
        List<Neuron> next = layerIndex == this.hiddenLayerList.length - 1 ? Arrays.asList(this.outputNeuronList) : this.hiddenLayerList[layerIndex + 1].getNeurons();
        for (Neuron tgtNeuron : next) {
            if (connectivity >= 1.0 || this.random.nextDouble() < connectivity) {
                double weight = this.random.nextDouble() * 0.002D - 0.001D;
                addSynapse(new Synapse(newN, tgtNeuron, weight));
            }
        }
    }

    private void removeHiddenLayer(final int pos) {
        // Entferne Layer aus Array
        this.hiddenLayerList = removeLayer(this.hiddenLayerList, pos);
    }

    // Hilfsmethode zum Entfernen eines Layers aus einem Array
    private static Layer[] removeLayer(Layer[] arr, int index) {
        Layer[] result = new Layer[arr.length - 1];
        System.arraycopy(arr, 0, result, 0, index);
        System.arraycopy(arr, index + 1, result, index, arr.length - index - 1);
        return result;
    }

    public void removeNeuronFromHiddenLayer(final int layerIndex) {
        final Layer layer = this.hiddenLayerList[layerIndex];
        final List<Neuron> layerNeuronList = layer.getNeurons();
        if (layerNeuronList.size() > 1) {
            // Skip the first neuron to avoid removing activation neurons
            final int idx = this.random.nextInt(layerNeuronList.size() - 1) + 1;
            this.removeNeuron(layer, idx);
        }
    }

    private void removeNeuron(final Layer layer, final int idx) {
        final List<Neuron> layerNeuronList = layer.getNeurons();
        final Neuron removedNeuron = layerNeuronList.remove(idx);
        int i = 0;
        while (i < synapseArray.length) {
            final Synapse synapse = synapseArray[i];
            if (synapse.getSourceNeuron() == removedNeuron || synapse.getTargetNeuron() == removedNeuron) {
                synapse.getSourceNeuron().getOutputSynapses().remove(synapse);
                if (synapse.getTargetNeuron() == removedNeuron) {
                    // Entferne Synapse
                    removeSynapseAt(i);
                    continue;
                }
                synapse.getTargetNeuron().removeInputSynapse(synapse);
                removeSynapseAt(i);
            } else {
                i++;
            }
        }
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
        List<List<Neuron>> allLayers = new ArrayList<>();
        allLayers.add(Arrays.asList(this.inputNeuronList));
        for (Layer l : this.hiddenLayerList) allLayers.add(l.getNeurons());
        allLayers.add(Arrays.asList(this.outputNeuronList));

        int sourceLayerIndex = this.random.nextInt(allLayers.size());
        // Exclude the input layer for the target.
        int targetLayerIndex = this.random.nextInt(allLayers.size() - 1) + 1;
        
        // Make sure we're not connecting within the same layer
        if (sourceLayerIndex == targetLayerIndex) {
            targetLayerIndex = (sourceLayerIndex + 1) % allLayers.size();
        }
        
        List<Neuron> sourceLayer = allLayers.get(sourceLayerIndex);
        List<Neuron> targetLayer = allLayers.get(targetLayerIndex);
        
        if (sourceLayer.isEmpty() || targetLayer.isEmpty()) return;
        
        // Wählen Sie zufällige Neuronen aus den beiden Layern
        Neuron sourceNeuron = sourceLayer.get(this.random.nextInt(sourceLayer.size()));
        Neuron targetNeuron = targetLayer.get(this.random.nextInt(targetLayer.size()));
        
        // Erstellen Sie die Synapse
        Synapse synapse = new Synapse(sourceNeuron, targetNeuron, Math.random() * 0.002D - 0.001D);
        addSynapse(synapse);
    }

    public void removeRandomSynapse() {
        if (this.synapseArray.length == 0) return;
        int idx = random.nextInt(this.synapseArray.length);
        Synapse rem = this.synapseArray[idx];
        rem.getSourceNeuron().getOutputSynapses().remove(rem);
        rem.getTargetNeuron().removeInputSynapse(rem);
        removeSynapseAt(idx);
    }

    // Expose the number of synapses for energy cost calculation
    public int getSynapseCount() {
        return this.synapseArray.length;
    }

    public List<Synapse> getSynapseList() {
        // Gibt eine unveränderliche Liste der Synapsen zurück
        return Collections.unmodifiableList(Arrays.asList(this.synapseArray));
    }

    /**
     * Returns the total count of all neurons in the network.
     * This is more efficient than creating a list of all neurons first.
     *
     * @return the total number of neurons in the network
     */
    public int getAllNeuronsSize() {
        int count = this.inputNeuronList.length + this.outputNeuronList.length;

        for (Layer layer : this.hiddenLayerList) {
            count += layer.getNeurons().size();
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
            int pos = random.nextInt(mutated.hiddenLayerList.length + 1);
            int neuronCount = 2 + random.nextInt(4); // 2-5 Neuronen
            mutated.addHiddenLayer(pos, neuronCount, 1.0);
        }

        // 50% Chance für mehrere zusätzliche Neuronen
        if (this.random.nextDouble() < 0.5 && !mutated.hiddenLayerList.equals(null)) {
            int count = 1 + this.random.nextInt(3); // 1-3 neue Neuronen
            for (int i = 0; i < count; i++) {
                int layer = this.random.nextInt(mutated.hiddenLayerList.length);
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

    public double getInputLayerSize() {
        return this.inputNeuronList.length;
    }

    public double getOutputLayerSize() {
        return this.outputNeuronList.length;
    }

    /**
     * Gibt das Array der Input-Neuronen zurück
     * @return Array der Input-Neuronen
     */
    public Neuron[] getInputNeuronList() {
        return inputNeuronList;
    }

    /**
     * Gibt die Liste der Hidden-Layer zurück
     * @return Liste der Hidden-Layer
     */
    public Layer[] getHiddenLayerList() {
        return hiddenLayerList;
    }

    /**
     * Gibt die Liste der Output-Neuronen zurück
     * @return Liste der Output-Neuronen
     */
    public Neuron[] getOutputNeuronList() {
        return outputNeuronList;
    }

    public long getProccessedSynapses() {
        return this.proccessedSynapses;
    }

    /**
     * Aktiviert oder deaktiviert die Layer-Deaktivierung (z.B. für Tests)
     */
    public void setDisableLayerDeactivation(boolean disable) {
        this.disableLayerDeactivation = disable;
    }

    public boolean isDisableLayerDeactivation() {
        return this.disableLayerDeactivation;
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
        List<Neuron> allNeurons = new ArrayList<>();
        Collections.addAll(allNeurons, inputNeuronList);
        for (Layer layer : hiddenLayerList) {
            Collections.addAll(allNeurons, layer.getNeuronsArray());
        }
        Collections.addAll(allNeurons, outputNeuronList);
        // Für jedes Neuron: Input-Synapsen-Array neu aufbauen
        for (Neuron neuron : allNeurons) {
            List<Synapse> inputList = new ArrayList<>();
            for (Synapse s : this.synapseArray) {
                if (s.getTargetNeuron() == neuron) {
                    inputList.add(s);
                }
            }
            neuron.setInputSynapses(inputList.toArray(new Synapse[0]));
        }
    }
}
