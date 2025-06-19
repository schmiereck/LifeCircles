package de.lifecircles.model.neural;

public class ValuesNeuronValueFunction implements NeuronValueFunction {
    private double[] valuesArr = { };
    private transient double[] inputSumArr = { };
    private transient double[] deltaArr = { };

    private int lowestFreeID = 0;
    private int lastFreeID = 0;

    @Override
    public double readValue(final NeuralNet neuralNet, final NeuronInterface neuron, final int outputTypePos) {
        final int id = neuron.getId();
        this.checkValueArrSize(id);
        return this.valuesArr[id];
    }

    @Override
    public void writeValue(final NeuralNet neuralNet, final NeuronInterface neuron, final int outputTypePos, final double value) {
        final int id = neuron.getId();
        this.checkValueArrSize(id);
        this.valuesArr[id] = value;
    }

    @Override
    public double readDelta(final NeuralNet neuralNet, final NeuronInterface neuron, final int outputTypePos) {
        final int id = neuron.getId();
        this.checkValueArrSize(id);
        return this.deltaArr[id];
    }

    @Override
    public void writeDelta(final NeuralNet neuralNet, final NeuronInterface neuron, final int outputTypePos, final double delta) {
        final int id = neuron.getId();
        this.checkValueArrSize(id);
        this.deltaArr[id] = delta;
    }

    @Override
    public double readInputSum(final NeuralNet neuralNet, final NeuronInterface neuron, final int outputTypePos) {
        final int id = neuron.getId();
        this.checkValueArrSize(id);
        return this.inputSumArr[id];
    }

    @Override
    public void writeInputSum(final NeuralNet neuralNet, final NeuronInterface neuron, final int outputTypePos, final double inputSum) {
        final int id = neuron.getId();
        this.checkValueArrSize(id);
        this.inputSumArr[id] = inputSum;
    }

    @Override
    public int fetchNextFreeId(final NeuralNet neuralNet) {
        final int currentLowestFreeID = this.lowestFreeID;
        final int currentLastFreeID = this.lastFreeID;

        if (currentLastFreeID == currentLowestFreeID) {
            // No known gaps, assign a new ID
            this.lastFreeID++;
            this.lowestFreeID = this.lastFreeID;
        } else {
            // Potential gap exists, search for the next free ID starting from lowestFreeID
            final java.util.Set<Integer> existingIds = new java.util.HashSet<>();
            for (final Neuron neuron : neuralNet.getInputNeuronArr()) {
                existingIds.add(neuron.getId());
            }
            for (final Layer layer : neuralNet.getHiddenLayerArr()) {
                for (final NeuronInterface neuron : layer.getNeuronsArr()) {
                    existingIds.add(neuron.getId());
                }
            }
            for (final Neuron neuron : neuralNet.getOutputNeuronArr()) {
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
    public void releaseNeuron(NeuralNet neuralNet, NeuronInterface neuron) {
        final int id = neuron.getId();

        if (this.lowestFreeID > id) {
            // If the neuron ID is less than to the lowest free ID, we can update it
            this.lowestFreeID = id;
        }
    }

    private void checkValueArrSize(final int id) {
        if (id >= this.valuesArr.length) {
            {
                // Ensure the valuesArr is large enough to hold the neuron's value
                final double[] newValuesArr = new double[id + 1];
                System.arraycopy(this.valuesArr, 0, newValuesArr, 0, this.valuesArr.length);
                this.valuesArr = newValuesArr;
            }
            {
                final double[] newDeltaArr = new double[id + 1];
                System.arraycopy(this.deltaArr, 0, newDeltaArr, 0, this.deltaArr.length);
                this.deltaArr = newDeltaArr;
            }
            {
                final double[] newInputSumArr = new double[id + 1];
                System.arraycopy(this.inputSumArr, 0, newInputSumArr, 0, this.inputSumArr.length);
                this.inputSumArr = newInputSumArr;
            }
        }
    }
}
