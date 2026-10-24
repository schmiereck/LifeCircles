package de.lifecircles.model.neural;

public class ValuesNeuronValueFunction implements NeuronValueFunction {
    private static final long serialVersionUID = 1177906211537918699L;
    // Keep port zero's serialized representation compatible with existing saves.
    private double[][] extraValues = new double[0][];
    private transient double[][] extraDeltas = new double[0][];
    private transient double[][] extraInputSums = new double[0][];
    private double[] valuesArr = { };
    private transient double[] inputSumArr = { };
    private transient double[] deltaArr = { };

    private int lowestFreeID = 0;
    private int lastFreeID = 0;
    private java.util.Map<NeuronNetwork, ModuleExecutionState> moduleStates = new java.util.IdentityHashMap<>();

    @Override
    public ModuleExecutionState getModuleState(final NeuronNetwork module, final boolean checkTopology) {
        if (moduleStates == null) moduleStates = new java.util.IdentityHashMap<>();
        ModuleExecutionState state = moduleStates.get(module);
        if (state == null || (checkTopology && !state.network.getNeuralNet().hasSameTopology(module.getNeuronTypeInfoData().getNeuralNet()))) {
            state = module.createExecutionState();
            moduleStates.put(module, state);
        }
        return state;
    }

    @Override
    public void clearModuleStates() {
        if (moduleStates != null) moduleStates.clear();
    }

    @Override
    public void reserveExistingIds(final NeuralNet neuralNet) {
        final int[] next = {0};
        neuralNet.applyToAllNeurons(neuron -> next[0] = Math.max(next[0], neuron.getId() + 1));
        lowestFreeID = lastFreeID = next[0];
    }

    @Override
    public double readValue(final NeuralNet neuralNet, final NeuronInterface neuron, final int outputTypePos) {
        final int id = neuron.getId();
        this.checkValueArrSize(id);
        this.ensureChannels(outputTypePos);
        return outputTypePos == 0 ? this.valuesArr[id] : channel(extraValues, outputTypePos, id)[id];
    }

    @Override
    public void writeValue(final NeuralNet neuralNet, final NeuronInterface neuron, final int outputTypePos, final double value) {
        final int id = neuron.getId();
        this.checkValueArrSize(id);
        this.ensureChannels(outputTypePos);
        if (outputTypePos == 0) this.valuesArr[id] = value;
        else channel(extraValues, outputTypePos, id)[id] = value;
    }

    @Override
    public double readDelta(final NeuralNet neuralNet, final NeuronInterface neuron, final int outputTypePos) {
        final int id = neuron.getId();
        this.checkValueArrSize(id);
        this.ensureChannels(outputTypePos);
        return outputTypePos == 0 ? this.deltaArr[id] : channel(extraDeltas, outputTypePos, id)[id];
    }

    @Override
    public void writeDelta(final NeuralNet neuralNet, final NeuronInterface neuron, final int outputTypePos, final double delta) {
        final int id = neuron.getId();
        this.checkValueArrSize(id);
        this.ensureChannels(outputTypePos);
        if (outputTypePos == 0) this.deltaArr[id] = delta;
        else channel(extraDeltas, outputTypePos, id)[id] = delta;
    }

    @Override
    public double readInputSum(final NeuralNet neuralNet, final NeuronInterface neuron, final int outputTypePos) {
        final int id = neuron.getId();
        this.checkValueArrSize(id);
        this.ensureChannels(outputTypePos);
        return outputTypePos == 0 ? this.inputSumArr[id] : channel(extraInputSums, outputTypePos, id)[id];
    }

    @Override
    public void writeInputSum(final NeuralNet neuralNet, final NeuronInterface neuron, final int outputTypePos, final double inputSum) {
        final int id = neuron.getId();
        this.checkValueArrSize(id);
        this.ensureChannels(outputTypePos);
        if (outputTypePos == 0) this.inputSumArr[id] = inputSum;
        else channel(extraInputSums, outputTypePos, id)[id] = inputSum;
    }

    @Override
    public int fetchNextFreeId(final NeuralNet neuralNet) {
        ensureChannels(0);
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
        if (currentLowestFreeID < valuesArr.length) {
            valuesArr[currentLowestFreeID] = 0;
            if (deltaArr != null) deltaArr[currentLowestFreeID] = 0;
            if (inputSumArr != null) inputSumArr[currentLowestFreeID] = 0;
            for (double[] channel : extraValues) if (channel != null && currentLowestFreeID < channel.length) channel[currentLowestFreeID] = 0;
            for (double[] channel : extraDeltas) if (channel != null && currentLowestFreeID < channel.length) channel[currentLowestFreeID] = 0;
            for (double[] channel : extraInputSums) if (channel != null && currentLowestFreeID < channel.length) channel[currentLowestFreeID] = 0;
        }
        return currentLowestFreeID;
    }

    @Override
    public void releaseNeuron(NeuralNet neuralNet, NeuronInterface neuron) {
        final int id = neuron.getId();
        if (moduleStates != null) moduleStates.remove(neuron);

        if (this.lowestFreeID > id) {
            // If the neuron ID is less than to the lowest free ID, we can update it
            this.lowestFreeID = id;
        }
    }

    /**
     * Initialisiert nach dem Deserialisieren die transienten Felder,
     * um NullPointerExceptions zu vermeiden.
     *
     * @param stream ObjectInputStream
     * @throws java.io.IOException Bei IO-Fehlern
     * @throws ClassNotFoundException Bei Klassen-Ladefehlern
     */
    private void readObject(java.io.ObjectInputStream stream) throws java.io.IOException, ClassNotFoundException {
        // Standard-Deserialisierung aufrufen
        stream.defaultReadObject();
        ensureChannels(0);

        // Transiente Felder initialisieren
        if (valuesArr != null) {
            initTransientArrays(valuesArr.length);
        } else {
            initTransientArrays(0);
        }
    }

    /**
     * Initialisiert die transienten Arrays mit der gegebenen Größe.
     *
     * @param size Die Größe der Arrays
     */
    private void initTransientArrays(int size) {
        if (inputSumArr == null) {
            inputSumArr = new double[size];
        }

        if (deltaArr == null) {
            deltaArr = new double[size];
        }
    }

    private void ensureChannels(int port) {
        if (port < 0) throw new IllegalArgumentException("Negative port");
        if (extraValues == null) extraValues = new double[0][];
        if (extraDeltas == null) extraDeltas = new double[0][];
        if (extraInputSums == null) extraInputSums = new double[0][];
        if (port > extraValues.length) extraValues = java.util.Arrays.copyOf(extraValues, port);
        if (port > extraDeltas.length) extraDeltas = java.util.Arrays.copyOf(extraDeltas, port);
        if (port > extraInputSums.length) extraInputSums = java.util.Arrays.copyOf(extraInputSums, port);
    }

    private double[] channel(double[][] channels, int port, int id) {
        int index = port - 1;
        if (channels[index] == null) channels[index] = new double[Math.max(16, id + 1)];
        else if (id >= channels[index].length) {
            channels[index] = java.util.Arrays.copyOf(channels[index], Math.max(id + 1, channels[index].length * 2));
        }
        return channels[index];
    }

    /** Port-0 values with room for maxId, for compiled forward passes; valid until an id beyond maxId is used. */
    double[] values(final int maxId) {
        ensureChannels(0);
        if (maxId >= 0) checkValueArrSize(maxId);
        return valuesArr;
    }

    /** Port-0 input sums matching {@link #values(int)}. */
    double[] inputSums(final int maxId) {
        if (maxId >= 0) checkValueArrSize(maxId);
        return inputSumArr;
    }

    private void checkValueArrSize(final int id) {
        if (id < 0) throw new IllegalArgumentException("Negative neuron ID");
        if (inputSumArr == null || deltaArr == null) initTransientArrays(valuesArr.length);
        if (id >= valuesArr.length) {
            int size = Math.max(id + 1, Math.max(16, valuesArr.length * 2));
            valuesArr = java.util.Arrays.copyOf(valuesArr, size);
            deltaArr = java.util.Arrays.copyOf(deltaArr, size);
            inputSumArr = java.util.Arrays.copyOf(inputSumArr, size);
        }
    }
}
