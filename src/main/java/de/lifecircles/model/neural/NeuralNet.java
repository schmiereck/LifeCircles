package de.lifecircles.model.neural;

import de.lifecircles.service.SimulationConfig;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;

import static de.lifecircles.model.neural.NeuralNetwork.DEFAULT_MUTATION_RATE;

public class NeuralNet implements Serializable {
    private static final long serialVersionUID = -1847201268253122334L;
    private transient CopyContext copyContext;
    private Neuron[] inputNeuronArr;
    private Layer[] hiddenLayerArr; // Array statt List
    private Neuron[] outputNeuronArr;

    private Synapse[] synapseArray;
    private int fixedHiddenLayerCount = SimulationConfig.CELL_STATE_ACTIVE_LAYER_COUNT;

    /**
     * Flag zum Deaktivieren der Layer-Deaktivierung (z.B. für Tests)
     */
    private transient boolean disableLayerDeactivation = false;

    private transient long proccessedSynapses = 0L;
    private transient Map<NeuronInterface, Integer> forwardOrder;
    private transient Map<Synapse, Double> forwardSources;
    private transient Set<NeuralNet> storageVisited;
    private transient NetworkExecutionPlan executionPlan;
    private transient boolean skippedState;
    private transient boolean executionOptimizationEnabled = true;
    private transient boolean usingExecutionPlan;
    private transient boolean optimizingModules;
    private transient boolean executionPlanUnneeded;

    /** Disabling preserves output semantics; reset before changing topology after optimized execution. */
    public void setExecutionOptimizationEnabled(boolean enabled) { executionOptimizationEnabled = enabled; }
    public boolean isExecutionOptimizationEnabled() { return executionOptimizationEnabled; }
    boolean isOptimizingModules() { return optimizingModules; }

    boolean isRecordingForward() { return forwardOrder != null; }

    private boolean wasExecuted(NeuronInterface neuron) {
        return forwardOrder != null && forwardOrder.containsKey(neuron);
    }

    /** Previous-step outputs are constants: this is one-step training, not BPTT. */
    boolean propagatesCurrentStep(Synapse edge) {
        if (forwardOrder == null) return false;
        Integer source = forwardOrder.get(edge.getSourceNeuron());
        Integer target = forwardOrder.get(edge.getTargetNeuron());
        return source != null && target != null && source < target;
    }

    private boolean enableNeuronType = false;

    /**
     * List of Neuron-Types can be used in this Network.
     */
    private List<NeuronTypeInfoData> neuronTypeInfoDataList = new ArrayList<>() {
        {
            // Mindestens ein "exhtes" Neuron.
            add(new NeuronTypeInfoData(1, 1, null));
        }
    };

    /**
     * Copies the genotype into independent neurons and modules with fresh activations.
     *
     * @param original Das zu kopierende neuronale Netzwerk
     */
    public NeuralNet(final NeuralNet original, final boolean makeCopy,
                     final NeuronValueFunctionFactory neuronValueFunctionFactory, final NeuronValueFunction neuronValueFunction) {
        this(original, makeCopy, neuronValueFunctionFactory, neuronValueFunction, new CopyContext(neuronValueFunctionFactory));
    }

    private NeuralNet(final NeuralNet original, final boolean makeCopy,
                      final NeuronValueFunctionFactory neuronValueFunctionFactory, final NeuronValueFunction neuronValueFunction,
                      final CopyContext context) {
        this.copyContext = context;
        if (makeCopy) {
            context.networks.put(original, this);
            context.visiting.add(original);
            this.inputNeuronArr = new Neuron[original.inputNeuronArr.length];
            this.outputNeuronArr = new Neuron[original.outputNeuronArr.length];
            // Synapsen-Array initialisieren
            this.synapseArray = new Synapse[original.synapseArray.length];

            this.disableLayerDeactivation = original.disableLayerDeactivation;
            this.fixedHiddenLayerCount = original.fixedHiddenLayerCount;
            this.enableNeuronType = original.enableNeuronType;

            this.neuronTypeInfoDataList = new ArrayList<>();
            // Kopiere die NeuronTypeInfoData
            for (final NeuronTypeInfoData typeInfo : original.neuronTypeInfoDataList) {
                this.neuronTypeInfoDataList.add(copyNeuronType(typeInfo, neuronValueFunctionFactory));
            }

            // Erstelle eine Map, die die Originalneuronen den neuen Neuronen zuordnet
            final Map<NeuronInterface, NeuronInterface> neuronMap = new HashMap<>();

            // Kopiere Input-Neuronen
            for (int inputNeuronPos = 0; inputNeuronPos < original.inputNeuronArr.length; inputNeuronPos++) {
                final Neuron originalNeuron = original.inputNeuronArr[inputNeuronPos];
                final Neuron newNeuron = (Neuron) originalNeuron.cloneNeuron(this, neuronValueFunction, true);
                this.inputNeuronArr[inputNeuronPos] = newNeuron;
                neuronMap.put(originalNeuron, newNeuron);
            }

            // Kopiere Hidden Layers
            this.hiddenLayerArr = new Layer[original.hiddenLayerArr.length];
            for (int hiddenLayerPos = 0; hiddenLayerPos < original.hiddenLayerArr.length; hiddenLayerPos++) {
                final Layer originalLayer = original.hiddenLayerArr[hiddenLayerPos];
                final Layer newLayer = new Layer();
                newLayer.setActiveLayer(hiddenLayerPos >= original.fixedHiddenLayerCount || originalLayer.isActiveLayer());

                for (final NeuronInterface originalNeuron : originalLayer.getNeuronsArr()) {
                    final NeuronInterface newNeuron = originalNeuron.cloneNeuron(this, neuronValueFunction, newLayer.isActiveLayer());
                    newLayer.addNeuron(newNeuron);
                    neuronMap.put(originalNeuron, newNeuron);
                }
                this.hiddenLayerArr[hiddenLayerPos] = newLayer;
            }

            // Kopiere Output-Neuronen
            for (int outputNeuronPos = 0; outputNeuronPos < original.outputNeuronArr.length; outputNeuronPos++) {
                final Neuron originalNeuron = original.outputNeuronArr[outputNeuronPos];
                final Neuron newNeuron = (Neuron) originalNeuron.cloneNeuron(this, neuronValueFunction, true);

                newNeuron.setOutputNeuron(true); // Markiere als Output-Neuron
                this.outputNeuronArr[outputNeuronPos] = newNeuron;
                neuronMap.put(originalNeuron, newNeuron);
            }

            // Kopiere alle Synapsen mit den korrekten Verbindungen zwischen den neuen Neuronen
            int synapseIndex = 0;
            for (int synapsePos = 0; synapsePos < original.synapseArray.length; synapsePos++) {
                final Synapse originalSynapse = original.synapseArray[synapsePos];
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
            this.inputNeuronArr = original.inputNeuronArr;
            this.outputNeuronArr = original.outputNeuronArr;
            // Synapsen-Array initialisieren
            this.synapseArray = original.synapseArray;

            this.disableLayerDeactivation = original.disableLayerDeactivation;
            this.fixedHiddenLayerCount = original.fixedHiddenLayerCount;
            this.enableNeuronType = original.enableNeuronType;

            // Execution views share genes but own their scheduling flags/counters.
            this.hiddenLayerArr = new Layer[original.hiddenLayerArr.length];
            for (int i = 0; i < hiddenLayerArr.length; i++) {
                this.hiddenLayerArr[i] = Layer.executionView(original.hiddenLayerArr[i],
                        i >= original.fixedHiddenLayerCount || original.hiddenLayerArr[i].isActiveLayer());
            }
            this.neuronTypeInfoDataList = original.neuronTypeInfoDataList;
        }
        context.visiting.remove(original);
        this.copyContext = null; // Never retain the parent's graph after copying.
    }

    private static final class CopyContext {
        final NeuronValueFunctionFactory factory;
        final Map<NeuronTypeInfoData, NeuronTypeInfoData> types = new IdentityHashMap<>();
        final Map<NeuralNet, NeuralNet> networks = new IdentityHashMap<>();
        final Set<NeuralNet> visiting = Collections.newSetFromMap(new IdentityHashMap<>());
        CopyContext(NeuronValueFunctionFactory factory) { this.factory = factory; }

        NeuronTypeInfoData copyType(NeuronTypeInfoData original) {
            NeuronTypeInfoData copy = types.get(original);
            if (copy == null) {
                NeuralNet inner = original.getNeuralNet();
                NeuralNet copiedInner = null;
                if (inner != null) {
                    if (visiting.contains(inner)) throw new IllegalArgumentException("Recursive neuron types cannot be executed");
                    copiedInner = networks.get(inner);
                    if (copiedInner == null) copiedInner = new NeuralNet(inner, true, factory, factory.create(), this);
                }
                copy = new NeuronTypeInfoData(original.getInputCount(), original.getOutputCount(), copiedInner);
                types.put(original, copy);
            }
            return copy;
        }
    }

    NeuronTypeInfoData copyNeuronType(NeuronTypeInfoData original, NeuronValueFunctionFactory factory) {
        return (copyContext == null ? new CopyContext(factory) : copyContext).copyType(original);
    }

    NeuronTypeInfoData copyScalarType(NeuronTypeInfoData original) {
        if (original.getNeuralNet() != null) throw new IllegalArgumentException("Scalar type has an inner network");
        return copyContext == null ? new NeuronTypeInfoData(original.getInputCount(), original.getOutputCount(), null)
                : copyContext.copyType(original);
    }

    boolean hasSameTopology(NeuralNet genotype) {
        if (inputNeuronArr != genotype.inputNeuronArr || outputNeuronArr != genotype.outputNeuronArr
                || synapseArray != genotype.synapseArray || hiddenLayerArr.length != genotype.hiddenLayerArr.length) return false;
        for (int i = 0; i < hiddenLayerArr.length; i++) {
            if (hiddenLayerArr[i].getNeuronsArr() != genotype.hiddenLayerArr[i].getNeuronsArr()) return false;
        }
        return true;
    }

    /**
     * Constructs a network with multiple hidden layers.
     *
     * @param neuronValueFunctionFactory
     * @param inputCount                 number of input neurons
     * @param hiddenCounts               sizes of each hidden layer
     * @param outputCount                number of output neurons
     * @param synapseConnectivity        Prozentsatz der zu erstellenden Synapsen (0.0-1.0)
     */
    public NeuralNet(NeuronValueFunctionFactory neuronValueFunctionFactory, final NeuronValueFunction neuronValueFunction,
                     final int inputCount, final int[] hiddenCounts, final int outputCount,
                     final double synapseConnectivity, final int fixedHiddenLayerCount) {
        this.inputNeuronArr = new Neuron[inputCount];
        this.outputNeuronArr = new Neuron[outputCount];
        // Synapsen-Array initialisieren
        this.synapseArray = new Synapse[0];

        this.fixedHiddenLayerCount = fixedHiddenLayerCount;

        // create input neurons
        for (int i = 0; i < this.inputNeuronArr.length; i++) {
            this.inputNeuronArr[i] = (Neuron) createNewNeuron(neuronValueFunctionFactory, neuronValueFunction, neuronTypeInfoDataList.get(0));
        }

        // create hidden layers
        this.hiddenLayerArr = new Layer[hiddenCounts.length];
        for (int i = 0; i < hiddenCounts.length; i++) {
            int count = hiddenCounts[i];
            Layer layer = new Layer();
            for (int j = 0; j < count; j++) {
                layer.addNeuron(createNewNeuron(neuronValueFunctionFactory, neuronValueFunction, this.neuronTypeInfoDataList.get(0)));
            }
            this.hiddenLayerArr[i] = layer;
        }

        // create output neurons
        for (int i = 0; i < this.outputNeuronArr.length; i++) {
            Neuron outputNeuron = (Neuron) createNewNeuron(neuronValueFunctionFactory, neuronValueFunction, this.neuronTypeInfoDataList.get(0));
            outputNeuron.setOutputNeuron(true); // Markiere als Output-Neuron
            this.outputNeuronArr[i] = outputNeuron;
        }

        // connect layers sequentially: input -> hidden1 -> ... -> hiddenN -> output
        NeuronInterface[] prev = this.inputNeuronArr;
        for (Layer layer : this.hiddenLayerArr) {
            this.connectLayers(prev, layer.getNeuronsArr(), synapseConnectivity);
            prev = layer.getNeuronsArr();
        }
        this.connectLayers(prev, this.outputNeuronArr, synapseConnectivity);
    }

    /**
     * Processes the inputs through the network and returns the outputs.
     */
    public double[] process(final NeuronValueFunction neuronValueFunction) {
        return process(neuronValueFunction, true);
    }

    double[] process(final NeuronValueFunction neuronValueFunction, boolean recordTraining) {
        return process(neuronValueFunction, recordTraining, true);
    }

    double[] process(final NeuronValueFunction neuronValueFunction, boolean recordTraining, boolean collectOutputs) {
        return process(neuronValueFunction, recordTraining, collectOutputs, executionOptimizationEnabled);
    }

    double[] process(final NeuronValueFunction neuronValueFunction, boolean recordTraining, boolean collectOutputs,
                     boolean optimize) {
        if (executionPlan != null && !executionPlan.matches(this)) {
            if (skippedState) throw new IllegalStateException(
                    "Network topology changed after optimized execution; copy the genotype or clear recurrent state first");
            executionPlan = null;
        }
        optimizingModules = optimize && executionOptimizationEnabled && !recordTraining;
        usingExecutionPlan = optimizingModules && !executionPlanUnneeded;
        if (usingExecutionPlan && executionPlan == null) {
            executionPlan = new NetworkExecutionPlan(this);
            if (!executionPlan.matches(this)) throw new IllegalStateException("Topology changed during plan preparation");
            if (!executionPlan.skipsNeurons) {
                // Dense/empty modules have nothing to skip. Avoid paying validation on every call.
                executionPlan = null;
                usingExecutionPlan = false;
                executionPlanUnneeded = true;
            }
        }
        if (recordTraining) {
            if (forwardOrder == null) forwardOrder = new IdentityHashMap<>();
            if (forwardSources == null) forwardSources = new IdentityHashMap<>();
            forwardOrder.clear(); forwardSources.clear();
            for (Neuron neuron : inputNeuronArr) forwardOrder.put(neuron, 0);
        } else { forwardOrder = null; forwardSources = null; }
        this.proccessedSynapses = 0L;
        final double[] outputArr = collectOutputs ? new double[this.outputNeuronArr.length] : null;

        // process hidden layers
        for (int layerIndex = 0; layerIndex < this.hiddenLayerArr.length; layerIndex++) {
            final Layer layer = this.hiddenLayerArr[layerIndex];
            final NeuronInterface[] neuronArr = layer.getNeuronsArr();
            if (layer.isActiveLayer() || this.disableLayerDeactivation) {
                for (int j = 0; j < neuronArr.length; j++) {
                    if (!usingExecutionPlan || executionPlan.executes(layerIndex, j))
                        this.proccessedSynapses += this.activate(neuronValueFunction, neuronArr[j]);
                }
                if (usingExecutionPlan) {
                    this.proccessedSynapses += executionPlan.skippedWork[layerIndex];
                    skippedState |= executionPlan.skipsNeurons;
                }
            } else {
                if (layerIndex < fixedHiddenLayerCount) {
                    // Cell-state layers are fully disabled, including their first neuron.
                    for (NeuronInterface neuron : neuronArr) {
                        for (int port = 0; port < neuron.getNeuronTypeInfoData().getOutputCount(); port++) {
                            neuronValueFunction.writeValue(this, neuron, port, 0);
                        }
                    }
                } else if (neuronArr.length > 0) {
                    // Dynamic layers still evaluate their scheduling neuron.
                    this.proccessedSynapses += this.activate(neuronValueFunction, layer.getNeuronsArr()[0]);
                }
            }
        }

        // process output layer
        for (final NeuronInterface neuron : this.outputNeuronArr) {
            this.proccessedSynapses += this.activate(neuronValueFunction, neuron);
        }

        // Collect outputs
        for (int outputNeuronPos = 0; collectOutputs && outputNeuronPos < this.outputNeuronArr.length; outputNeuronPos++) {
            final int outputTypePos = 0; // Default-Output-Type for output neurons.
            outputArr[outputNeuronPos] = neuronValueFunction.readValue(this, this.outputNeuronArr[outputNeuronPos], outputTypePos);
        }

        // process hidden layer activation counters (ohne fixed layers)
        if (!this.disableLayerDeactivation) {
            for (int layerPos = this.fixedHiddenLayerCount; layerPos < hiddenLayerArr.length; layerPos++) {
                final Layer layer = this.hiddenLayerArr[layerPos];
                final NeuronInterface[] neuronArr = layer.getNeuronsArr();
                if (neuronArr.length > 0) {
                    final NeuronInterface activationNeuron = neuronArr[0];
                    final int activationOutputTypePos = 0; // Default-Output-Type for activation.
                    // Even a silent controller eventually wakes its payload (at most 1000 calls).
                    final double increment = Math.max(1.0D / 1000,
                            Math.min(1, neuronValueFunction.readValue(this, activationNeuron, activationOutputTypePos)));
                    final double activationValue = layer.getActivationCounter() + increment;
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

        return outputArr;
    }

    public double getOutputValue(final NeuronValueFunction neuronValueFunction, final int outputNeuronPos) {
        final int outputTypePos = 0; // Default-Output-Type for output neurons.
        return neuronValueFunction.readValue(this, this.outputNeuronArr[outputNeuronPos], outputTypePos);
    }

    /**
     * Mutat this neural network.
     *
     * @param mutationRate               Probability of each weight/bias being mutated
     * @param mutationStrength           Maximum amount of mutation
     */
    public void mutate(final NeuronValueFunctionFactory neuronValueFunctionFactory,
                       final NeuronValueFunction neuronValueFunction,
                       final double mutationRate, final double mutationStrength, final boolean mutateNeuronType) {
        mutate(neuronValueFunctionFactory, neuronValueFunction, mutationRate, mutationStrength, mutateNeuronType,
                Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private void mutate(NeuronValueFunctionFactory neuronValueFunctionFactory, NeuronValueFunction neuronValueFunction,
                        double mutationRate, double mutationStrength, boolean mutateNeuronType, Set<NeuralNet> visited) {
        if (!visited.add(this)) return;
        if (mutationRate <= 0) return;
        final Random random = NeuralNetwork.getRandom();

        // Mutate weights and biases
        this.applyToAllNeurons(neuron -> {
            if (random.nextDouble() < mutationRate) {
                final double mutation = (random.nextDouble() * 2.0D - 1.0D) * mutationStrength;
                final int inputTypePos = random.nextInt(neuron.getNeuronTypeInfoData().getInputCount());
                neuron.setBias(inputTypePos, neuron.getBias(inputTypePos) + mutation);
            }
        });

        for (final Synapse synapse : this.synapseArray) {
            if (random.nextDouble() < mutationRate) {
                final double mutation = (random.nextDouble() * 2.0D - 1.0D) * mutationStrength;
                synapse.setWeight(synapse.getWeight() + mutation);
            }
        }

        // Apply structural mutations
        this.applyStructuralMutations(neuronValueFunctionFactory, neuronValueFunction, mutationRate, mutationStrength, mutateNeuronType);
        pruneUnusedNeuronTypes();
        List<NeuralNet> nested = new ArrayList<>();
        Set<NeuralNet> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (NeuronTypeInfoData type : neuronTypeInfoDataList)
            if (type.getNeuralNet() != null && seen.add(type.getNeuralNet())) nested.add(type.getNeuralNet());
        applyToAllNeurons(neuron -> {
            NeuralNet definition = neuron.getNeuronTypeInfoData().getNeuralNet();
            if (definition != null && seen.add(definition)) nested.add(definition);
        });
        for (NeuralNet definition : nested) {
            NeuronValueFunction local = neuronValueFunctionFactory.create();
            local.reserveExistingIds(definition);
            definition.mutate(neuronValueFunctionFactory, local, mutationRate, mutationStrength, mutateNeuronType, visited);
        }
    }

    // Keep a bounded nursery of unused innovations so new types can later be instantiated.
    static final int UNUSED_TYPE_LIMIT = 8;
    private void pruneUnusedNeuronTypes() {
        Set<NeuronTypeInfoData> used = Collections.newSetFromMap(new IdentityHashMap<>());
        applyToAllNeurons(neuron -> used.add(neuron.getNeuronTypeInfoData()));
        int unused = 0;
        for (int i = neuronTypeInfoDataList.size() - 1; i >= 1; i--) {
            if (!used.contains(neuronTypeInfoDataList.get(i)) && ++unused > UNUSED_TYPE_LIMIT)
                neuronTypeInfoDataList.remove(i);
        }
    }

    public void applyStructuralMutations(final NeuronValueFunctionFactory neuronValueFunctionFactory, final NeuronValueFunction neuronValueFunction,
                                         final double mutationRate, final double mutationStrength, final boolean mutateNeuronType) {
        // Structural mutations should be much less frequent than weight/bias mutations
        final double structuralMutationRate = mutationRate * DEFAULT_MUTATION_RATE; // Only 10% of the regular mutation rate

        final Random random = NeuralNetwork.getRandom();

        // Mutiere die Neuronen-Typen.
        if (this.enableNeuronType && mutateNeuronType && random.nextDouble() < structuralMutationRate) {
            this.mutateNeuronTypeInfoDataList(neuronValueFunctionFactory, neuronValueFunction, random, mutationRate, structuralMutationRate);
        }

        // Möglichkeit, ein komplettes Hidden Layer hinzuzufügen
        int addLayerRange = (this.hiddenLayerArr.length - this.fixedHiddenLayerCount) + 1;
        if (addLayerRange > 0 && random.nextDouble() < structuralMutationRate) {
            final int pos = random.nextInt(addLayerRange) + this.fixedHiddenLayerCount;
            // Zufällige Neuronenzahl zwischen 1 und 5
            int neuronCount = 1 + random.nextInt(5);
            this.addHiddenLayer(neuronValueFunctionFactory, neuronValueFunction, pos, neuronCount, structuralMutationRate);
        }

        // Möglichkeit, ein Hidden Layer zu entfernen
        int removeLayerRange = (this.hiddenLayerArr.length - this.fixedHiddenLayerCount);
        if (removeLayerRange > 0 && random.nextDouble() < structuralMutationRate * 1.5D) { // Erhöhte Wahrscheinlichkeit
            final int pos = random.nextInt(removeLayerRange) + this.fixedHiddenLayerCount;
            this.removeHiddenLayer(neuronValueFunction, pos);
        }

        // Wahrscheinlichkeit, ein Neuron zu einem bestehenden Layer hinzuzufügen
        int addNeuronLayerCount = this.hiddenLayerArr.length - this.fixedHiddenLayerCount;
        if (addNeuronLayerCount > 0 &&
                random.nextDouble() < structuralMutationRate * 1.5D) { // Verdoppelte Wahrscheinlichkeit
            int li = random.nextInt(addNeuronLayerCount) + this.fixedHiddenLayerCount;
            this.addNeuronToHiddenLayer(neuronValueFunctionFactory, neuronValueFunction, random, li, structuralMutationRate);
        }

        // Wahrscheinlichkeit, ein Neuron zu entfernen
        if (this.hiddenLayerArr.length > 0 && random.nextDouble() < structuralMutationRate) {
            int li = random.nextInt(this.hiddenLayerArr.length);
            this.removeNeuronFromHiddenLayer(neuronValueFunction, random, li);
        }

        // Wahrscheinlichkeit, eine zufällige Synapse hinzuzufügen
        if (random.nextDouble() < (structuralMutationRate * 3.0D)) { // erhöhte Wahrscheinlichkeit
            this.addRandomSynapse(random);
        }

        // Wahrscheinlichkeit, eine zufällige Synapse zu entfernen
        if (random.nextDouble() < structuralMutationRate) {
            this.removeRandomSynapse(random);
        }

        // Neue Mutation: Aktivierungsfunktion eines zufälligen Neurons ändern
        if (this.hiddenLayerArr.length > 0 &&
                (random.nextDouble() < structuralMutationRate)) {
            // Wähle ein zufälliges Hidden Layer
            List<NeuronInterface> layer = this.hiddenLayerArr[random.nextInt(this.hiddenLayerArr.length)].getNeuronList();
            if (!layer.isEmpty()) {
                final NeuronInterface neuron = layer.get(random.nextInt(layer.size()));

                neuron.mutateNeuron(random, mutationRate, mutationStrength);
            }
        }
    }

    void mutateNeuronTypeInfoDataList(final NeuronValueFunctionFactory factory,
                                      final NeuronValueFunction outerValues,
                                      Random random, double mutationRate, double structuralMutationRate) {
        NeuronTypeInfoData selected = neuronTypeInfoDataList.get(random.nextInt(neuronTypeInfoDataList.size()));
        NeuronValueFunction local = factory.create();
        NeuralNet variant;
        if (selected.getNeuralNet() == null) {
            int inputs = 1 + random.nextInt(7), outputs = 1 + random.nextInt(7);
            variant = new NeuralNet(factory, local, inputs, new int[]{inputs}, outputs,
                    .2 + random.nextDouble() * .8, 0);
        } else {
            variant = new NeuralNet(selected.getNeuralNet(), true, factory, local);
            // Interface changes only affect a new type, never live module instances.
            if (random.nextDouble() < structuralMutationRate) {
                int change = random.nextInt(3) - 1;
                if (change < 0 && variant.getInputCount() > 1) variant.removeInputNeuron(local, random.nextInt(variant.getInputCount()));
                if (change > 0) variant.addNewNeuronToInputLayer(factory, local, random, .2 + random.nextDouble() * .8);
            }
            if (random.nextDouble() < structuralMutationRate) {
                int change = random.nextInt(3) - 1;
                if (change < 0 && variant.getOutputLayerSize() > 1) variant.removeOutputNeuron(local, random.nextInt(variant.getOutputLayerSize()));
                if (change > 0) variant.addNewNeuronToOutputLayer(factory, local, random, .2 + random.nextDouble() * .8);
            }
        }
        neuronTypeInfoDataList.add(new NeuronTypeInfoData(variant.getInputCount(),variant.getOutputLayerSize(),variant));
        pruneUnusedNeuronTypes();
    }

    /**
     * Creates a mutated copy with more radikalen Änderungen wenn nötig.
     * Diese Methode ermöglicht stärkere Mutationen als die Standardmethode.
     */
    public void mutateAggressively(NeuronValueFunctionFactory neuronValueFunctionFactory, final NeuronValueFunction neuronValueFunction, double mutationRate, double mutationStrength) {
        final Random random = NeuralNetwork.getRandom();

        // 50% Chance für eine zusätzliche Strukturmutation
        if (random.nextDouble() < 0.5) {
            // Füge ein zusätzliches Hidden Layer hinzu
            int pos = this.fixedHiddenLayerCount + random.nextInt(this.hiddenLayerArr.length - this.fixedHiddenLayerCount + 1);
            int neuronCount = 2 + random.nextInt(4); // 2-5 Neuronen
            this.addHiddenLayer(neuronValueFunctionFactory, neuronValueFunction, pos, neuronCount, 1.0);
        }

        // 50% Chance für mehrere zusätzliche Neuronen
        if (random.nextDouble() < 0.5 && this.hiddenLayerArr.length > this.fixedHiddenLayerCount) {
            int count = 1 + random.nextInt(3); // 1-3 neue Neuronen
            for (int i = 0; i < count; i++) {
                int layer = this.fixedHiddenLayerCount + random.nextInt(this.hiddenLayerArr.length - this.fixedHiddenLayerCount);
                this.addNeuronToHiddenLayer(neuronValueFunctionFactory, neuronValueFunction, random, layer, mutationStrength);
            }
        }

        // 70% Chance für mehrere zusätzliche Synapsen
        if (random.nextDouble() < 0.7) {
            int count = 2 + random.nextInt(4); // 2-5 neue Synapsen
            for (int i = 0; i < count; i++) {
                this.addRandomSynapse(random);
            }
        }
    }

    public void addRandomSynapse(final Random random) {
        // Wählen Sie zufällige Quelle und Ziel-Layer
        List<List<NeuronInterface>> allLayers = new ArrayList<>();
        allLayers.add(Arrays.asList(this.inputNeuronArr));
        for (Layer l : this.hiddenLayerArr) allLayers.add(l.getNeuronList());
        allLayers.add(Arrays.asList(this.outputNeuronArr));

        int sourceLayerIndex = random.nextInt(allLayers.size());
        // Exclude the input layer for the target.
        int targetLayerIndex = random.nextInt(allLayers.size() - 1) + 1;

        // Make sure we're not connecting within the same layer
        if (sourceLayerIndex == targetLayerIndex) {
            // With only input and output there is no distinct, legal target.
            if (allLayers.size() == 2) return;
            targetLayerIndex = targetLayerIndex % (allLayers.size() - 1) + 1;
        }

        List<NeuronInterface> sourceLayer = allLayers.get(sourceLayerIndex);
        List<NeuronInterface> targetLayer = allLayers.get(targetLayerIndex);

        if (sourceLayer.isEmpty() || targetLayer.isEmpty()) return;

        // Wählen Sie zufällige Neuronen aus den beiden Layern
        NeuronInterface sourceNeuron = sourceLayer.get(random.nextInt(sourceLayer.size()));
        NeuronInterface targetNeuron = targetLayer.get(random.nextInt(targetLayer.size()));

        // Erstellen Sie die Synapse
        Synapse synapse = new Synapse(sourceNeuron,
                sourceNeuron.getNeuronTypeInfoData().getOutputCount() == 1 ? 0 : random.nextInt(sourceNeuron.getNeuronTypeInfoData().getOutputCount()),
                targetNeuron,
                targetNeuron.getNeuronTypeInfoData().getInputCount() == 1 ? 0 : random.nextInt(targetNeuron.getNeuronTypeInfoData().getInputCount()),
                random.nextDouble() * 0.002D - 0.001D);
        addSynapse(synapse);
    }

    /**
     * Adds a hidden layer and ensures it is active by default.
     * Existing connections between adjacent layers are preserved.
     *
     * @param neuronValueFunctionFactory
     * @param index                      Position, an der der neue Layer eingefügt werden soll
     * @param neuronCount                Anzahl der Neuronen im neuen Layer
     * @param connectivity               Prozentsatz der zu erstellenden Synapsen (0.0-1.0)
     */
    public void addHiddenLayer(final NeuronValueFunctionFactory neuronValueFunctionFactory, final NeuronValueFunction neuronValueFunction, final int index, final int neuronCount, double connectivity) {
        // Begrenze die Konnektivität auf gültige Werte
        connectivity = Math.max(0.0, Math.min(1.0, connectivity));

        final Random random = NeuralNetwork.getRandom();

        final Layer newLayer = new Layer();
        newLayer.setActiveLayer(true); // Set new layer as active by default

        // Erstelle die neuen Neuronen
        for (int i = 0; i < neuronCount; i++) {
            int typeIndex = random.nextInt(this.neuronTypeInfoDataList.size());
            newLayer.addNeuron(createNewNeuron(neuronValueFunctionFactory, neuronValueFunction,
                    this.neuronTypeInfoDataList.get(i == 0 ? 0 : typeIndex)));
        }

        // Bestimme die angrenzenden Layer
        final List<NeuronInterface> prevLayer = index == 0 ? Arrays.asList(this.inputNeuronArr) : this.hiddenLayerArr[index - 1].getNeuronList();
        final List<NeuronInterface> nextLayer = index == this.hiddenLayerArr.length ? Arrays.asList(this.outputNeuronArr) : this.hiddenLayerArr[index].getNeuronList();

        // Füge den neuen Layer ein
        this.hiddenLayerArr = insertLayer(this.hiddenLayerArr, index, newLayer);

        // Verbinde den vorherigen Layer mit dem neuen Layer
        for (final NeuronInterface srcNeuron : prevLayer) {
            for (final NeuronInterface tgtNeuron : newLayer.getNeuronsArr()) {
                if (connectivity >= 1.0D || random.nextDouble() < connectivity) {
                    final double weight = random.nextDouble() * 0.002D - 0.001D;
                    addSynapse(new Synapse(srcNeuron,
                            srcNeuron.getNeuronTypeInfoData().getOutputCount() == 1 ? 0 : random.nextInt(srcNeuron.getNeuronTypeInfoData().getOutputCount()),
                            tgtNeuron,
                            tgtNeuron.getNeuronTypeInfoData().getInputCount() == 1 ? 0 : random.nextInt(tgtNeuron.getNeuronTypeInfoData().getInputCount()),
                            weight));
                }
            }
        }

        // Verbinde den neuen Layer mit dem nächsten Layer
        for (final NeuronInterface srcNeuron : newLayer.getNeuronsArr()) {
            for (final NeuronInterface tgtNeuron : nextLayer) {
                if (connectivity >= 1.0D || random.nextDouble() < connectivity) {
                    final double weight = random.nextDouble() * 0.002D - 0.001D;
                    addSynapse(new Synapse(srcNeuron,
                            srcNeuron.getNeuronTypeInfoData().getOutputCount() == 1 ? 0 : random.nextInt(srcNeuron.getNeuronTypeInfoData().getOutputCount()),
                            tgtNeuron,
                            tgtNeuron.getNeuronTypeInfoData().getInputCount() == 1 ? 0 : random.nextInt(tgtNeuron.getNeuronTypeInfoData().getInputCount()),
                            weight));
                }
            }
        }
    }

    private NeuronInterface createNewNeuron(final NeuronValueFunctionFactory neuronValueFunctionFactory,
                                   final NeuronValueFunction neuronValueFunction,
                                   final NeuronTypeInfoData neuronTypeInfoDataList) {
        final NeuronInterface retNeuron;

        // TODO this.enableNeuronType

        if (Objects.nonNull(neuronTypeInfoDataList.getNeuralNet())) {
            retNeuron = new NeuronNetwork(neuronValueFunction.fetchNextFreeId(this),
                    neuronTypeInfoDataList, neuronValueFunctionFactory);
        } else {
            retNeuron = new Neuron(neuronValueFunction.fetchNextFreeId(this),
                    neuronTypeInfoDataList);
        }
        return retNeuron;
    }

    private void removeHiddenLayer(final NeuronValueFunction neuronValueFunction, final int pos) {
        if (pos < this.fixedHiddenLayerCount) throw new IllegalArgumentException("Cannot remove a fixed layer");
        final Layer layer = this.hiddenLayerArr[pos];
        this.hiddenLayerArr = removeLayer(this.hiddenLayerArr, pos);
        for (NeuronInterface neuron : layer.getNeuronsArr()) {
            this.removeNeuronNeuronSynapses(neuron);
            neuronValueFunction.releaseNeuron(this, neuron);
        }
    }

    public void addNeuronToHiddenLayer(final NeuronValueFunctionFactory neuronValueFunctionFactory, final NeuronValueFunction neuronValueFunction, final Random random, final int layerIndex, final double connectivity) {
        final  NeuronInterface newNeuron = createNewNeuron(neuronValueFunctionFactory, neuronValueFunction, this.neuronTypeInfoDataList.get(random.nextInt(this.neuronTypeInfoDataList.size())));

        this.createPrevSynapses(random, layerIndex, connectivity, newNeuron);
        this.createNextSynapses(random, layerIndex, connectivity, newNeuron);

        this.hiddenLayerArr[layerIndex].addNeuron(newNeuron);
    }

    private void createPrevSynapses(final Random random, final int layerIndex, final double connectivity, final NeuronInterface newNeuron) {
        final List<NeuronInterface> prevNeuronList = layerIndex == 0 ? Arrays.asList(this.inputNeuronArr) : this.hiddenLayerArr[layerIndex - 1].getNeuronList();
        for (final NeuronInterface srcNeuron : prevNeuronList) {
            if (connectivity >= 1.0 || random.nextDouble() < connectivity) {
                final double weight = random.nextDouble() * 0.002D - 0.001D;
                this.addSynapse(new Synapse(srcNeuron,
                        srcNeuron.getNeuronTypeInfoData().getOutputCount() == 1 ? 0 : random.nextInt(srcNeuron.getNeuronTypeInfoData().getOutputCount()),
                        newNeuron,
                        newNeuron.getNeuronTypeInfoData().getInputCount() == 1 ? 0 : random.nextInt(newNeuron.getNeuronTypeInfoData().getInputCount()),
                        weight));
            }
        }
    }

    private void createNextSynapses(final Random random, int layerIndex, final double connectivity, final NeuronInterface newNeuron) {
        final List<NeuronInterface> nextNeuronList = layerIndex == this.hiddenLayerArr.length - 1 ? Arrays.asList(this.outputNeuronArr) : this.hiddenLayerArr[layerIndex + 1].getNeuronList();
        for (final NeuronInterface tgtNeuron : nextNeuronList) {
            if (connectivity >= 1.0 || random.nextDouble() < connectivity) {
                final double weight = random.nextDouble() * 0.002D - 0.001D;
                this.addSynapse(new Synapse(newNeuron,
                                newNeuron.getNeuronTypeInfoData().getOutputCount() == 1 ? 0 : random.nextInt(newNeuron.getNeuronTypeInfoData().getOutputCount()),
                        tgtNeuron,
                        tgtNeuron.getNeuronTypeInfoData().getInputCount() == 1 ? 0 : random.nextInt(tgtNeuron.getNeuronTypeInfoData().getInputCount()),
                        weight));
            }
        }
    }

    // Hilfsmethode zum Entfernen eines Layers aus einem Array
    private static Layer[] removeLayer(final Layer[] arr, final int index) {
        final Layer[] result = new Layer[arr.length - 1];
        System.arraycopy(arr, 0, result, 0, index);
        System.arraycopy(arr, index + 1, result, index, arr.length - index - 1);
        return result;
    }

    public void removeNeuronFromHiddenLayer(final NeuronValueFunction neuronValueFunction, final Random random, final int layerIndex) {
        final Layer layer = this.hiddenLayerArr[layerIndex];
        final List<NeuronInterface> layerNeuronList = layer.getNeuronList();
        if (layerNeuronList.size() > 1) {
            // Skip the first neuron to avoid removing activation neurons
            final int idx = random.nextInt(layerNeuronList.size() - 1) + 1;
            this.removeNeuron(neuronValueFunction, layer, idx);
        }
    }

    private void removeNeuron(final NeuronValueFunction neuronValueFunction, final Layer layer, final int neuronIdx) {
        final NeuronInterface removedNeuron = layer.getNeuron(neuronIdx);
        layer.removeNeuron(neuronIdx);

        this.removeNeuronNeuronSynapses(removedNeuron);

        neuronValueFunction.releaseNeuron(this, removedNeuron);
    }

    /** Backward graph closure, including all dynamic scheduling roots and all recurrent edges. */
    void removeUnobservableHiddenNeurons(NeuronValueFunctionFactory factory, NeuronValueFunction values,
                                        Set<NeuralNet> visited) {
        if (!visited.add(this)) return;
        Set<NeuronInterface> needed = Collections.newSetFromMap(new IdentityHashMap<>());
        ArrayDeque<NeuronInterface> pending = new ArrayDeque<>();
        for (Neuron output : outputNeuronArr) if (needed.add(output)) pending.add(output);
        // Always keep controllers, even with scheduling disabled: the flag can change later.
        for (int i = fixedHiddenLayerCount; i < hiddenLayerArr.length; i++) {
            Layer layer = hiddenLayerArr[i];
            if (layer.size() > 0 && needed.add(layer.getNeuron(0))) pending.add(layer.getNeuron(0));
        }
        while (!pending.isEmpty()) {
            NeuronInterface target = pending.removeFirst();
            for (int port = 0; port < target.getNeuronTypeInfoData().getInputCount(); port++) {
                for (Synapse edge : target.getInputSynapseArr(port)) {
                    // Even a zero weight is kept: IEEE arithmetic with nonfinite inputs is observable.
                    NeuronInterface source = edge.getSourceNeuron();
                    if (needed.add(source)) pending.add(source);
                }
            }
        }
        // Keep layer order, fixed-state slots, input/output interfaces and surviving summation order.
        for (Layer layer : hiddenLayerArr)
            for (int i = layer.size() - 1; i >= 0; i--)
                if (!needed.contains(layer.getNeuron(i))) removeNeuron(values, layer, i);

        // Definitions can be shared by several instances. Simplify each once, preserving every port.
        // Leave unused type candidates in the nursery intact; they are latent genetic material.
        for (Layer layer : hiddenLayerArr) for (NeuronInterface neuron : layer.getNeuronsArr()) {
            NeuralNet inner = neuron.getNeuronTypeInfoData().getNeuralNet();
            if (inner != null) inner.removeUnobservableHiddenNeurons(factory, factory.create(), visited);
        }
        values.clearModuleStates();
    }

    private void removeInputNeuron(final NeuronValueFunction neuronValueFunction, final int removeNeuronPos) {
        final Neuron removedNeuron = this.inputNeuronArr[removeNeuronPos];

        final Neuron[] newInputNeuronArr = removeNeuronFromArray(this.inputNeuronArr, removeNeuronPos);

        this.inputNeuronArr = newInputNeuronArr;

        //for (int pos = 0; pos < this.inputNeuronArr.length; pos++) {
        //    if (this.inputNeuronArr[pos] == null) {
        //        throw new RuntimeException("Input neuron at position " + pos + " is null after removal.");
        //    }
        //}

        this.removeNeuronNeuronSynapses(removedNeuron);

        neuronValueFunction.releaseNeuron(this, removedNeuron);
    }

    private void removeOutputNeuron(final NeuronValueFunction neuronValueFunction, final int removeNeuronPos) {
        final Neuron removedNeuron = this.outputNeuronArr[removeNeuronPos];

        final Neuron[] newOutputNeuronArr = removeNeuronFromArray(this.outputNeuronArr, removeNeuronPos);

        this.outputNeuronArr = newOutputNeuronArr;

        this.removeNeuronNeuronSynapses(removedNeuron);

        neuronValueFunction.releaseNeuron(this, removedNeuron);
    }

    public void addNewNeuronToInputLayer(final NeuronValueFunctionFactory neuronValueFunctionFactory, final NeuronValueFunction neuronValueFunction, final Random random, final double connectivity) {
        final  Neuron newNeuron = (Neuron) createNewNeuron(neuronValueFunctionFactory, neuronValueFunction, this.neuronTypeInfoDataList.get(0));

        final Neuron[] newInputNeuronArr = addNeuronToArr(this.inputNeuronArr, newNeuron);

        this.inputNeuronArr = newInputNeuronArr;

        //for (int pos = 0; pos < this.inputNeuronArr.length; pos++) {
        //    if (this.inputNeuronArr[pos] == null) {
        //        throw new RuntimeException("Input neuron at position " + pos + " is null after addNewNeuro.");
        //    }
        //}

        this.createNextSynapses(random, -1, connectivity, newNeuron);
    }

    public void addNewNeuronToOutputLayer(final NeuronValueFunctionFactory neuronValueFunctionFactory, final NeuronValueFunction neuronValueFunction, final Random random, final double connectivity) {
        final  Neuron newNeuron = (Neuron) createNewNeuron(neuronValueFunctionFactory, neuronValueFunction, this.neuronTypeInfoDataList.get(0));

        newNeuron.setOutputNeuron(true);

        final Neuron[] newOutputNeuronArr = addNeuronToArr(this.outputNeuronArr, newNeuron);

        this.outputNeuronArr = newOutputNeuronArr;

        this.createPrevSynapses(random, this.hiddenLayerArr.length, connectivity, newNeuron);
    }

    private static Neuron[] addNeuronToArr(Neuron[] neuronArr, Neuron newNeuron) {
        final Neuron[] newInputNeuronArr = new Neuron[neuronArr.length + 1];
        System.arraycopy(neuronArr, 0, newInputNeuronArr, 0, neuronArr.length);
        newInputNeuronArr[newInputNeuronArr.length - 1] = newNeuron;
        return newInputNeuronArr;
    }

    private static Neuron[] removeNeuronFromArray(final Neuron[] sourceNeuronArr, final int removeNeuronPos) {
        final Neuron[] newNeuronArr = new Neuron[sourceNeuronArr.length - 1];
        System.arraycopy(sourceNeuronArr, 0, newNeuronArr, 0, removeNeuronPos);
        System.arraycopy(sourceNeuronArr, removeNeuronPos + 1, newNeuronArr, removeNeuronPos, sourceNeuronArr.length - removeNeuronPos - 1);
        return newNeuronArr;
    }

    private void removeNeuronNeuronSynapses(final NeuronInterface removedNeuron) {
        final List<Synapse> retained = new ArrayList<>(this.synapseArray.length);
        for (final Synapse synapse : this.synapseArray) {
            final NeuronInterface sourceNeuron = synapse.getSourceNeuron();
            final NeuronInterface targetNeuron = synapse.getTargetNeuron();
            if (sourceNeuron == removedNeuron || targetNeuron == removedNeuron) {
                final int outputTypePos = synapse.getSourceOutputTypePos();
                sourceNeuron.removeOutputSynapse(outputTypePos, synapse);
                int inputTypePos = synapse.getTargetInputTypePos();
                targetNeuron.removeInputSynapse(inputTypePos, synapse);
            } else {
                retained.add(synapse);
            }
        }
        this.synapseArray = retained.toArray(Synapse[]::new);
    }

    public void removeRandomSynapse(final Random random) {
        if (this.synapseArray.length == 0) return;
        final int synapseIdx = random.nextInt(this.synapseArray.length);
        final Synapse removedSynapse = this.synapseArray[synapseIdx];
        //removedSynapse.getSourceNeuron().getOutputSynapseList(removedSynapse.getSourceOutputTypePos()).remove(removedSynapse);
        removedSynapse.getSourceNeuron().removeOutputSynapse(removedSynapse.getSourceOutputTypePos(), removedSynapse);
        removedSynapse.getTargetNeuron().removeInputSynapse(removedSynapse.getTargetInputTypePos(), removedSynapse);
        removeSynapseAt(synapseIdx);
    }

    // Hilfsmethode zum Entfernen einer Synapse an Index
    private void removeSynapseAt(int idx) {
        if (this.synapseArray.length == 0) return;
        Synapse[] newArr = new Synapse[this.synapseArray.length - 1];
        System.arraycopy(this.synapseArray, 0, newArr, 0, idx);
        System.arraycopy(this.synapseArray, idx + 1, newArr, idx, this.synapseArray.length - idx - 1);
        this.synapseArray = newArr;
    }

    // Hilfsmethode zum Einfügen eines Layers in ein Array
    private static Layer[] insertLayer(Layer[] arr, int index, Layer layer) {
        Layer[] result = new Layer[arr.length + 1];
        System.arraycopy(arr, 0, result, 0, index);
        result[index] = layer;
        System.arraycopy(arr, index, result, index + 1, arr.length - index);
        return result;
    }

    /**
     * Wendet eine Funktion auf alle Neuronen des Netzwerks an.
     * Dies ist effizienter als eine neue Liste aller Neuronen zu erstellen.
     *
     * @param function Die Funktion, die auf jedes Neuron angewendet werden soll
     */
    public void applyToAllNeurons(final Consumer<NeuronInterface> function) {
        // Anwenden auf Input-Neuronen
        for (final NeuronInterface neuron : this.inputNeuronArr) {
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
     * Trainiert das neuronale Netzwerk mittels Backpropagation.
     * Diese Methode berechnet den Fehler und aktualisiert die Gewichte und Bias-Werte.
     *
     * @param targetOutput Die erwarteten Ausgabewerte
     * @return Der quadratische Fehler des Netzwerks vor dem Training
     */
    public double backpropagate(final NeuronValueFunction neuronValueFunction, final double[] targetOutput, final double learningRate) {
        if (forwardOrder == null) throw new IllegalStateException("Training needs a recorded forward pass");
        if (targetOutput.length != this.outputNeuronArr.length) {
            throw new IllegalArgumentException("Zielausgabe muss die gleiche Größe haben wie die Ausgabeschicht");
        }

        // Berechne den Fehler vor dem Training
        final double error = this.calculateError(neuronValueFunction, targetOutput);

        // 1. Berechne den Fehler für die Ausgabeneuronen
        this.calcOutputNeuronDelta(neuronValueFunction, targetOutput);

        // 2. Backpropagiere den Fehler durch alle versteckten Schichten (von hinten nach vorne)
        this.backpropagateDelta(neuronValueFunction);

        // 3. Aktualisiere Gewichte und Bias-Werte
        this.updateBiasAndWeights(neuronValueFunction, learningRate);

        // 4. Backpropagiere die speziellen Neuronen (NeuronNetwork).
        this.backpropagateExtra(neuronValueFunction, learningRate);

        return error;
    }

    /**
     * 1. Berechne den Fehler für die Ausgabeneuronen.
     * Berechnet die Deltas für die Ausgabeneuronen basierend auf der Differenz zwischen den tatsächlichen und den erwarteten Ausgaben.
     * Diese Methode wird aufgerufen, nachdem die Ausgaben des Netzwerks berechnet wurden.
     *
     * @param targetOutput Die erwarteten Ausgabewerte
     */
    private void calcOutputNeuronDelta(final NeuronValueFunction neuronValueFunction, final double[] targetOutput) {
        for (int outputNeuronPos = this.outputNeuronArr.length - 1; outputNeuronPos >= 0; outputNeuronPos--) {
            final Neuron outputNeuron = this.outputNeuronArr[outputNeuronPos];
            for (int outputTypePos = 0; outputTypePos < outputNeuron.getNeuronTypeInfoData().getOutputCount(); outputTypePos++) {
                final double output = neuronValueFunction.readValue(this, outputNeuron, outputTypePos);
                final double target = targetOutput[outputNeuronPos];

                // Delta = (Ausgabe - Ziel) * Ableitung der Aktivierungsfunktion
                //final double inputSum = outputNeuron.getInputSum(outputTypePos);
                final double inputSum = neuronValueFunction.readInputSum(this, outputNeuron, outputTypePos);
                double errorGradient = output - target;
                for (Synapse edge : outputNeuron.getOutputSynapseList(outputTypePos))
                    if (propagatesCurrentStep(edge)) errorGradient += edge.getWeight() *
                            edge.getTargetNeuron().getInputDelta(this, neuronValueFunction, edge.getTargetInputTypePos());
                final double delta = errorGradient *
                        outputNeuron.getActivationFunction().derivative(inputSum);
                //outputNeuron.setDelta(outputTypePos, delta);
                neuronValueFunction.writeDelta(this, outputNeuron, outputTypePos, delta);
            }
        }
    }

    /**
     * 2. Backpropagiere den Fehler durch alle versteckten Schichten (von hinten nach vorne).
     * Diese Methode wird aufgerufen, nachdem die Deltas für die Ausgabeneuronen berechnet wurden.
     */
    void backpropagateDelta(final NeuronValueFunction neuronValueFunction) {
        for (int hiddenLayerPos = this.hiddenLayerArr.length - 1; hiddenLayerPos >= 0; hiddenLayerPos--) {
            final Layer layer = this.hiddenLayerArr[hiddenLayerPos];
            final NeuronInterface[] neuronArr = layer.getNeuronsArr();
            for (int i = neuronArr.length - 1; i >= 0; i--) {
                if (wasExecuted(neuronArr[i])) neuronArr[i].backpropagateDelta(this, neuronValueFunction);
            }
        }
    }

    /**
     * 3. Aktualisiert die Gewichte und Bias-Werte basierend auf den berechneten Deltas.
     * Wird als Teil des Backpropagation-Algorithmus aufgerufen.
     */
    void updateBiasAndWeights(final NeuronValueFunction neuronValueFunction, final double learningRate) {
        // Aktualisiere alle Bias-Werte und Gewichte

        // 1. Aktualisiere die Bias-Werte in allen Neuronen
        // Input-Neuronen haben normalerweise keinen Bias

        // Hidden Layer Neuronen
        for (final Layer layer : this.hiddenLayerArr) {
            final NeuronInterface[] neurons = layer.getNeuronsArr();
            for (final NeuronInterface neuron : neurons) {
                if (!wasExecuted(neuron)) continue;
                for (int inputTypePos = 0; inputTypePos < neuron.getNeuronTypeInfoData().getInputCount(); inputTypePos++) {
                    double biasDelta = learningRate * neuron.getInputDelta(this, neuronValueFunction, inputTypePos);
                    final double bias = neuron.getBias(inputTypePos);
                    neuron.setBias(inputTypePos, bias - biasDelta);
                }
            }
        }

        // Output Neuronen
        for (final Neuron neuron : this.outputNeuronArr) {
            for (int inputTypePos = 0; inputTypePos < neuron.getNeuronTypeInfoData().getInputCount(); inputTypePos++) {
                double biasDelta = learningRate * neuron.getInputDelta(this, neuronValueFunction, inputTypePos);
                final double bias = neuron.getBias(inputTypePos);
                neuron.setBias(inputTypePos, bias - biasDelta);
            }
        }

        // 2. Aktualisiere alle Synapsengewichte
        for (final Synapse synapse : this.synapseArray) {
            final NeuronInterface targetNeuron = synapse.getTargetNeuron();
            final NeuronInterface sourceNeuron = synapse.getSourceNeuron();

            if (!wasExecuted(targetNeuron)) continue;

            // A synapse connects exactly one source output to one target input.
            final double sourceValue = forwardSources.get(synapse);
            final double targetDelta = targetNeuron.getInputDelta(this, neuronValueFunction, synapse.getTargetInputTypePos());
            synapse.setWeight(synapse.getWeight() - learningRate * sourceValue * targetDelta);
        }
    }

    /**
     * 4. Backpropagiere die speziellen Neuronen (NeuronNetwork).
     */
    void backpropagateExtra(NeuronValueFunction neuronValueFunction, double learningRate) {
        for (final Layer layer : this.hiddenLayerArr) {
            final NeuronInterface[] neurons = layer.getNeuronsArr();
            for (final NeuronInterface neuron : neurons) {
                if (!wasExecuted(neuron)) continue;
                neuron.backpropagateExtra(this, neuronValueFunction, learningRate);
            }
        }

        // Output Neuronen
        for (final Neuron neuron : this.outputNeuronArr) {
            neuron.backpropagateExtra(this, neuronValueFunction, learningRate);
        }
    }

    /**
     * Berechnet den quadratischen Fehler des Netzwerks
     *
     * @param targetOutput Die erwarteten Ausgabewerte
     * @return Der quadratische Fehler
     */
    private double calculateError(final NeuronValueFunction neuronValueFunction, final double[] targetOutput) {
        double error = 0.0D;
        final int outputTypePos = 0; // Default-Output-Type für Output-Neuronen.
        for (int outputNeuronPos = 0; outputNeuronPos < this.outputNeuronArr.length; outputNeuronPos++) {
            double output = neuronValueFunction.readValue(this, this.outputNeuronArr[outputNeuronPos], outputTypePos);
            double target = targetOutput[outputNeuronPos];
            double diff = output - target;
            error += diff * diff; // Quadratischer Fehler
        }
        return error / 2.0D; // Division durch 2 ist übliche Konvention für MSE
    }

    /**
     * Sets the input values for the network.
     */
    public void setInputs(final NeuronValueFunction neuronValueFunction,
                          final double[] inputArr) {
        if (inputArr.length != this.inputNeuronArr.length) {
            throw new IllegalArgumentException("Input size mismatch");
        }
        final int outputTypePos = 0; // Default-Output-Type für Input-Neuronen.
        for (int inputPos = 0; inputPos < inputArr.length; inputPos++) {
            neuronValueFunction.writeValue(this, this.inputNeuronArr[inputPos], outputTypePos, inputArr[inputPos]);
        }
    }

    public int getInputLayerSize() {
        return this.inputNeuronArr.length;
    }

    public int getOutputLayerSize() {
        return this.outputNeuronArr.length;
    }

    /**
     * Gibt das Array der Input-Neuronen zurück
     * @return Array der Input-Neuronen
     */
    public Neuron[] getInputNeuronArr() {
        return this.inputNeuronArr;
    }

    /**
     * Gibt die Liste der Hidden-Layer zurück
     * @return Liste der Hidden-Layer
     */
    public Layer[] getHiddenLayerArr() {
        return this.hiddenLayerArr;
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
     * Gibt die Größen aller Layer (Input, Hidden, Output) im Netzwerk zurück
     * @return Array mit den Größen der einzelnen Layer
     */
    public int[] getLayerSizes() {
        int totalLayers = 2 + this.hiddenLayerArr.length; // Input + Hidden + Output
        int[] sizes = new int[totalLayers];

        // Input Layer
        sizes[0] = this.inputNeuronArr.length;

        // Hidden Layers
        for (int i = 0; i < this.hiddenLayerArr.length; i++) {
            sizes[i + 1] = this.hiddenLayerArr[i].getNeuronsArr().length;
        }

        // Output Layer
        sizes[totalLayers - 1] = this.outputNeuronArr.length;

        return sizes;
    }

    /**
     * Returns the number of input neurons in the network.
     * @return the count of input neurons
     */
    public int getInputCount() {
        return this.inputNeuronArr.length;
    }

    /** Append disconnected interface inputs without resetting existing memory or changing genes. */
    void ensureInputCount(NeuronValueFunction values, int count) {
        if (count <= inputNeuronArr.length) return;
        int oldCount = inputNeuronArr.length;
        values.reserveExistingIds(this);
        inputNeuronArr = Arrays.copyOf(inputNeuronArr, count);
        var scalar = new NeuronTypeInfoData(1, 1, null);
        for (int i = oldCount; i < count; i++)
            inputNeuronArr[i] = new Neuron(values.fetchNextFreeId(this), scalar, 0);
        // This operation only appends disconnected inputs, so a rebuilt plan is safe for old memory.
        executionPlan = null;
        executionPlanUnneeded = false;
    }

    /** Appends disconnected sigmoid outputs with bias 0 (exactly 0.5), without consuming the evolution RNG. */
    void ensureOutputCount(NeuronValueFunction values, int count) {
        if (count <= outputNeuronArr.length) return;
        int oldCount = outputNeuronArr.length;
        values.reserveExistingIds(this);
        outputNeuronArr = Arrays.copyOf(outputNeuronArr, count);
        var scalar = new NeuronTypeInfoData(1, 1, null);
        for (int i = oldCount; i < count; i++) {
            Neuron output = new Neuron(values.fetchNextFreeId(this), scalar, 0);
            output.setOutputNeuron(true);
            outputNeuronArr[i] = output;
        }
        // Only disconnected outputs are appended, so a rebuilt plan is safe for old memory.
        executionPlan = null;
        executionPlanUnneeded = false;
    }

    public void rnnClearPreviousState(final NeuronValueFunction neuronValueFunction) {
        executionPlan = null;
        skippedState = false;
        usingExecutionPlan = false;
        optimizingModules = false;
        executionPlanUnneeded = false;
        neuronValueFunction.clearModuleStates();
        forwardOrder = null; forwardSources = null;
        this.proccessedSynapses = 0;
        for (int i = 0; i < hiddenLayerArr.length; i++) {
            hiddenLayerArr[i].setActivationCounter(0);
            if (i >= fixedHiddenLayerCount) hiddenLayerArr[i].setActiveLayer(true);
        }
        applyToAllNeurons(neuron -> {
            for (int p = 0; p < neuron.getNeuronTypeInfoData().getOutputCount(); p++) {
                neuronValueFunction.writeValue(this, neuron, p, 0);
                neuronValueFunction.writeDelta(this, neuron, p, 0);
            }
            for (int p = 0; p < neuron.getNeuronTypeInfoData().getInputCount(); p++)
                neuronValueFunction.writeInputSum(this, neuron, p, 0);
        });
    }

    public int getFixedHiddenLayerCount() { return fixedHiddenLayerCount; }

    /** New genomes use independent state experts feeding the common trunk. */
    void addParallelStateLayers(NeuronValueFunctionFactory factory, NeuronValueFunction values,
                                int size, double connectivity) {
        NeuronInterface[] trunk = hiddenLayerArr.length == 0 ? outputNeuronArr : hiddenLayerArr[0].getNeuronsArr();
        for (int i = 0; i < fixedHiddenLayerCount; i++) addHiddenLayer(factory, values, i, size, 0);
        for (int i = 0; i < fixedHiddenLayerCount; i++) {
            connectLayers(inputNeuronArr, hiddenLayerArr[i].getNeuronsArr(), connectivity);
            connectLayers(hiddenLayerArr[i].getNeuronsArr(), trunk, connectivity);
        }
    }

    /**
     * Gibt die Liste der Output-Neuronen zurück
     * @return Liste der Output-Neuronen
     */
    public Neuron[] getOutputNeuronArr() {
        return this.outputNeuronArr;
    }

    /**
     * Gibt die Aktivierungswerte aller Neuronen im Netzwerk zurück.
     * Dies ist nützlich für die Analyse und das Debugging.
     *
     * @return Eine Map mit den Neuronennamen (IDs) und ihren Aktivierungswerten
     */
    public Map<String, Double> getNeuronActivations(final NeuronValueFunction neuronValueFunction) {
        final Map<String, Double> activations = new HashMap<>();

        // Füge Input-Neuronen hinzu
        for (int inputNeuronPos = 0; inputNeuronPos < this.inputNeuronArr.length; inputNeuronPos++) {
            final Neuron neuron = this.inputNeuronArr[inputNeuronPos];
            for (int outputTypePos = 0; outputTypePos < neuron.getNeuronTypeInfoData().getOutputCount(); outputTypePos++) {
                activations.put("input_" + inputNeuronPos + "_" + outputTypePos, neuronValueFunction.readValue(this, neuron, outputTypePos));
            }
        }

        // Füge Hidden-Layer-Neuronen hinzu
        for (int hiddenLayerPos = 0; hiddenLayerPos < this.hiddenLayerArr.length; hiddenLayerPos++) {
            final Layer layer = this.hiddenLayerArr[hiddenLayerPos];
            for (int neuronPos = 0; neuronPos < layer.getNeuronsArr().length; neuronPos++) {
                final NeuronInterface neuron = layer.getNeuronsArr()[neuronPos];
                for (int outputTypePos = 0; outputTypePos < neuron.getNeuronTypeInfoData().getOutputCount(); outputTypePos++) {
                    activations.put("hidden_" + hiddenLayerPos + "_" + neuronPos + "_" + outputTypePos, neuronValueFunction.readValue(this, neuron, outputTypePos));
                }
            }
        }

        // Füge Output-Neuronen hinzu
        for (int outputNeuronPos = 0; outputNeuronPos < this.outputNeuronArr.length; outputNeuronPos++) {
            final Neuron neuron = this.outputNeuronArr[outputNeuronPos];
            for (int outputTypePos = 0; outputTypePos < neuron.getNeuronTypeInfoData().getOutputCount(); outputTypePos++) {
                activations.put("output_" + outputNeuronPos + "_" + outputTypePos, neuronValueFunction.readValue(this, neuron, outputTypePos));
            }
        }
        return activations;
    }

    private void connectLayers(NeuronInterface[] sourceLayer, NeuronInterface[] targetLayer, double connectivity) {
        connectivity = Math.max(0.0, Math.min(1.0, connectivity));
        List<Synapse> added = new ArrayList<>();
        for (NeuronInterface source : sourceLayer) {
            for (NeuronInterface target : targetLayer) {
                if (connectivity >= 1.0 || NeuralNetwork.getRandom().nextDouble() < connectivity) {
                    double limit = Math.sqrt(6.0 / (this.getInputLayerSize() + this.getOutputLayerSize()));
                    final double weight = NeuralNetwork.getRandom().nextDouble() * 2.0D * limit - limit;
                    added.add(new Synapse(source,
                            source.getNeuronTypeInfoData().getOutputCount() == 1 ? 0 : NeuralNetwork.getRandom().nextInt(source.getNeuronTypeInfoData().getOutputCount()),
                            target,
                            target.getNeuronTypeInfoData().getInputCount() == 1 ? 0 : NeuralNetwork.getRandom().nextInt(target.getNeuronTypeInfoData().getInputCount()),
                            weight));
                }
            }
        }
        if (!added.isEmpty()) {
            int start = synapseArray.length;
            synapseArray = Arrays.copyOf(synapseArray, start + added.size());
            for (int i = 0; i < added.size(); i++) synapseArray[start+i] = added.get(i);
        }
    }

    private void addSynapse(Synapse synapse) {
        Synapse[] newArr = new Synapse[this.synapseArray.length + 1];
        System.arraycopy(this.synapseArray, 0, newArr, 0, this.synapseArray.length);
        newArr[this.synapseArray.length] = synapse;
        this.synapseArray = newArr;
    }

    /**
     * Calculates the neuron's output value based on its inputs.
     * Optimized version using array iteration instead of ArrayList.
     */
    private long activate(final NeuronValueFunction neuronValueFunction, final NeuronInterface neuron) {
        if (forwardOrder != null) {
            for (int port = 0; port < neuron.getNeuronTypeInfoData().getInputCount(); port++)
                for (Synapse edge : neuron.getInputSynapseArr(port))
                    forwardSources.put(edge, neuronValueFunction.readValue(this, edge.getSourceNeuron(), edge.getSourceOutputTypePos()));
            forwardOrder.put(neuron, forwardOrder.size() + 1);
        }
        return neuron.activate(neuronValueFunction, this);
    }

    // Expose the number of synapses for energy cost calculation
    public int getSynapseCount() {
        return this.synapseArray.length;
    }

    /** Unique stored genes, including shared module definitions and unused type candidates. */
    public int getStoredSynapseCount() {
        if (storageVisited == null) storageVisited = Collections.newSetFromMap(new IdentityHashMap<>());
        storageVisited.clear();
        return countStoredSynapses(storageVisited);
    }

    private transient IdentityHashMap<NeuralNet,Long> maximumWorkMemo;
    /** All possible active layers, counting each module invocation, including shared definitions. */
    public long getMaximumProcessedSynapses() {
        if(maximumWorkMemo==null)maximumWorkMemo=new IdentityHashMap<>();
        maximumWorkMemo.clear();
        return maximumWork(maximumWorkMemo);
    }
    private long maximumWork(IdentityHashMap<NeuralNet,Long> memo) {
        Long known=memo.get(this);
        if(known!=null) {
            if(known<0)throw new IllegalStateException("Cyclic neural module definition");
            return known;
        }
        memo.put(this,-1L);long work=0;
        for(Layer layer:hiddenLayerArr)for(NeuronInterface neuron:layer.getNeuronsArr())work=addWork(work,neuronWork(neuron,memo));
        for(NeuronInterface neuron:outputNeuronArr)work=addWork(work,neuronWork(neuron,memo));
        memo.put(this,work);return work;
    }
    private static long neuronWork(NeuronInterface neuron,IdentityHashMap<NeuralNet,Long> memo) {
        long work=0;
        for(int port=0;port<neuron.getNeuronTypeInfoData().getInputCount();port++)work=addWork(work,neuron.getInputSynapseArr(port).length);
        NeuralNet module=neuron.getNeuronTypeInfoData().getNeuralNet();
        return module==null?work:addWork(work,module.maximumWork(memo));
    }
    private static long addWork(long a,long b) { return a>Long.MAX_VALUE-b?Long.MAX_VALUE:a+b; }

    private int countStoredSynapses(Set<NeuralNet> visited) {
        if (!visited.add(this)) return 0;
        int count = synapseArray.length;
        for (NeuronTypeInfoData type : neuronTypeInfoDataList)
            if (type.getNeuralNet() != null) count += type.getNeuralNet().countStoredSynapses(visited);
        // Inputs/outputs are scalar; live modules reside in hidden layers.
        for (Layer layer : hiddenLayerArr) for (NeuronInterface neuron : layer.getNeuronsArr()) {
            NeuralNet definition = neuron.getNeuronTypeInfoData().getNeuralNet();
            if (definition != null) count += definition.countStoredSynapses(visited);
        }
        return count;
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
        int count = this.inputNeuronArr.length + this.outputNeuronArr.length;

        for (Layer layer : this.hiddenLayerArr) {
            count += layer.size();
        }

        return count;
    }

    // Deserialisierung: Output-Synapsen wiederherstellen
    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        executionOptimizationEnabled = true;
        // Nach der Wiederherstellung der Synapsen: Input-Synapsen-Array und Zähler für alle Neuronen korrekt setzen
        // Alle Neuronen einsammeln
        List<NeuronInterface> allNeurons = new ArrayList<>();
        Collections.addAll(allNeurons, inputNeuronArr);
        for (Layer layer : hiddenLayerArr) {
            Collections.addAll(allNeurons, layer.getNeuronsArr());
        }
        Collections.addAll(allNeurons, outputNeuronArr);
        // Serialized neuron lists may already contain these edges. Rebuild once
        // from the canonical graph instead of appending duplicate connections.
        for (NeuronInterface neuron : allNeurons) {
            for (int port = 0; port < neuron.getNeuronTypeInfoData().getOutputCount(); port++) {
                for (Synapse synapse : new ArrayList<>(neuron.getOutputSynapseList(port))) {
                    neuron.removeOutputSynapse(port, synapse);
                }
            }
        }
        for (Synapse synapse : this.synapseArray) {
            synapse.getSourceNeuron().addOutputSynapse(synapse.getSourceOutputTypePos(), synapse);
        }
        // Für jedes Neuron: Input-Synapsen-Array neu aufbauen
        for (final NeuronInterface neuron : allNeurons) {
            for (int inputTypePos = 0; inputTypePos < neuron.getNeuronTypeInfoData().getInputCount(); inputTypePos++) {
                final List<Synapse> inputSynapseList = new ArrayList<>();
                for (final Synapse synapse : this.synapseArray) {
                    if (synapse.getTargetNeuron() == neuron && synapse.getTargetInputTypePos() == inputTypePos) {
                        inputSynapseList.add(synapse);
                    }
                }
                neuron.setInputSynapseArr(inputTypePos, inputSynapseList.toArray(new Synapse[0]));
            }
        }
    }

    public void setEnableNeuronType(final boolean enableNeuronType) {
        this.enableNeuronType = enableNeuronType;
    }
}
