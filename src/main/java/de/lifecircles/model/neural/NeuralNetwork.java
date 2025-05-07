package de.lifecircles.model.neural;

import java.util.*;
import java.util.function.Consumer;

/**
 * Represents the neural network that controls cell behavior.
 */
public class NeuralNetwork {
    private final List<Neuron> inputNeurons;
    private final List<List<Neuron>> hiddenLayers;
    private final List<Neuron> outputNeurons;
    private final List<Synapse> synapses;
    private final Random random = new Random();
    private double[] outputs;

    private static final double DEFAULT_MUTATION_RATE = 0.1D;

    /**
     * Constructs a network with a single hidden layer.
     */
    public NeuralNetwork(int inputCount, int hiddenCount, int outputCount) {
        this(inputCount, new int[]{hiddenCount}, outputCount);
    }

    /**
     * Constructs a network with multiple hidden layers.
     *
     * @param inputCount number of input neurons
     * @param hiddenCounts sizes of each hidden layer
     * @param outputCount number of output neurons
     */
    public NeuralNetwork(int inputCount, int[] hiddenCounts, int outputCount) {
        this.inputNeurons = new ArrayList<>();
        this.hiddenLayers = new ArrayList<>();
        this.outputNeurons = new ArrayList<>();
        this.synapses = new ArrayList<>();

        // create input neurons
        for (int i = 0; i < inputCount; i++) {
            this.inputNeurons.add(new Neuron());
        }

        // create hidden layers
        for (int count : hiddenCounts) {
            List<Neuron> layer = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                layer.add(new Neuron());
            }
            this.hiddenLayers.add(layer);
        }

        // create output neurons
        for (int i = 0; i < outputCount; i++) {
            Neuron outputNeuron = new Neuron();
            outputNeuron.setOutputNeuron(true); // Markiere als Output-Neuron
            this.outputNeurons.add(outputNeuron);
        }
        this.outputs = new double[this.outputNeurons.size()];

        // connect layers sequentially: input -> hidden1 -> ... -> hiddenN -> output
        List<Neuron> prev = this.inputNeurons;
        for (List<Neuron> layer : this.hiddenLayers) {
            connectLayers(prev, layer);
            prev = layer;
        }
        connectLayers(prev, this.outputNeurons);
    }

    private void connectLayers(List<Neuron> sourceLayer, List<Neuron> targetLayer) {
        for (Neuron source : sourceLayer) {
            for (Neuron target : targetLayer) {
                synapses.add(new Synapse(source, target));
            }
        }
    }

    /**
     * Sets the input values for the network.
     */
    public void setInputs(double[] inputs) {
        if (inputs.length != inputNeurons.size()) {
            throw new IllegalArgumentException("Input size mismatch");
        }
        for (int i = 0; i < inputs.length; i++) {
            inputNeurons.get(i).setValue(inputs[i]);
        }
    }

    /**
     * Processes the inputs through the network and returns the outputs.
     */
    public double[] process() {
        // process hidden layers
        for (List<Neuron> layer : this.hiddenLayers) {
            for (Neuron neuron : layer) {
                neuron.activate();
            }
        }

        // process output layer
        for (Neuron neuron : this.outputNeurons) {
            neuron.activate();
        }

        // Collect outputs
        for (int outputNeuronPos = 0; outputNeuronPos < this.outputs.length; outputNeuronPos++) {
            this.outputs[outputNeuronPos] = this.getOutputValue(outputNeuronPos);
        }
        return this.outputs;
    }

    public double getOutputValue(final int outputNeuronPos) {
        return this.outputNeurons.get(outputNeuronPos).getValue();
    }

    /**
     * Copies weights and biases from another network.
     */
    public void copyFrom(NeuralNetwork other) {
        if (this.inputNeurons.size() != other.inputNeurons.size() ||
                this.hiddenLayers.size() != other.hiddenLayers.size() ||
                this.outputNeurons.size() != other.outputNeurons.size()) {
            throw new IllegalArgumentException("Network architectures do not match");
        }

        // copy input neurons
        this.copyNeurons(other.inputNeurons, this.inputNeurons);
        // copy hidden layers
        for (int i = 0; i < this.hiddenLayers.size(); i++) {
            this.copyNeurons(other.hiddenLayers.get(i), this.hiddenLayers.get(i));
        }
        // copy output neurons
        for (int i = 0; i < this.outputNeurons.size(); i++) {
            Neuron src = other.outputNeurons.get(i);
            Neuron dst = this.outputNeurons.get(i);
            dst.setBias(src.getBias());
            dst.setOutputNeuron(true); // Stelle sicher, dass Output-Neuronen richtig markiert sind
        }
    }

    private void copyNeurons(List<Neuron> source, List<Neuron> target) {
        for (int i = 0; i < source.size(); i++) {
            target.get(i).setBias(source.get(i).getBias());
        }
    }

    /**
     * Returns the number of input neurons in the network.
     * @return the count of input neurons
     */
    public int getInputCount() {
        return inputNeurons.size();
    }

    /**
     * Creates a mutated copy of this neural network.
     * @param mutationRate Probability of each weight/bias being mutated
     * @param mutationStrength Maximum amount of mutation
     */
    public NeuralNetwork mutate(double mutationRate, double mutationStrength) {
        // Create a new network with the same architecture
        int[] hiddenSizes = new int[hiddenLayers.size()];
        for (int i = 0; i < hiddenLayers.size(); i++) {
            hiddenSizes[i] = hiddenLayers.get(i).size();
        }
        NeuralNetwork mutated = new NeuralNetwork(
            inputNeurons.size(),
            hiddenSizes,
            outputNeurons.size()
        );

        // Copy all neurons
        for (int i = 0; i < inputNeurons.size(); i++) {
            mutated.inputNeurons.get(i).setBias(inputNeurons.get(i).getBias());
        }
        for (int i = 0; i < outputNeurons.size(); i++) {
            mutated.outputNeurons.get(i).setBias(outputNeurons.get(i).getBias());
        }
        for (int i = 0; i < hiddenLayers.size(); i++) {
            for (int j = 0; j < hiddenLayers.get(i).size(); j++) {
                mutated.hiddenLayers.get(i).get(j).setBias(hiddenLayers.get(i).get(j).getBias());
            }
        }

        // Erstelle ein Mapping zwischen Neuronen des Quellnetzwerks und des mutierten Netzwerks
        Map<Neuron, Neuron> neuronMapping = createNeuronMapping(mutated);

        // Copy all synapses
        for (Synapse synapse : synapses) {
            Neuron source = synapse.getSourceNeuron();
            Neuron target = synapse.getTargetNeuron();
            
            // Verwende die Mapping-Tabelle für schnellen Zugriff
            Neuron newSource = neuronMapping.get(source);
            Neuron newTarget = neuronMapping.get(target);
            
            if (newSource != null && newTarget != null) {
                Synapse newSynapse = new Synapse(newSource, newTarget);
                newSynapse.setWeight(synapse.getWeight());
            }
        }

        // Mutate weights and biases
        mutated.applyToAllNeurons(neuron -> {
            if (Math.random() < mutationRate) {
                double mutation = (Math.random() * 2.0D - 1.0D) * mutationStrength;
                neuron.setBias(neuron.getBias() + mutation);
            }
        });
        
        for (Synapse synapse : mutated.synapses) {
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
        for (Neuron neuron : inputNeurons) {
            function.accept(neuron);
        }
        
        // Anwenden auf Hidden-Layer-Neuronen
        for (List<Neuron> layer : hiddenLayers) {
            for (Neuron neuron : layer) {
                function.accept(neuron);
            }
        }
        
        // Anwenden auf Output-Neuronen
        for (Neuron neuron : outputNeurons) {
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
        for (int i = 0; i < this.inputNeurons.size(); i++) {
            mapping.put(this.inputNeurons.get(i), target.inputNeurons.get(i));
        }
        
        // Hidden-Layer-Neuronen zuordnen
        for (int i = 0; i < this.hiddenLayers.size(); i++) {
            List<Neuron> sourceLayer = this.hiddenLayers.get(i);
            List<Neuron> targetLayer = target.hiddenLayers.get(i);
            for (int j = 0; j < sourceLayer.size(); j++) {
                mapping.put(sourceLayer.get(j), targetLayer.get(j));
            }
        }
        
        // Output-Neuronen zuordnen
        for (int i = 0; i < this.outputNeurons.size(); i++) {
            mapping.put(this.outputNeurons.get(i), target.outputNeurons.get(i));
            // Stelle sicher, dass Output-Neuronen richtig markiert sind
            target.outputNeurons.get(i).setOutputNeuron(true);
        }
        
        return mapping;
    }

    // Die alte Methode kann entweder entfernt oder durch die optimierte Version ersetzt werden
    private Neuron findCorrespondingNeuron(NeuralNetwork network, Neuron original) {
        // Diese Methode wird nicht mehr benötigt, wenn die neue Mapping-Implementierung verwendet wird
        if (this.inputNeurons.contains(original)) {
            return network.inputNeurons.get(this.inputNeurons.indexOf(original));
        }
        if (this.outputNeurons.contains(original)) {
            return network.outputNeurons.get(outputNeurons.indexOf(original));
        }
        for (int i = 0; i < this.hiddenLayers.size(); i++) {
            if (this.hiddenLayers.get(i).contains(original)) {
                return network.hiddenLayers.get(i).get(this.hiddenLayers.get(i).indexOf(original));
            }
        }
        return null;
    }

    private void copyAndMutateNeurons(List<Neuron> source, List<Neuron> target,
                                    double mutationRate, double mutationStrength) {
        for (int i = 0; i < source.size(); i++) {
            Neuron original = source.get(i);
            Neuron copy = target.get(i);
            
            if (Math.random() < mutationRate) {
                double mutation = (Math.random() * 2 - 1) * mutationStrength;
                copy.setBias(original.getBias() + mutation);
            } else {
                copy.setBias(original.getBias());
            }
        }
    }

    // Structural mutation helpers
    private void applyStructuralMutations(double mutationRate) {
        // Structural mutations should be much less frequent than weight/bias mutations
        double structuralMutationRate = mutationRate * DEFAULT_MUTATION_RATE; // Only 10% of the regular mutation rate
        
        // Möglichkeit, ein komplettes Hidden Layer hinzuzufügen
        if (this.random.nextDouble() < structuralMutationRate * 1.5) { // Erhöhte Wahrscheinlichkeit
            int pos = this.random.nextInt(this.hiddenLayers.size() + 1);
            // Zufällige Neuronenzahl zwischen 1 und 5
            int neuronCount = 1 + this.random.nextInt(5);
            addHiddenLayer(pos, neuronCount);
        }

        // Wahrscheinlichkeit, ein Neuron zu einem bestehenden Layer hinzuzufügen
        if (!this.hiddenLayers.isEmpty() && this.random.nextDouble() < structuralMutationRate * 2) { // Verdoppelte Wahrscheinlichkeit
            int li = this.random.nextInt(this.hiddenLayers.size());
            addNeuronToHiddenLayer(li);
        }

        // Wahrscheinlichkeit, ein Neuron zu entfernen
        if (!this.hiddenLayers.isEmpty() && this.random.nextDouble() < structuralMutationRate) {
            int li = random.nextInt(this.hiddenLayers.size());
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
        if (!this.hiddenLayers.isEmpty() && this.random.nextDouble() < structuralMutationRate) {
            // Wähle ein zufälliges Hidden Layer
            List<Neuron> layer = this.hiddenLayers.get(random.nextInt(this.hiddenLayers.size()));
            if (!layer.isEmpty()) {
                Neuron neuron = layer.get(random.nextInt(layer.size()));
                // Wähle eine zufällige Aktivierungsfunktion
                ActivationFunction[] functions = ActivationFunction.values();
                neuron.setActivationFunction(functions[random.nextInt(functions.length)]);
            }
        }
    }

    public void addHiddenLayer(int index, int neuronCount) {
        List<Neuron> newLayer = new ArrayList<>();
        for (int i = 0; i < neuronCount; i++) {
            newLayer.add(new Neuron());
        }
        hiddenLayers.add(index, newLayer);
        List<Neuron> prev = index == 0 ? inputNeurons : hiddenLayers.get(index - 1);
        for (Neuron src : prev) {
            for (Neuron tgt : newLayer) {
                synapses.add(new Synapse(src, tgt, Math.random() * 0.002D - 0.001D));
            }
        }
        List<Neuron> next = index == hiddenLayers.size() - 1 ? outputNeurons : hiddenLayers.get(index + 1);
        for (Neuron src : newLayer) {
            for (Neuron tgt : next) {
                synapses.add(new Synapse(src, tgt, Math.random() * 0.002D - 0.001D));
            }
        }
    }

    public void addNeuronToHiddenLayer(int layerIndex) {
        Neuron newN = new Neuron();
        hiddenLayers.get(layerIndex).add(newN);
        List<Neuron> prev = layerIndex == 0 ? inputNeurons : hiddenLayers.get(layerIndex - 1);
        for (Neuron src : prev) {
            synapses.add(new Synapse(src, newN, Math.random() * 0.002D - 0.001D));
        }
        List<Neuron> next = layerIndex == hiddenLayers.size() - 1 ? outputNeurons : hiddenLayers.get(layerIndex + 1);
        for (Neuron tgt : next) {
            synapses.add(new Synapse(newN, tgt, Math.random() * 0.002D - 0.001D));
        }
    }

    public void removeNeuronFromHiddenLayer(int layerIndex) {
        List<Neuron> layer = hiddenLayers.get(layerIndex);
        if (layer.isEmpty()) return;
        int idx = random.nextInt(layer.size());
        Neuron rem = layer.remove(idx);
        Iterator<Synapse> it = synapses.iterator();
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
        allLayers.add(inputNeurons);
        allLayers.addAll(hiddenLayers);
        allLayers.add(outputNeurons);
        
        int sourceLayerIndex = random.nextInt(allLayers.size());
        int targetLayerIndex = random.nextInt(allLayers.size());
        
        // Make sure we're not connecting within the same layer
        if (sourceLayerIndex == targetLayerIndex) {
            targetLayerIndex = (sourceLayerIndex + 1) % allLayers.size();
        }
        
        List<Neuron> sourceLayer = allLayers.get(sourceLayerIndex);
        List<Neuron> targetLayer = allLayers.get(targetLayerIndex);
        
        if (sourceLayer.isEmpty() || targetLayer.isEmpty()) return;
        
        // Wählen Sie zufällige Neuronen aus den beiden Layern
        Neuron sourceNeuron = sourceLayer.get(random.nextInt(sourceLayer.size()));
        Neuron targetNeuron = targetLayer.get(random.nextInt(targetLayer.size()));
        
        // Erstellen Sie die Synapse
        Synapse s = new Synapse(sourceNeuron, targetNeuron, Math.random() * 0.002D - 0.001D);
        synapses.add(s);
    }

    public void removeRandomSynapse() {
        if (synapses.isEmpty()) return;
        
        Synapse rem = synapses.get(random.nextInt(synapses.size()));
        rem.getSourceNeuron().getOutputSynapses().remove(rem);
        rem.getTargetNeuron().removeInputSynapse(rem);
        synapses.remove(rem);
    }

    private boolean isInHiddenLayer(Neuron n) {
        for (List<Neuron> layer : hiddenLayers) {
            if (layer.contains(n)) return true;
        }
        return false;
    }

    // Expose the number of synapses for energy cost calculation
    public int getSynapseCount() {
        return synapses.size();
    }

    public List<Synapse> getSynapses() {
        return this.synapses;
    }

    public List<List<Neuron>> getHiddenLayers() {
        return this.hiddenLayers;
    }

    /**
     * Returns the total count of all neurons in the network.
     * This is more efficient than creating a list of all neurons first.
     * 
     * @return the total number of neurons in the network
     */
    public int getAllNeuronsSize() {
        int count = inputNeurons.size() + outputNeurons.size();
        
        for (List<Neuron> layer : hiddenLayers) {
            count += layer.size();
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
            int pos = random.nextInt(mutated.hiddenLayers.size() + 1);
            int neuronCount = 2 + random.nextInt(4); // 2-5 Neuronen
            mutated.addHiddenLayer(pos, neuronCount);
        }

        // 50% Chance für mehrere zusätzliche Neuronen
        if (random.nextDouble() < 0.5 && !mutated.hiddenLayers.isEmpty()) {
            int count = 1 + random.nextInt(3); // 1-3 neue Neuronen
            for (int i = 0; i < count; i++) {
                int layer = random.nextInt(mutated.hiddenLayers.size());
                mutated.addNeuronToHiddenLayer(layer);
            }
        }

        // 70% Chance für mehrere zusätzliche Synapsen
        if (random.nextDouble() < 0.7) {
            int count = 2 + random.nextInt(4); // 2-5 neue Synapsen
            for (int i = 0; i < count; i++) {
                mutated.addRandomSynapse();
            }
        }

        return mutated;
    }

    public double getInputLayerSize() {
        return this.inputNeurons.size();
    }

    public double getOutputLayerSize() {
        return this.outputNeurons.size();
    }
}
