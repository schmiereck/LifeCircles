package de.lifecircles.model.neural;

import java.util.List;

public class NeuralNetworkValues {
    private final NeuralNetwork neuralNetwork;
    private double[] valuesArr = { };
    private double[] outputArr;

    private int lowestFreeID = 0;
    private int lastFreeID = 0;

    private NeuronValueFunction neuronValueFunction;

    private class NeuralNetworkValuesFunction implements NeuronValueFunction {
        private NeuralNetworkValues neuralNetworkValues;

        public void setNeuralNetworkValues(final NeuralNetworkValues neuralNetworkValues) {
            this.neuralNetworkValues = neuralNetworkValues;
            final int neuronsSize = this.neuralNetworkValues.neuralNetwork.getAllNeuronsSize();
            final double[] newValuesArr = new double[neuronsSize];
            NeuralNetworkValues.this.valuesArr = newValuesArr;
        }

        @Override
        public double readValue(final Neuron neuron) {
            final int id = neuron.getId();
            this.checkValueArrSize(id);
            return NeuralNetworkValues.this.valuesArr[id];
        }

        @Override
        public void writeValue(final Neuron neuron, double value) {
            final int id = neuron.getId();
            this.checkValueArrSize(id);
            NeuralNetworkValues.this.valuesArr[id] = value;
        }

        @Override
        public int fetchNextFreeId() {
            final int currentLowestFreeID = this.neuralNetworkValues.lowestFreeID;
            final int currentLastFreeID = this.neuralNetworkValues.lastFreeID;

            if (currentLastFreeID == currentLowestFreeID) {
                // No known gaps, assign a new ID
                this.neuralNetworkValues.lastFreeID++;
                this.neuralNetworkValues.lowestFreeID = this.neuralNetworkValues.lastFreeID;
            } else {
                // Potential gap exists, search for the next free ID starting from lowestFreeID
                final java.util.Set<Integer> existingIds = new java.util.HashSet<>();
                for (final Neuron neuron : this.neuralNetworkValues.neuralNetwork.getInputNeuronArr()) {
                    existingIds.add(neuron.getId());
                }
                for (final Layer layer : this.neuralNetworkValues.neuralNetwork.getHiddenLayerArr()) {
                    for (final Neuron neuron : layer.getNeuronsArray()) {
                        existingIds.add(neuron.getId());
                    }
                }
                for (final Neuron neuron : this.neuralNetworkValues.neuralNetwork.getOutputNeuronArr()) {
                    existingIds.add(neuron.getId());
                }

                int searchId = currentLowestFreeID + 1; // Start searching from the next ID after lowestFreeID
                while (searchId < currentLastFreeID) {
                    if (!existingIds.contains(searchId)) {
                        break;
                    }
                    searchId++;
                }

                this.neuralNetworkValues.lowestFreeID = searchId;
                if (searchId > this.neuralNetworkValues.lastFreeID) {
                    this.neuralNetworkValues.lastFreeID = searchId;
                }
            }
            return currentLowestFreeID;
        }

        private void checkValueArrSize(final int id) {
            if (id >= NeuralNetworkValues.this.valuesArr.length) {
                // Ensure the valuesArr is large enough to hold the neuron's value
                final double[] newValuesArr = new double[id + 1];
                System.arraycopy(NeuralNetworkValues.this.valuesArr, 0, newValuesArr, 0, NeuralNetworkValues.this.valuesArr.length);
                NeuralNetworkValues.this.valuesArr = newValuesArr;
            }
        }
    };

    public NeuralNetworkValues(final NeuralNetwork neuralNetwork) {
        NeuralNetworkValuesFunction neuralNetworkValuesFunction = new NeuralNetworkValuesFunction();
        this.neuronValueFunction = neuralNetworkValuesFunction;
        this.neuralNetwork = neuralNetwork;
        neuralNetworkValuesFunction.setNeuralNetworkValues(this);
        this.neuralNetwork.setNeuronValueFunction(this.neuronValueFunction);
    }

    public NeuralNetworkValues(final int inputCount, final int[] hiddenCounts, final int outputCount, final double synapseConnectivity, final int fixedHiddenLayerCount) {
        NeuralNetworkValuesFunction neuralNetworkValuesFunction = new NeuralNetworkValuesFunction();
        this.neuronValueFunction = neuralNetworkValuesFunction;
        this.neuralNetwork = new NeuralNetwork(inputCount, hiddenCounts, outputCount, synapseConnectivity, fixedHiddenLayerCount);
        neuralNetworkValuesFunction.setNeuralNetworkValues(this);
        this.neuralNetwork.setNeuronValueFunction(this.neuronValueFunction);
    }

    public void setDisableLayerDeactivation(final boolean disableLayerDeactivation) {
        this.neuralNetwork.setDisableLayerDeactivation(disableLayerDeactivation);
    }

    public List<Synapse> getSynapseList() {
        return this.neuralNetwork.getSynapseList();
    }

    public Synapse[] getSynapseArr() {
        return this.neuralNetwork.getSynapseArr();
    }

    public int getInputLayerSize() {
        return this.neuralNetwork.getInputLayerSize();
    }

    public int getOutputLayerSize() {
        return this.neuralNetwork.getOutputLayerSize();
    }

    public void setInputs(final double[] inputArr) {
        this.neuralNetwork.setInputs(inputArr);
    }

    public double[] process() {
        return this.neuralNetwork.process();
    }

    public NeuralNetworkValues mutate(final double mutationRate, final double mutationStrength) {
        return new NeuralNetworkValues(this.neuralNetwork.mutate(mutationRate, mutationStrength));
    }

    public Layer[] getHiddenLayerArr() {
        return this.neuralNetwork.getHiddenLayerArr();
    }
}
