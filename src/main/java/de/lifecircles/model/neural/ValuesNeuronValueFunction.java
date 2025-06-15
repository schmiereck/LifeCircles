package de.lifecircles.model.neural;

public class ValuesNeuronValueFunction implements NeuronValueFunction {
    private double[] valuesArr = { };

    private int lowestFreeID = 0;
    private int lastFreeID = 0;

    @Override
    public double readValue(NeuralNetwork neuralNetwork, final Neuron neuron) {
        final int id = neuron.getId();
        this.checkValueArrSize(id);
        return this.valuesArr[id];
    }

    @Override
    public void writeValue(NeuralNetwork neuralNetwork, final Neuron neuron, double value) {
        final int id = neuron.getId();
        this.checkValueArrSize(id);
        this.valuesArr[id] = value;
    }

    @Override
    public int fetchNextFreeId(NeuralNetwork neuralNetwork) {
        final int currentLowestFreeID = this.lowestFreeID;
        final int currentLastFreeID = this.lastFreeID;

        if (currentLastFreeID == currentLowestFreeID) {
            // No known gaps, assign a new ID
            this.lastFreeID++;
            this.lowestFreeID = this.lastFreeID;
        } else {
            // Potential gap exists, search for the next free ID starting from lowestFreeID
            final java.util.Set<Integer> existingIds = new java.util.HashSet<>();
            for (final Neuron neuron : neuralNetwork.getInputNeuronArr()) {
                existingIds.add(neuron.getId());
            }
            for (final Layer layer : neuralNetwork.getHiddenLayerArr()) {
                for (final Neuron neuron : layer.getNeuronsArray()) {
                    existingIds.add(neuron.getId());
                }
            }
            for (final Neuron neuron : neuralNetwork.getOutputNeuronArr()) {
                existingIds.add(neuron.getId());
            }

            int searchId = currentLowestFreeID + 1; // Start searching from the next ID after lowestFreeID
            while (searchId < currentLastFreeID) {
                if (!existingIds.contains(searchId)) {
                    break;
                }
                searchId++;
            }

            this.lowestFreeID = searchId;
            if (searchId > this.lastFreeID) {
                this.lastFreeID = searchId;
            }
        }
        return currentLowestFreeID;
    }

    @Override
    public void releaseNeuron(NeuralNetwork neuralNetwork, Neuron neuron) {
        final int id = neuron.getId();

        if (this.lowestFreeID > id) {
            // If the neuron ID is less than to the lowest free ID, we can update it
            this.lowestFreeID = id;
        }
    }

    private void checkValueArrSize(final int id) {
        if (id >= this.valuesArr.length) {
            // Ensure the valuesArr is large enough to hold the neuron's value
            final double[] newValuesArr = new double[id + 1];
            System.arraycopy(this.valuesArr, 0, newValuesArr, 0, this.valuesArr.length);
            this.valuesArr = newValuesArr;
        }
    }
}
