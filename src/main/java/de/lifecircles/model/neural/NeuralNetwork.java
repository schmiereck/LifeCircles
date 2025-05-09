package de.lifecircles.model.neural;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Represents the neural network that controls cell behavior.
 */
public class NeuralNetwork {
    private final List<Neuron> inputNeuronList;
    private final List<Layer> hiddenLayerList; // Verwende Layer-Objekte statt Listen von Neuronen
    private final List<Neuron> outputNeuronList;
    private final List<Synapse> synapsesynapseList;
    private final Random random = new Random();
    private double[] outputArr;

    private static final double DEFAULT_MUTATION_RATE = 0.1D;

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
                newNeuron.setValue(originalNeuron.getValue());
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
    public NeuralNetwork(int inputCount, int hiddenCount, int outputCount, double synapseConnectivity) {
        this(inputCount, new int[]{hiddenCount}, outputCount, synapseConnectivity);
    }

    /**
     * Constructs a network with a single hidden layer with full connectivity.
     */
    public NeuralNetwork(int inputCount, int hiddenCount, int outputCount) {
        this(inputCount, new int[]{hiddenCount}, outputCount, 1.0);
    }

    /**
     * Constructs a network with multiple hidden layers.
     *
     * @param inputCount number of input neurons
     * @param hiddenCounts sizes of each hidden layer
     * @param outputCount number of output neurons
     * @param synapseConnectivity Prozentsatz der zu erstellenden Synapsen (0.0-1.0)
     */
    public NeuralNetwork(int inputCount, int[] hiddenCounts, int outputCount, double synapseConnectivity) {
        this.inputNeuronList = new ArrayList<>();
        this.hiddenLayerList = new ArrayList<>();
        this.outputNeuronList = new ArrayList<>();
        this.synapsesynapseList = new ArrayList<>();

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
        for (int i = 0; i < inputs.length; i++) {
            this.inputNeuronList.get(i).setValue(inputs[i]);
        }
    }

    /**
     * Processes the inputs through the network and returns the outputs.
     */
    public double[] process() {
        // process hidden layers
        for (Layer layer : this.hiddenLayerList) {
            if (layer.isActiveLayer()) {
                for (Neuron neuron : layer.getNeurons()) {
                    neuron.activate();
                }
            }
        }

        // process output layer
        for (Neuron neuron : this.outputNeuronList) {
            neuron.activate();
        }

        // Collect outputs
        for (int outputNeuronPos = 0; outputNeuronPos < this.outputArr.length; outputNeuronPos++) {
            this.outputArr[outputNeuronPos] = this.getOutputValue(outputNeuronPos);
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
            int pos = this.random.nextInt(this.hiddenLayerList.size() + 1);
            // Zufällige Neuronenzahl zwischen 1 und 5
            int neuronCount = 1 + this.random.nextInt(5);
            addHiddenLayer(pos, neuronCount);
        }

        // Wahrscheinlichkeit, ein Neuron zu einem bestehenden Layer hinzuzufügen
        if (!this.hiddenLayerList.isEmpty() && this.random.nextDouble() < structuralMutationRate * 2) { // Verdoppelte Wahrscheinlichkeit
            int li = this.random.nextInt(this.hiddenLayerList.size());
            addNeuronToHiddenLayer(li);
        }

        // Wahrscheinlichkeit, ein Neuron zu entfernen
        if (!this.hiddenLayerList.isEmpty() && this.random.nextDouble() < structuralMutationRate) {
            int li = random.nextInt(this.hiddenLayerList.size());
            removeNeuronFromHiddenLayer(li);
        }

        // Wahrscheinlichkeit, eine zufällige Synapse hinzuzufügen
        if (this.random.nextDouble() < structuralMutationRate * 3) { // Deutlich erhöhte Wahrscheinlichkeit
            addRandomSynapse();
        }

        // Wahrscheinlichkeit, eine zufällige Synapse zu entfernen
        if (this.random.nextDouble() < structuralMutationRate) {
            removeRandomSynapse();
        }

        // Neue Mutation: Aktivierungsfunktion eines zufälligen Neurons ändern
        if (!this.hiddenLayerList.isEmpty() && this.random.nextDouble() < structuralMutationRate) {
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
     */
    public void addHiddenLayer(int index, int neuronCount) {
        Layer newLayer = new Layer();
        newLayer.setActiveLayer(true); // Set new layer as active by default
        for (int i = 0; i < neuronCount; i++) {
            newLayer.addNeuron(new Neuron());
        }
        this.hiddenLayerList.add(index, newLayer);
        List<Neuron> prev = index == 0 ? this.inputNeuronList : this.hiddenLayerList.get(index - 1).getNeurons();
        for (Neuron src : prev) {
            for (Neuron tgt : newLayer.getNeurons()) {
                synapsesynapseList.add(new Synapse(src, tgt, Math.random() * 0.002D - 0.001D));
            }
        }
        List<Neuron> next = index == this.hiddenLayerList.size() - 1 ? this.outputNeuronList : this.hiddenLayerList.get(index + 1).getNeurons();
        for (Neuron src : newLayer.getNeurons()) {
            for (Neuron tgt : next) {
                this.synapsesynapseList.add(new Synapse(src, tgt, Math.random() * 0.002D - 0.001D));
            }
        }
    }

    public void addNeuronToHiddenLayer(int layerIndex) {
        Neuron newN = new Neuron();
        this.hiddenLayerList.get(layerIndex).addNeuron(newN);
        List<Neuron> prev = layerIndex == 0 ? this.inputNeuronList : this.hiddenLayerList.get(layerIndex - 1).getNeurons();
        for (Neuron src : prev) {
            synapsesynapseList.add(new Synapse(src, newN, Math.random() * 0.002D - 0.001D));
        }
        List<Neuron> next = layerIndex == this.hiddenLayerList.size() - 1 ? this.outputNeuronList : this.hiddenLayerList.get(layerIndex + 1).getNeurons();
        for (Neuron tgt : next) {
            this.synapsesynapseList.add(new Synapse(newN, tgt, Math.random() * 0.002D - 0.001D));
        }
    }

    public void removeNeuronFromHiddenLayer(int layerIndex) {
        List<Neuron> layer = this.hiddenLayerList.get(layerIndex).getNeurons();
        if (layer.isEmpty()) return;
        int idx = this.random.nextInt(layer.size());
        Neuron rem = layer.remove(idx);
        Iterator<Synapse> it = this.synapsesynapseList.iterator();
        while (it.hasNext()) {
            Synapse s = it.next();
            if (s.getSourceNeuron() == rem || s.getTargetNeuron() == rem) {
                s.getSourceNeuron().getOutputSynapses().remove(s);
                // Für Input-Synapsen die neue Methode verwenden
                if (s.getTargetNeuron() == rem) {
                    it.remove();
                    continue;
                }
                s.getTargetNeuron().removeInputSynapse(s);
                it.remove();
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
        int targetLayerIndex = this.random.nextInt(allLayers.size());
        
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
        Synapse s = new Synapse(sourceNeuron, targetNeuron, Math.random() * 0.002D - 0.001D);
        this.synapsesynapseList.add(s);
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
                mutated.addNeuronToHiddenLayer(layer);
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
}

