package de.lifecircles.model.neural;

import org.junit.jupiter.api.*;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class NestedInheritanceTest {
    private final NeuronValueFunctionFactory factory = new ValuesNeuronValueFunctionFactory();
    private Random previousRandom;
    @BeforeEach void setup() { previousRandom = NeuralNetwork.getRandom(); NeuralNetwork.setRandom(new Random(419)); }
    @AfterEach void restore() { NeuralNetwork.setRandom(previousRandom); }

    private NeuralNetwork scalarNet(int hidden, int hiddenLayers) {
        int[] sizes = new int[hiddenLayers]; Arrays.fill(sizes, hidden);
        return new NeuralNetwork(factory, 1, sizes, 1, 1, 0);
    }

    private NeuronNetwork replace(NeuralNetwork net, int position, NeuronTypeInfoData type) {
        NeuronInterface old = net.getHiddenLayerArr()[0].getNeuron(position);
        NeuronNetwork module = new NeuronNetwork(old.getId(), type, factory);
        net.getHiddenLayerArr()[0].getNeuronsArr()[position] = module;
        Synapse[] edges = net.getNeuralNet().getSynapseArr();
        for (int i = 0; i < edges.length; i++) {
            Synapse edge = edges[i];
            if (edge.getSourceNeuron() != old && edge.getTargetNeuron() != old) continue;
            edge.getSourceNeuron().removeOutputSynapse(edge.getSourceOutputTypePos(), edge);
            edge.getTargetNeuron().removeInputSynapse(edge.getTargetInputTypePos(), edge);
            edges[i] = new Synapse(edge.getSourceNeuron() == old ? module : edge.getSourceNeuron(), 0,
                    edge.getTargetNeuron() == old ? module : edge.getTargetNeuron(), 0, edge.getWeight());
        }
        return module;
    }

    private NeuralNetwork nested(int depth) {
        NeuralNetwork net = scalarNet(2, 1);
        if (depth > 0) {
            NeuralNetwork inner = nested(depth - 1);
            NeuronTypeInfoData type = new NeuronTypeInfoData(1, 1, inner.getNeuralNet());
            replace(net, 0, type); replace(net, 1, type);
        }
        return net;
    }

    private Set<Object> genotypeObjects(NeuralNet net) {
        Set<Object> found = Collections.newSetFromMap(new IdentityHashMap<>());
        collect(net, found); return found;
    }

    private void collect(NeuralNet net, Set<Object> found) {
        if (!found.add(net)) return;
        found.add(net.getInputNeuronArr()); found.add(net.getOutputNeuronArr()); found.add(net.getHiddenLayerArr());
        found.add(net.getSynapseArr()); found.addAll(net.getSynapseList());
        List<NeuronInterface> neurons = new ArrayList<>(Arrays.asList(net.getInputNeuronArr()));
        neurons.addAll(Arrays.asList(net.getOutputNeuronArr()));
        for (Layer layer : net.getHiddenLayerArr()) {
            found.add(layer); found.add(layer.getNeuronsArr()); neurons.addAll(layer.getNeuronList());
        }
        for (NeuronInterface neuron : neurons) {
            found.add(neuron); found.add(neuron.getNeuronTypeInfoData());
            NeuralNet inner = neuron.getNeuronTypeInfoData().getNeuralNet();
            if (inner != null) collect(inner, found);
        }
    }

    private void freeze(NeuralNet net, Set<NeuralNet> visited) {
        if (!visited.add(net)) return;
        net.setDisableLayerDeactivation(true);
        for (Layer layer : net.getHiddenLayerArr()) for (NeuronInterface neuron : layer.getNeuronsArr()) {
            NeuralNet inner = neuron.getNeuronTypeInfoData().getNeuralNet();
            if (inner != null) freeze(inner, visited);
        }
    }

    private void freeze(NeuralNetwork net) { freeze(net.getNeuralNet(), Collections.newSetFromMap(new IdentityHashMap<>())); }

    private record Parameter(java.util.function.DoubleSupplier read, java.util.function.DoubleConsumer write) {}

    private List<Parameter> parameters(NeuralNet net) {
        List<Parameter> parameters = new ArrayList<>();
        Set<Object> objects = genotypeObjects(net);
        Set<NeuronInterface> inputs = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Object gene : objects) if (gene instanceof NeuralNet graph) inputs.addAll(Arrays.asList(graph.getInputNeuronArr()));
        for (Object gene : objects) {
            if (gene instanceof Synapse edge) parameters.add(new Parameter(edge::getWeight, edge::setWeight));
            if (gene instanceof NeuronInterface neuron && !inputs.contains(neuron)) {
                for (int port = 0; port < neuron.getNeuronTypeInfoData().getInputCount(); port++) {
                    final int index = port;
                    parameters.add(new Parameter(() -> neuron.getBias(index), value -> neuron.setBias(index, value)));
                }
            }
        }
        return parameters;
    }

    @Test void threeNestedLevelsShareNoGenotypeObjectsWithParentOrSibling() {
        NeuralNetwork parent = nested(3), child = parent.mutate(0, 0), sibling = parent.mutate(0, 0);
        Set<Object> parentObjects = genotypeObjects(parent.getNeuralNet());
        Set<Object> childObjects = genotypeObjects(child.getNeuralNet());
        Set<Object> siblingObjects = genotypeObjects(sibling.getNeuralNet());
        assertTrue(Collections.disjoint(parentObjects, childObjects), "Child still references parental genotype objects");
        assertTrue(Collections.disjoint(parentObjects, siblingObjects));
        assertTrue(Collections.disjoint(childObjects, siblingObjects));
    }

    @Test void oneInheritedTypeRemainsSharedAcrossDifferentNestingLevels() {
        NeuralNetwork leaf = scalarNet(2, 1);
        NeuronTypeInfoData leafType = new NeuronTypeInfoData(1, 1, leaf.getNeuralNet());
        NeuralNetwork branch = scalarNet(1, 1); replace(branch, 0, leafType);
        NeuralNetwork parent = scalarNet(2, 1);
        replace(parent, 0, new NeuronTypeInfoData(1, 1, branch.getNeuralNet()));
        replace(parent, 1, leafType);
        NeuralNetwork child = parent.mutate(0, 0);
        NeuronTypeInfoData direct = child.getHiddenLayerArr()[0].getNeuron(1).getNeuronTypeInfoData();
        NeuronTypeInfoData nested = child.getHiddenLayerArr()[0].getNeuron(0).getNeuronTypeInfoData()
                .getNeuralNet().getHiddenLayerArr()[0].getNeuron(0).getNeuronTypeInfoData();
        assertSame(direct, nested, "One parental type should map to one child type throughout the graph");
        assertNotSame(leafType, direct);
    }

    @Test void sharedNestedTypesHaveIndependentExecutionState() {
        NeuralNetwork leaf = scalarNet(2, 1);
        NeuronInterface control = leaf.getHiddenLayerArr()[0].getNeuron(0);
        control.setBias(0, Math.log(0.4 / 0.6));
        for (Synapse edge : control.getInputSynapseArr(0)) edge.setWeight(0);
        NeuralNetwork branch = scalarNet(1, 1);
        replace(branch, 0, new NeuronTypeInfoData(1, 1, leaf.getNeuralNet()));
        NeuralNetwork shared = scalarNet(2, 1);
        NeuronTypeInfoData type = new NeuronTypeInfoData(1, 1, branch.getNeuralNet());
        replace(shared, 0, type).setBias(0, -2);
        replace(shared, 1, type).setBias(0, 2);
        shared.setDisableLayerDeactivation(true);
        NeuralNetwork separate = new NeuralNetwork(shared);
        NeuronInterface second = separate.getHiddenLayerArr()[0].getNeuron(1);
        NeuronTypeInfoData independent = new NeuronTypeInfoData(1, 1,
                new NeuralNet(second.getNeuronTypeInfoData().getNeuralNet(), true, factory, factory.create()));
        NeuronNetwork replacement = replace(separate, 1, independent);
        replacement.setBias(0, second.getBias(0));
        for (double input : new double[]{0.2, 0.8, -0.3, 0.4, -0.7, 0.1}) {
            assertArrayEquals(separate.calcProcess(new double[]{input}), shared.calcProcess(new double[]{input}), 1e-12,
                    "Sharing a type must not couple the state of its instances");
        }
    }

    @Test void sharedNestedTypesLearnWithCorrectIndependentGradients() {
        NeuralNetwork net = nested(2); freeze(net);
        List<Parameter> parameters = parameters(net.getNeuralNet());
        double[] original = new double[parameters.size()], derivative = new double[parameters.size()];
        double epsilon = 1e-6, rate = 0.001;
        for (int i = 0; i < parameters.size(); i++) {
            Parameter parameter = parameters.get(i);
            original[i] = parameter.read.getAsDouble();
            parameter.write.accept(original[i] + epsilon);
            double plus = 0.5 * Math.pow(net.calcProcess(new double[]{0.4})[0] - 0.7, 2);
            parameter.write.accept(original[i] - epsilon);
            double minus = 0.5 * Math.pow(net.calcProcess(new double[]{0.4})[0] - 0.7, 2);
            parameter.write.accept(original[i]);
            derivative[i] = (plus - minus) / (2 * epsilon);
        }
        net.setLearningRate(rate); net.calcProcess(new double[]{0.4}); net.backpropagate(new double[]{0.7});
        for (int i = 0; i < parameters.size(); i++) {
            assertEquals(derivative[i], (original[i] - parameters.get(i).read.getAsDouble()) / rate, 1e-8,
                    "Shared nested gradient " + i);
        }
    }

    @Test void childLearningAndDeepMutationLeaveParentAndSiblingUnchanged() {
        NeuralNetwork parent = nested(3), child = parent.mutate(0, 0), sibling = parent.mutate(0, 0);
        freeze(parent); freeze(child); freeze(sibling);
        double[] input = {0.6};
        double[] parentOutput = parent.calcProcess(input), siblingOutput = sibling.calcProcess(input);
        List<Parameter> parentGenes = parameters(parent.getNeuralNet()), siblingGenes = parameters(sibling.getNeuralNet());
        double[] parentBefore = parentGenes.stream().mapToDouble(p -> p.read.getAsDouble()).toArray();
        double[] siblingBefore = siblingGenes.stream().mapToDouble(p -> p.read.getAsDouble()).toArray();
        NeuralNet deepest = child.getNeuralNet();
        for (int level = 0; level < 3; level++) deepest = deepest.getHiddenLayerArr()[0].getNeuron(0).getNeuronTypeInfoData().getNeuralNet();
        deepest.getSynapseList().getFirst().setWeight(17);
        child.calcProcess(input); child.backpropagate(new double[]{0.8});
        assertArrayEquals(parentBefore, parentGenes.stream().mapToDouble(p -> p.read.getAsDouble()).toArray(), 0);
        assertArrayEquals(siblingBefore, siblingGenes.stream().mapToDouble(p -> p.read.getAsDouble()).toArray(), 0);
        assertArrayEquals(parentOutput, parent.calcProcess(input), 0);
        assertArrayEquals(siblingOutput, sibling.calcProcess(input), 0);
    }

    @Test void aggressiveMutationNeverAllocatesNeuronIdsInParentStore() {
        NeuralNetwork parent = nested(2);
        int nextId = parent.getNeuronValueFunction().fetchNextFreeId(parent.getNeuralNet());
        NeuralNetwork.setRandom(new Random(1));
        NeuralNetwork child = parent.mutateAggressively(parent.getNeuronValueFunction(), 0, 0.2);
        assertEquals(nextId + 1, parent.getNeuronValueFunction().fetchNextFreeId(parent.getNeuralNet()));
        assertTrue(Collections.disjoint(genotypeObjects(parent.getNeuralNet()), genotypeObjects(child.getNeuralNet())));
    }

    @Test void executionViewsShareGenesButKeepTheirOwnValuesAndLayerCounters() {
        NeuralNetwork parent = nested(2);
        parent.setLearningRate(0.2);
        parent.getHiddenLayerArr()[0].setActivationCounter(0.75);
        NeuralNetwork view = new NeuralNetwork(parent, false);
        assertSame(parent.getNeuralNet().getSynapseArr(), view.getNeuralNet().getSynapseArr());
        assertNotSame(parent.getHiddenLayerArr()[0], view.getHiddenLayerArr()[0]);
        assertEquals(0, view.getHiddenLayerArr()[0].getActivationCounter(), 0);
        view.getHiddenLayerArr()[0].setActivationCounter(0.25);
        assertEquals(0.75, parent.getHiddenLayerArr()[0].getActivationCounter(), 0);
        assertEquals(0.2, new NeuralNetwork(parent).getLearningRate(), 0);
        int maximumId = Arrays.stream(parent.getInputNeuronArr()).mapToInt(NeuronInterface::getId).max().orElse(-1);
        maximumId = Math.max(maximumId, Arrays.stream(parent.getOutputNeuronArr()).mapToInt(NeuronInterface::getId).max().orElse(-1));
        for (Layer layer : parent.getHiddenLayerArr()) for (NeuronInterface neuron : layer.getNeuronsArr()) maximumId = Math.max(maximumId, neuron.getId());
        assertEquals(maximumId + 1, view.getNeuronValueFunction().fetchNextFreeId(view.getNeuralNet()));
        NeuronNetwork module = (NeuronNetwork) parent.getHiddenLayerArr()[0].getNeuron(0);
        assertNotSame(parent.getNeuronValueFunction().getModuleState(module), view.getNeuronValueFunction().getModuleState(module));
    }

    @Test void structuralTypeChangesRefreshOnlyTheAffectedChildExecution() {
        NeuralNetwork parent = nested(1), child = parent.mutate(0, 0);
        NeuronNetwork parentModule = (NeuronNetwork) parent.getHiddenLayerArr()[0].getNeuron(0);
        NeuronNetwork childModule = (NeuronNetwork) child.getHiddenLayerArr()[0].getNeuron(0);
        ModuleExecutionState parentState = parent.getNeuronValueFunction().getModuleState(parentModule);
        ModuleExecutionState before = child.getNeuronValueFunction().getModuleState(childModule);
        NeuralNet childType = childModule.getNeuronTypeInfoData().getNeuralNet();
        NeuronValueFunction mutationValues = factory.create(); mutationValues.reserveExistingIds(childType);
        childType.addHiddenLayer(factory, mutationValues, 0, 2, 1);
        child.calcProcess(new double[]{0.3});
        ModuleExecutionState after = child.getNeuronValueFunction().getModuleState(childModule);
        assertNotSame(before, after);
        assertEquals(childType.getHiddenLayerArr().length, after.network.getHiddenLayerArr().length);
        assertSame(parentState, parent.getNeuronValueFunction().getModuleState(parentModule));
        assertEquals(1, parentModule.getNeuronTypeInfoData().getNeuralNet().getHiddenLayerArr().length);
    }

    @Test void clearingMemoryResetsNestedExecutionWithoutChangingGenes() {
        NeuralNetwork net = nested(2); net.calcProcess(new double[]{0.3});
        NeuronNetwork module = (NeuronNetwork) net.getHiddenLayerArr()[0].getNeuron(0);
        ModuleExecutionState before = net.getNeuronValueFunction().getModuleState(module);
        Synapse gene = module.getNeuronTypeInfoData().getNeuralNet().getSynapseList().getFirst();
        double weight = gene.getWeight();
        net.rnnClearPreviousState();
        ModuleExecutionState after = net.getNeuronValueFunction().getModuleState(module);
        assertNotSame(before, after);
        assertEquals(0, after.network.getNeuronValueFunction().readValue(after.network.getNeuralNet(),
                after.network.getNeuralNet().getOutputNeuronArr()[0], 0), 0);
        assertEquals(weight, gene.getWeight(), 0);
    }

    @Test void recursiveTypeGraphsFailClearlyInsteadOfOverflowingTheStack() {
        NeuralNetwork net = scalarNet(1, 1);
        replace(net, 0, new NeuronTypeInfoData(1, 1, net.getNeuralNet()));
        assertThrows(IllegalArgumentException.class, () -> new NeuralNetwork(net));
    }

    private void freezeExecution(NeuralNetwork net) {
        net.setDisableLayerDeactivation(true);
        for (Layer layer : net.getHiddenLayerArr()) for (NeuronInterface neuron : layer.getNeuronsArr()) {
            if (neuron instanceof NeuronNetwork module) freezeExecution(net.getNeuronValueFunction().getModuleState(module).network);
        }
    }

    @Test void serializedNestedInstancesRemainIndependentAndCanContinueLearning() throws Exception {
        NeuralNetwork original = nested(2); freeze(original); freezeExecution(original);
        original.calcProcess(new double[]{0.4}); original.backpropagate(new double[]{0.7});
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(original); }
        NeuralNetwork restored;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (NeuralNetwork) in.readObject();
        }
        freeze(restored); freezeExecution(restored);
        assertTrue(Collections.disjoint(genotypeObjects(original.getNeuralNet()), genotypeObjects(restored.getNeuralNet())));
        NeuronNetwork a = (NeuronNetwork) restored.getHiddenLayerArr()[0].getNeuron(0);
        NeuronNetwork b = (NeuronNetwork) restored.getHiddenLayerArr()[0].getNeuron(1);
        assertSame(a.getNeuronTypeInfoData(), b.getNeuronTypeInfoData());
        assertNotSame(restored.getNeuronValueFunction().getModuleState(a), restored.getNeuronValueFunction().getModuleState(b));
        for (double input : new double[]{0.6, -0.2, 0.8}) {
            assertArrayEquals(original.calcProcess(new double[]{input}), restored.calcProcess(new double[]{input}), 1e-12);
            original.backpropagate(new double[]{0.3}); restored.backpropagate(new double[]{0.3});
        }
    }

    @Test void independentChildrenRemainRepeatableDuringParallelLearning() {
        NeuralNetwork parent = nested(2);
        List<NeuralNetwork> children = new ArrayList<>(), reference = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            NeuralNetwork child = parent.mutate(0, 0); freeze(child); children.add(child);
            NeuralNetwork control = parent.mutate(0, 0); freeze(control); reference.add(control);
        }
        for (int step = 0; step < 20; step++) {
            double input = step * 0.02, target = 0.6;
            children.parallelStream().forEach(child -> {
                child.calcProcess(new double[]{input}); child.backpropagate(new double[]{target});
            });
            for (NeuralNetwork control : reference) {
                control.calcProcess(new double[]{input}); control.backpropagate(new double[]{target});
            }
            for (int i = 0; i < children.size(); i++) {
                assertArrayEquals(reference.get(i).calcProcess(new double[]{input}),
                        children.get(i).calcProcess(new double[]{input}), 0);
            }
        }
    }
}
