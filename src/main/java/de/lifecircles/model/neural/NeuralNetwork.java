package de.lifecircles.model.neural;

import java.util.*;

/**
 * Represents the neural network that controls cell behavior.
 */
public class NeuralNetwork {
    private final List<Neuron> inputNeurons;
    private final List<List<Neuron>> hiddenLayers;
    private final List<Neuron> outputNeurons;
    private final List<Synapse> synapses;
    private final Random random = new Random();

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
            inputNeurons.add(new Neuron());
        }

        // create hidden layers
        for (int count : hiddenCounts) {
            List<Neuron> layer = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                layer.add(new Neuron());
            }
            hiddenLayers.add(layer);
        }

        // create output neurons
        for (int i = 0; i < outputCount; i++) {
            outputNeurons.add(new Neuron());
        }

        // connect layers sequentially: input -> hidden1 -> ... -> hiddenN -> output
        List<Neuron> prev = inputNeurons;
        for (List<Neuron> layer : hiddenLayers) {
            connectLayers(prev, layer);
            prev = layer;
        }
        connectLayers(prev, outputNeurons);
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
        for (List<Neuron> layer : hiddenLayers) {
            for (Neuron neuron : layer) {
                neuron.activate();
            }
        }

        // process output layer
        for (Neuron neuron : this.outputNeurons) {
            neuron.activate();
        }

        // Collect outputs
        double[] outputs = new double[this.outputNeurons.size()];
        for (int outputNeuronPos = 0; outputNeuronPos < outputs.length; outputNeuronPos++) {
            outputs[outputNeuronPos] = this.getOutput(outputNeuronPos);
        }
        return outputs;
    }

    public double getOutput(final int outputNeuronPos) {
        return this.outputNeurons.get(outputNeuronPos).getValue();
    }

    /**
     * Copies weights and biases from another network.
     */
    public void copyFrom(NeuralNetwork other) {
        if (inputNeurons.size() != other.inputNeurons.size() ||
            hiddenLayers.size() != other.hiddenLayers.size() ||
            outputNeurons.size() != other.outputNeurons.size()) {
            throw new IllegalArgumentException("Network architectures do not match");
        }

        // copy input neurons
        copyNeurons(other.inputNeurons, inputNeurons);
        // copy hidden layers
        for (int i = 0; i < hiddenLayers.size(); i++) {
            copyNeurons(other.hiddenLayers.get(i), hiddenLayers.get(i));
        }
        // copy output neurons
        copyNeurons(other.outputNeurons, outputNeurons);
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
     * Mutates the network's weights and biases.
     */
    public void mutate2(double mutationRate, double mutationStrength) {
        // Mutate neuron biases
        for (Neuron neuron : getAllNeurons()) {
            if (random.nextDouble() < mutationRate) {
                double mutation = (random.nextDouble() * 2 - 1) * mutationStrength;
                neuron.setBias(neuron.getBias() + mutation);
            }
        }

        // Mutate synapse weights
        for (Synapse synapse : synapses) {
            if (random.nextDouble() < mutationRate) {
                double mutation = (random.nextDouble() * 2 - 1) * mutationStrength;
                synapse.setWeight(synapse.getWeight() + mutation);
            }
        }
    }

    /**
     * Creates a mutated copy of this neural network.
     * @param mutationRate Probability of each weight/bias being mutated
     * @param mutationStrength Maximum amount of mutation
     */
    public NeuralNetwork mutate(double mutationRate, double mutationStrength) {
        // capture hidden layer sizes
        int[] hiddenSizes = new int[hiddenLayers.size()];
        for (int i = 0; i < hiddenLayers.size(); i++) {
            hiddenSizes[i] = hiddenLayers.get(i).size();
        }
        NeuralNetwork mutated = new NeuralNetwork(
                inputNeurons.size(),
                hiddenSizes,
                outputNeurons.size()
        );

        // copy and potentially mutate input neurons
        copyAndMutateNeurons(inputNeurons, mutated.inputNeurons, mutationRate, mutationStrength);
        // copy and potentially mutate hidden layers
        for (int i = 0; i < hiddenLayers.size(); i++) {
            copyAndMutateNeurons(hiddenLayers.get(i), mutated.hiddenLayers.get(i), mutationRate, mutationStrength);
        }
        // copy and potentially mutate output neurons
        copyAndMutateNeurons(outputNeurons, mutated.outputNeurons, mutationRate, mutationStrength);

        // apply structural mutations
        mutated.applyStructuralMutations(mutationRate);

        return mutated;
    }

    public List<Neuron> getAllNeurons() {
        List<Neuron> allNeurons = new ArrayList<>();
        allNeurons.addAll(inputNeurons);
        for (List<Neuron> layer : hiddenLayers) {
            allNeurons.addAll(layer);
        }
        allNeurons.addAll(outputNeurons);
        return allNeurons;
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
        if (random.nextDouble() < mutationRate) {
            int pos = random.nextInt(hiddenLayers.size() + 1);
            addHiddenLayer(pos, 1);
        }
        if (!hiddenLayers.isEmpty() && random.nextDouble() < mutationRate) {
            int li = random.nextInt(hiddenLayers.size());
            addNeuronToHiddenLayer(li);
        }
        if (!hiddenLayers.isEmpty() && random.nextDouble() < mutationRate) {
            int li = random.nextInt(hiddenLayers.size());
            removeNeuronFromHiddenLayer(li);
        }
        if (!hiddenLayers.isEmpty() && random.nextDouble() < mutationRate) {
            addRandomSynapseInHiddenLayers();
        }
        if (!synapses.isEmpty() && random.nextDouble() < mutationRate) {
            removeRandomSynapseFromHiddenLayers();
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
                synapses.add(new Synapse(src, tgt));
            }
        }
        List<Neuron> next = index == hiddenLayers.size() - 1 ? outputNeurons : hiddenLayers.get(index + 1);
        for (Neuron src : newLayer) {
            for (Neuron tgt : next) {
                synapses.add(new Synapse(src, tgt));
            }
        }
    }

    public void addNeuronToHiddenLayer(int layerIndex) {
        Neuron newN = new Neuron();
        hiddenLayers.get(layerIndex).add(newN);
        List<Neuron> prev = layerIndex == 0 ? inputNeurons : hiddenLayers.get(layerIndex - 1);
        for (Neuron src : prev) {
            synapses.add(new Synapse(src, newN));
        }
        List<Neuron> next = layerIndex == hiddenLayers.size() - 1 ? outputNeurons : hiddenLayers.get(layerIndex + 1);
        for (Neuron tgt : next) {
            synapses.add(new Synapse(newN, tgt));
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
                s.getTargetNeuron().getInputSynapses().remove(s);
                it.remove();
            }
        }
    }

    public void addRandomSynapseInHiddenLayers() {
        int li = random.nextInt(hiddenLayers.size());
        List<Neuron> layer = hiddenLayers.get(li);
        if (layer.size() < 2) return;
        int i = random.nextInt(layer.size());
        int j = random.nextInt(layer.size());
        if (i != j) {
            Synapse s = new Synapse(layer.get(i), layer.get(j));
            synapses.add(s);
        }
    }

    public void removeRandomSynapseFromHiddenLayers() {
        List<Synapse> list = new ArrayList<>();
        for (Synapse s : synapses) {
            if (isInHiddenLayer(s.getSourceNeuron()) && isInHiddenLayer(s.getTargetNeuron())) {
                list.add(s);
            }
        }
        if (list.isEmpty()) return;
        Synapse rem = list.get(random.nextInt(list.size()));
        rem.getSourceNeuron().getOutputSynapses().remove(rem);
        rem.getTargetNeuron().getInputSynapses().remove(rem);
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
}
