package de.lifecircles.model.neural;

import de.lifecircles.service.SimulationConfig;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.IOException;

/**
 * Represents the neural network that controls cell behavior.
 */
public class NeuralNetwork implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<Neuron> inputNeuronList;
    private final List<Layer> hiddenLayerList; // Verwende Layer-Objekte statt Listen von Neuronen
    private final List<Neuron> outputNeuronList;
    private final List<Synapse> synapsesynapseList;
    private final Random random = new Random();
    private double[] outputArr;

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
        this.inputNeuronList = new ArrayList<>();
        this.hiddenLayerList = new ArrayList<>();
        this.outputNeuronList = new ArrayList<>();
        this.synapsesynapseList = new ArrayList<>();

        this.fixedHiddenLayerCount = original.fixedHiddenLayerCount;
        
        // Erstelle eine Map, die die Originalneuronen den neuen Neuronen zuordnet
        Map<Neuron, Neuron> neuronMap = new HashMap<>();
        
        // Kopiere Input-Neuronen
        for (Neuron originalNeuron : original.inputNeuronList) {
            Neuron newNeuron = new Neuron();
            newNeuron.setBias(originalNeuron.getBias());
            newNeuron.setActivationFunction(originalNeuron.getActivationFunction());
            newNeuron.setValue(originalNeuron.getValue());
            this.inputNeuronList.add(newNeuron);
            neuronMap.put(originalNeuron, newNeuron);
        }
        
        // Kopiere Hidden Layers
        for (Layer originalLayer : original.hiddenLayerList) {
            Layer newLayer = new Layer();
            newLayer.setActiveLayer(originalLayer.isActiveLayer());
            
            for (Neuron originalNeuron : originalLayer.getNeurons()) {
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
            
            this.hiddenLayerList.add(newLayer);
        }
        
        // Kopiere Output-Neuronen
        for (Neuron originalNeuron : original.outputNeuronList) {
            Neuron newNeuron = new Neuron();
            newNeuron.setBias(originalNeuron.getBias());
            newNeuron.setActivationFunction(originalNeuron.getActivationFunction());
            newNeuron.setValue(originalNeuron.getValue());
            newNeuron.setOutputNeuron(true); // Markiere als Output-Neuron
            this.outputNeuronList.add(newNeuron);
            neuronMap.put(originalNeuron, newNeuron);
        }
        
        // Erstelle das Array für die Ausgabewerte
        this.outputArr = new double[this.outputNeuronList.size()];
        
        // Kopiere alle Synapsen mit den korrekten Verbindungen zwischen den neuen Neuronen
        for (Synapse originalSynapse : original.synapsesynapseList) {
            Neuron sourceNeuron = neuronMap.get(originalSynapse.getSourceNeuron());
            Neuron targetNeuron = neuronMap.get(originalSynapse.getTargetNeuron());
            
            if (sourceNeuron != null && targetNeuron != null) {
                Synapse newSynapse = new Synapse(sourceNeuron, targetNeuron, originalSynapse.getWeight());
                this.synapsesynapseList.add(newSynapse);
            }
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
        this.inputNeuronList = new ArrayList<>();
        this.hiddenLayerList = new ArrayList<>();
        this.outputNeuronList = new ArrayList<>();
        this.synapsesynapseList = new ArrayList<>();

        this.fixedHiddenLayerCount = fixedHiddenLayerCount;

        // create input neurons
        for (int i = 0; i < inputCount; i++) {
            this.inputNeuronList.add(new Neuron());
        }

        // create hidden layers
        for (int count : hiddenCounts) {
            Layer layer = new Layer();
            for (int i = 0; i < count; i++) {
                layer.addNeuron(new Neuron());
            }
            this.hiddenLayerList.add(layer);
        }

        // create output neurons
        for (int i = 0; i < outputCount; i++) {
            Neuron outputNeuron = new Neuron();
            outputNeuron.setOutputNeuron(true); // Markiere als Output-Neuron
            this.outputNeuronList.add(outputNeuron);
        }
        this.outputArr = new double[this.outputNeuronList.size()];

        // connect layers sequentially: input -> hidden1 -> ... -> hiddenN -> output
        List<Neuron> prev = this.inputNeuronList;
        for (Layer layer : this.hiddenLayerList) {
            this.connectLayers(prev, layer.getNeurons(), synapseConnectivity);
            prev = layer.getNeurons();
        }
        this.connectLayers(prev, this.outputNeuronList, synapseConnectivity);
    }

//    /**
//     * Constructs a network with multiple hidden layers with full connectivity.
//     */
//    public NeuralNetwork(int inputCount, int[] hiddenCounts, int outputCount) {
//        this(inputCount, hiddenCounts, outputCount, 1.0);
//    }

    private void connectLayers(List<Neuron> sourceLayer, List<Neuron> targetLayer, double connectivity) {
        // Überprüfe, ob die Konnektivität im gültigen Bereich ist
        connectivity = Math.max(0.0, Math.min(1.0, connectivity));
        
        for (Neuron source : sourceLayer) {
            for (Neuron target : targetLayer) {
                // Entscheide zufällig, ob diese Verbindung erstellt werden soll
                if (connectivity >= 1.0 || this.random.nextDouble() < connectivity) {
                    // Verbesserte Xavier/Glorot-Initialisierung
                    double limit = Math.sqrt(6.0 / (this.getInputLayerSize() + this.getOutputLayerSize()));
                    final double weight = this.random.nextDouble() * 2.0D * limit - limit;
                    this.synapsesynapseList.add(new Synapse(source, target, weight));
                }
            }
        }
    }

    /**
     * Sets the input values for the network.
     */
    public void setInputs(double[] inputs) {
        if (inputs.length != this.inputNeuronList.size()) {
            throw new IllegalArgumentException("Input size mismatch");
        }
        for (int inputPos = 0; inputPos < inputs.length; inputPos++) {
            this.inputNeuronList.get(inputPos).setValue(inputs[inputPos]);
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
                for (final Neuron neuron : layer.getNeurons()) {
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
            for (int layerPos = this.fixedHiddenLayerCount; layerPos < hiddenLayerList.size(); layerPos++) {
                final Layer layer = this.hiddenLayerList.get(layerPos);
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
        int totalLayers = 2 + hiddenLayerList.size(); // Input + Hidden + Output
        int[] sizes = new int[totalLayers];
        
        // Input Layer
        sizes[0] = inputNeuronList.size();
        
        // Hidden Layers
        for (int i = 0; i < hiddenLayerList.size(); i++) {
            sizes[i + 1] = hiddenLayerList.get(i).getNeurons().size();
        }
        
        // Output Layer
        sizes[totalLayers - 1] = outputNeuronList.size();
        
        return sizes;
    }

    public double getOutputValue(final int outputNeuronPos) {
        return this.outputNeuronList.get(outputNeuronPos).getValue();
    }

    /**
     * Returns the number of input neurons in the network.
     * @return the count of input neurons
     */
    public int getInputCount() {
        return this.inputNeuronList.size();
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
        
        for (Synapse synapse : mutated.synapsesynapseList) {
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
            for (Neuron neuron : layer.getNeurons()) {
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
        for (int i = 0; i < this.inputNeuronList.size(); i++) {
            mapping.put(this.inputNeuronList.get(i), target.inputNeuronList.get(i));
        }
        
        // Hidden-Layer-Neuronen zuordnen
        for (int i = 0; i < this.hiddenLayerList.size(); i++) {
            List<Neuron> sourceLayer = this.hiddenLayerList.get(i).getNeurons();
            List<Neuron> targetLayer = target.hiddenLayerList.get(i).getNeurons();
            for (int j = 0; j < sourceLayer.size(); j++) {
                mapping.put(sourceLayer.get(j), targetLayer.get(j));
            }
        }
        
        // Output-Neuronen zuordnen
        for (int i = 0; i < this.outputNeuronList.size(); i++) {
            mapping.put(this.outputNeuronList.get(i), target.outputNeuronList.get(i));
            // Stelle sicher, dass Output-Neuronen richtig markiert sind
            target.outputNeuronList.get(i).setOutputNeuron(true);
        }
        
        return mapping;
    }

    // Structural mutation helpers
    private void applyStructuralMutations(double mutationRate) {
        // Structural mutations should be much less frequent than weight/bias mutations
        double structuralMutationRate = mutationRate * DEFAULT_MUTATION_RATE; // Only 10% of the regular mutation rate
        
        // Möglichkeit, ein komplettes Hidden Layer hinzuzufügen
        if (this.random.nextDouble() < structuralMutationRate * 1.5) { // Erhöhte Wahrscheinlichkeit
            final int pos = random.nextInt((this.hiddenLayerList.size() - this.fixedHiddenLayerCount) + 1) +
                    this.fixedHiddenLayerCount;
            //int pos = this.random.nextInt(this.hiddenLayerList.size() + 1);
            // Zufällige Neuronenzahl zwischen 1 und 5
            int neuronCount = 1 + this.random.nextInt(5);
            this.addHiddenLayer(pos, neuronCount, structuralMutationRate);
        }

        // Möglichkeit, ein Hidden Layer zu entfernen
        if (this.random.nextDouble() < structuralMutationRate * 1.5) { // Erhöhte Wahrscheinlichkeit
            final int pos = random.nextInt((this.hiddenLayerList.size() - this.fixedHiddenLayerCount)) +
                    this.fixedHiddenLayerCount;
            this.removeHiddenLayer(pos);
        }

        // Wahrscheinlichkeit, ein Neuron zu einem bestehenden Layer hinzuzufügen
        if (!this.hiddenLayerList.isEmpty() &&
                (this.hiddenLayerList.size() >= this.fixedHiddenLayerCount) &&
                this.random.nextDouble() < structuralMutationRate * 2.0D) { // Verdoppelte Wahrscheinlichkeit
            int li = this.random.nextInt(this.hiddenLayerList.size() - this.fixedHiddenLayerCount) + this.fixedHiddenLayerCount;
            this.addNeuronToHiddenLayer(li, structuralMutationRate);
        }

        // Wahrscheinlichkeit, ein Neuron zu entfernen
        if (!this.hiddenLayerList.isEmpty() && this.random.nextDouble() < structuralMutationRate) {
            int li = random.nextInt(this.hiddenLayerList.size());
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
        if (!this.hiddenLayerList.isEmpty() &&
                (this.random.nextDouble() < structuralMutationRate)) {
            // Wähle ein zufälliges Hidden Layer
            List<Neuron> layer = this.hiddenLayerList.get(random.nextInt(this.hiddenLayerList.size())).getNeurons();
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
        List<Neuron> prevLayer = index == 0 ? this.inputNeuronList : this.hiddenLayerList.get(index - 1).getNeurons();
        List<Neuron> nextLayer = index == this.hiddenLayerList.size() ? this.outputNeuronList : this.hiddenLayerList.get(index).getNeurons();
        
        // Füge den neuen Layer ein
        this.hiddenLayerList.add(index, newLayer);
        
        // Verbinde den vorherigen Layer mit dem neuen Layer
        for (Neuron srcNeuron : prevLayer) {
            for (Neuron tgtNeuron : newLayer.getNeurons()) {
                // Erstelle Synapse nur mit der angegebenen Wahrscheinlichkeit
                if (connectivity >= 1.0D || this.random.nextDouble() < connectivity) {
                    double weight = this.random.nextDouble() * 0.002D - 0.001D;
                    synapsesynapseList.add(new Synapse(srcNeuron, tgtNeuron, weight));
                }
            }
        }
        
        // Verbinde den neuen Layer mit dem nächsten Layer
        for (Neuron srcNeuron : newLayer.getNeurons()) {
            for (Neuron tgtNeuron : nextLayer) {
                // Erstelle Synapse nur mit der angegebenen Wahrscheinlichkeit
                if (connectivity >= 1.0D || this.random.nextDouble() < connectivity) {
                    double weight = this.random.nextDouble() * 0.002D - 0.001D;
                    this.synapsesynapseList.add(new Synapse(srcNeuron, tgtNeuron, weight));
                }
            }
        }
    }
    
    /**
     * Overloaded method that adds a hidden layer with full connectivity.
     */
    public void addHiddenLayer(int index, int neuronCount) {
        addHiddenLayer(index, neuronCount, 1.0);
    }

    public void addNeuronToHiddenLayer(final int layerIndex, final double connectivity) {
        Neuron newN = new Neuron();
        this.hiddenLayerList.get(layerIndex).addNeuron(newN);
        List<Neuron> prev = layerIndex == 0 ? this.inputNeuronList : this.hiddenLayerList.get(layerIndex - 1).getNeurons();
        for (Neuron srcNeuron : prev) {
            // Erstelle Synapse nur mit der angegebenen Wahrscheinlichkeit
            if (connectivity >= 1.0 || this.random.nextDouble() < connectivity) {
                double weight = this.random.nextDouble() * 0.002D - 0.001D;
                this.synapsesynapseList.add(new Synapse(srcNeuron, newN, weight));
            }
        }
        List<Neuron> next = layerIndex == this.hiddenLayerList.size() - 1 ? this.outputNeuronList : this.hiddenLayerList.get(layerIndex + 1).getNeurons();
        for (Neuron tgtNeuron : next) {
            // Erstelle Synapse nur mit der angegebenen Wahrscheinlichkeit
            if (connectivity >= 1.0 || this.random.nextDouble() < connectivity) {
                double weight = this.random.nextDouble() * 0.002D - 0.001D;
                this.synapsesynapseList.add(new Synapse(newN, tgtNeuron, weight));
            }
        }
    }

    private void removeHiddenLayer(final int pos) {
        final Layer layer = this.hiddenLayerList.get(pos);
        if (layer.getNeurons().size() == 1) {
            this.removeNeuron(layer, 0);
        }
    }

    public void removeNeuronFromHiddenLayer(final int layerIndex) {
        final Layer layer = this.hiddenLayerList.get(layerIndex);
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
        final Iterator<Synapse> synapseIterator = this.synapsesynapseList.iterator();
        while (synapseIterator.hasNext()) {
            final Synapse synapse = synapseIterator.next();
            if (synapse.getSourceNeuron() == removedNeuron || synapse.getTargetNeuron() == removedNeuron) {
                synapse.getSourceNeuron().getOutputSynapses().remove(synapse);
                // Für Input-Synapsen die neue Methode verwenden
                if (synapse.getTargetNeuron() == removedNeuron) {
                    synapseIterator.remove();
                    continue;
                }
                synapse.getTargetNeuron().removeInputSynapse(synapse);
                synapseIterator.remove();
            }
        }
    }

    public void addRandomSynapse() {
        // Wählen Sie zufällige Quelle und Ziel-Layer
        List<List<Neuron>> allLayers = new ArrayList<>();
        allLayers.add(this.inputNeuronList);
        allLayers.addAll(this.hiddenLayerList.stream().map(Layer::getNeurons).collect(Collectors.toList()));
        allLayers.add(this.outputNeuronList);
        
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
        this.synapsesynapseList.add(synapse);
    }

    public void removeRandomSynapse() {
        if (this.synapsesynapseList.isEmpty()) return;
        
        Synapse rem = this.synapsesynapseList.get(random.nextInt(this.synapsesynapseList.size()));
        rem.getSourceNeuron().getOutputSynapses().remove(rem);
        rem.getTargetNeuron().removeInputSynapse(rem);
        this.synapsesynapseList.remove(rem);
    }

    // Expose the number of synapses for energy cost calculation
    public int getSynapseCount() {
        return this.synapsesynapseList.size();
    }

    public List<Synapse> getSynapsesynapseList() {
        return this.synapsesynapseList;
    }

    /**
     * Returns the total count of all neurons in the network.
     * This is more efficient than creating a list of all neurons first.
     *
     * @return the total number of neurons in the network
     */
    public int getAllNeuronsSize() {
        int count = this.inputNeuronList.size() + this.outputNeuronList.size();

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
            int pos = random.nextInt(mutated.hiddenLayerList.size() + 1);
            int neuronCount = 2 + random.nextInt(4); // 2-5 Neuronen
            mutated.addHiddenLayer(pos, neuronCount);
        }

        // 50% Chance für mehrere zusätzliche Neuronen
        if (this.random.nextDouble() < 0.5 && !mutated.hiddenLayerList.isEmpty()) {
            int count = 1 + this.random.nextInt(3); // 1-3 neue Neuronen
            for (int i = 0; i < count; i++) {
                int layer = this.random.nextInt(mutated.hiddenLayerList.size());
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
        return this.inputNeuronList.size();
    }

    public double getOutputLayerSize() {
        return this.outputNeuronList.size();
    }

    /**
     * Gibt die Liste der Input-Neuronen zurück
     * @return Liste der Input-Neuronen
     */
    public List<Neuron> getInputNeuronList() {
        return inputNeuronList;
    }

    /**
     * Gibt die Liste der Hidden-Layer zurück
     * @return Liste der Hidden-Layer
     */
    public List<Layer> getHiddenLayerList() {
        return hiddenLayerList;
    }

    /**
     * Gibt die Liste der Output-Neuronen zurück
     * @return Liste der Output-Neuronen
     */
    public List<Neuron> getOutputNeuronList() {
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

    /**
     * Stellt nach der Deserialisierung die transiente outputSynapses-Liste in allen Neuronen wieder her.
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        // Stelle die Output-Synapsen in allen Neuronen wieder her
        for (Synapse synapse : synapsesynapseList) {
            synapse.restoreConnections();
        }
    }
}

