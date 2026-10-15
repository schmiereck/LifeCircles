package de.lifecircles.model.neural;

import de.lifecircles.model.*;
import de.lifecircles.service.*;
import org.junit.jupiter.api.*;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class InheritanceRegressionTest {
    private final NeuronValueFunctionFactory factory = new ValuesNeuronValueFunctionFactory();
    private Random previousRandom;
    private double mutationRate, mutationStrength;

    @BeforeEach void setup() {
        previousRandom = NeuralNetwork.getRandom();
        NeuralNetwork.setRandom(new Random(785));
        mutationRate = SimulationConfig.getInstance().getMutationRate();
        mutationStrength = SimulationConfig.getInstance().getMutationStrength();
    }

    @AfterEach void restore() {
        NeuralNetwork.setRandom(previousRandom);
        SimulationConfig.getInstance().setMutationRate(mutationRate);
        SimulationConfig.getInstance().setMutationStrength(mutationStrength);
    }

    @Test void modulePortsKeepValuesDeltasAndInputSumsSeparate() {
        NeuronValueFunction values = factory.create();
        NeuronInterface module = module(0);
        for (int port = 0; port < 2; port++) {
            values.writeValue(null, module, port, 0.2 + port);
            values.writeDelta(null, module, port, 0.3 + port);
            values.writeInputSum(null, module, port, 0.4 + port);
        }
        for (int port = 0; port < 2; port++) {
            assertEquals(0.2 + port, values.readValue(null, module, port), 0);
            assertEquals(0.3 + port, values.readDelta(null, module, port), 0);
            assertEquals(0.4 + port, values.readInputSum(null, module, port), 0);
        }
    }

    private NeuronNetwork module(int id) {
        NeuralNetwork inner = new NeuralNetwork(factory, 1, new int[]{}, 2, 0, 0);
        inner.getOutputNeuronArr()[0].setBias(0, -2);
        inner.getOutputNeuronArr()[1].setBias(0, 2);
        return new NeuronNetwork(id, new NeuronTypeInfoData(1, 2, inner.getNeuralNet()), factory);
    }

    @Test void moduleProducesDistinctOutputsAndScalarSynapseReadsSelectedPort() {
        NeuralNetwork outer = new NeuralNetwork(factory, 1, new int[]{}, 1, 0, 0);
        NeuronValueFunction values = outer.getNeuronValueFunction();
        NeuronNetwork module = module(values.fetchNextFreeId(outer.getNeuralNet()));
        module.activate(values, outer.getNeuralNet());
        assertEquals(ActivationFunction.Sigmoid.apply(-2), values.readValue(outer.getNeuralNet(), module, 0), 1e-12);
        assertEquals(ActivationFunction.Sigmoid.apply(2), values.readValue(outer.getNeuralNet(), module, 1), 1e-12);
        Neuron output = (Neuron) outer.getOutputNeuronArr()[0];
        output.setBias(0, 0);
        new Synapse(module, 1, output, 0, 1);
        output.activate(values, outer.getNeuralNet());
        assertEquals(ActivationFunction.Sigmoid.apply(ActivationFunction.Sigmoid.apply(2)),
                values.readValue(outer.getNeuralNet(), output, 0), 1e-12);
    }

    @Test void copiedModulesShareChildTypeButNeverParentType() {
        NeuralNetwork parent = new NeuralNetwork(factory, 1, new int[]{0}, 1, 0, 0);
        NeuronNetwork first = module(parent.getNeuronValueFunction().fetchNextFreeId(parent.getNeuralNet()));
        NeuronNetwork second = new NeuronNetwork(parent.getNeuronValueFunction().fetchNextFreeId(parent.getNeuralNet()),
                first.getNeuronTypeInfoData(), factory);
        parent.getHiddenLayerArr()[0].addNeuron(first);
        parent.getHiddenLayerArr()[0].addNeuron(second);
        parent.writeNeuronValue(first, 0, 0.7);
        parent.writeNeuronValue(first, 1, 0.9);

        NeuralNetwork child = parent.mutate(0, 0);
        NeuronInterface childFirst = child.getHiddenLayerArr()[0].getNeuron(0);
        NeuronInterface childSecond = child.getHiddenLayerArr()[0].getNeuron(1);
        assertSame(childFirst.getNeuronTypeInfoData(), childSecond.getNeuronTypeInfoData());
        assertNotSame(first.getNeuronTypeInfoData(), childFirst.getNeuronTypeInfoData());
        assertNotSame(first.getNeuronTypeInfoData().getNeuralNet(), childFirst.getNeuronTypeInfoData().getNeuralNet());
        childFirst.getNeuronTypeInfoData().getNeuralNet().getOutputNeuronArr()[0].setBias(0, 99);
        assertEquals(-2, first.getNeuronTypeInfoData().getNeuralNet().getOutputNeuronArr()[0].getBias(0), 0);
        assertEquals(0, child.readNeuronValue(childFirst, 0), 0);
        assertEquals(0, child.readNeuronValue(childFirst, 1), 0);
        assertEquals(0.7, parent.readNeuronValue(first, 0), 0);
        assertEquals(0.9, parent.readNeuronValue(first, 1), 0);
    }

    @Test void zeroMutationPreservesBehaviorAndSynapsesAreIndependent() {
        NeuralNetwork parent = new NeuralNetwork(factory, 3, new int[]{4, 3}, 2, 1, 0);
        parent.setDisableLayerDeactivation(true);
        double[] input = {0.2, 0.5, 0.9};
        double[] expected = parent.calcProcess(input);
        NeuralNetwork child = parent.mutate(0, 0);
        assertArrayEquals(expected, child.calcProcess(input), 1e-12);
        assertEquals(parent.getSynapseCount(), child.getSynapseCount());
        double parentWeight = parent.getSynapseList().getFirst().getWeight();
        child.getSynapseList().getFirst().setWeight(100);
        assertEquals(parentWeight, parent.getSynapseList().getFirst().getWeight(), 0);
        assertArrayEquals(expected, parent.calcProcess(input), 1e-12);
    }

    @Test void modulesCreatedByStructuralMutationUseIndependentInheritedTypes() throws Exception {
        NeuralNetwork parent = new NeuralNetwork(factory, 1, new int[]{0}, 1, 0, 0);
        NeuralNet parentNet = parent.getNeuralNet();
        parentNet.mutateNeuronTypeInfoDataList(factory, parent.getNeuronValueFunction(), new Random(27), 0.1, 0.01);
        Random chooseModule = new Random(9) {
            @Override public int nextInt(int bound) { return bound - 1; }
        };
        parentNet.addNeuronToHiddenLayer(factory, parent.getNeuronValueFunction(), chooseModule, 0, 1);
        parentNet.addNeuronToHiddenLayer(factory, parent.getNeuronValueFunction(), chooseModule, 0, 1);
        NeuronInterface original = parentNet.getHiddenLayerArr()[0].getNeuron(0);
        assertInstanceOf(NeuronNetwork.class, original);
        NeuralNetwork child = parent.mutate(0, 0);
        NeuronInterface inherited = child.getHiddenLayerArr()[0].getNeuron(0);
        assertSame(inherited.getNeuronTypeInfoData(), child.getHiddenLayerArr()[0].getNeuron(1).getNeuronTypeInfoData());
        assertNotSame(original.getNeuronTypeInfoData().getNeuralNet(), inherited.getNeuronTypeInfoData().getNeuralNet());
        parent.setDisableLayerDeactivation(true); child.setDisableLayerDeactivation(true);
        assertArrayEquals(parent.calcProcess(new double[]{0.6}), child.calcProcess(new double[]{0.6}), 1e-12);
        NeuralNet parentType = original.getNeuronTypeInfoData().getNeuralNet();
        double bias = parentType.getOutputNeuronArr()[0].getBias(0);
        inherited.getNeuronTypeInfoData().getNeuralNet().getOutputNeuronArr()[0].setBias(0, bias + 1);
        assertEquals(bias, parentType.getOutputNeuronArr()[0].getBias(0), 0);
        parentType.setDisableLayerDeactivation(true);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(parent); }
        NeuralNetwork restored;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (NeuralNetwork) in.readObject();
        }
        restored.setDisableLayerDeactivation(true);
        restored.getHiddenLayerArr()[0].getNeuron(0).getNeuronTypeInfoData().getNeuralNet()
                .setDisableLayerDeactivation(true);
        assertArrayEquals(parent.calcProcess(new double[]{0.6}), restored.calcProcess(new double[]{0.6}), 1e-12);
        for (Synapse synapse : restored.getSynapseList()) {
            assertEquals(1, Collections.frequency(synapse.getSourceNeuron().getOutputSynapseList(
                    synapse.getSourceOutputTypePos()), synapse));
            assertTrue(Arrays.asList(synapse.getTargetNeuron().getInputSynapseArr(
                    synapse.getTargetInputTypePos())).contains(synapse));
        }
    }

    @Test void multipleMutatedGenerationsNeverChangeTheirParents() {
        NeuralNetwork parent = new NeuralNetwork(factory, 2, new int[]{3, 3}, 2, 1, 0);
        for (int generation = 0; generation < 20; generation++) {
            parent.setDisableLayerDeactivation(true);
            parent.rnnClearPreviousState();
            double[] before = parent.calcProcess(new double[]{0.2, 0.8});
            double[] weights = parent.getSynapseList().stream().mapToDouble(Synapse::getWeight).toArray();
            NeuralNetwork child = parent.mutate(0.3, 0.2);
            assertArrayEquals(weights, parent.getSynapseList().stream().mapToDouble(Synapse::getWeight).toArray(), 0);
            parent.rnnClearPreviousState();
            assertArrayEquals(before, parent.calcProcess(new double[]{0.2, 0.8}), 1e-12);
            parent = child;
        }
    }

    @Test void valuesAndMultiplePortsSurviveSerialization() throws Exception {
        ValuesNeuronValueFunction original = new ValuesNeuronValueFunction();
        NeuronInterface module = module(5);
        original.writeValue(null, module, 0, 0.25);
        original.writeValue(null, module, 1, 0.75);
        original.writeDelta(null, module, 1, 12);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(original); }
        ValuesNeuronValueFunction restored;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (ValuesNeuronValueFunction) in.readObject();
        }
        assertEquals(0.25, restored.readValue(null, module, 0), 0);
        assertEquals(0.75, restored.readValue(null, module, 1), 0);
        assertEquals(0, restored.readDelta(null, module, 1), 0);
    }

    @Test void reusedNeuronIdDoesNotInheritOldPortValues() {
        NeuralNetwork network = new NeuralNetwork(factory, 1, new int[]{}, 1, 0, 0);
        NeuronValueFunction values = network.getNeuronValueFunction();
        int id = values.fetchNextFreeId(network.getNeuralNet());
        NeuronInterface removed = module(id);
        values.writeValue(network.getNeuralNet(), removed, 1, 12);
        values.writeDelta(network.getNeuralNet(), removed, 1, 13);
        values.writeInputSum(network.getNeuralNet(), removed, 1, 14);
        values.releaseNeuron(network.getNeuralNet(), removed);
        assertEquals(id, values.fetchNextFreeId(network.getNeuralNet()));
        assertEquals(0, values.readValue(network.getNeuralNet(), removed, 1), 0);
        assertEquals(0, values.readDelta(network.getNeuralNet(), removed, 1), 0);
        assertEquals(0, values.readInputSum(network.getNeuralNet(), removed, 1), 0);
    }

    @Test void disabledCellStateLayerDoesNotActivateItsFirstNeuron() {
        NeuralNetwork network = new NeuralNetwork(factory, 1, new int[]{2}, 1, 1, 1);
        Layer layer = network.getHiddenLayerArr()[0];
        network.writeNeuronValue(layer.getNeuron(0), 0, 0.7);
        network.writeNeuronValue(layer.getNeuron(1), 0, 0.8);
        layer.setActiveLayer(false);
        network.calcProcess(new double[]{1});
        assertEquals(0, network.readNeuronValue(layer.getNeuron(0), 0), 0);
        assertEquals(0, network.readNeuronValue(layer.getNeuron(1), 0), 0);
    }

    @Test void reproductionUsesChildStateBeforeFirstThinkAndConservesEnergy() {
        SimulationConfig config = SimulationConfig.getInstance();
        config.setMutationRate(0); config.setMutationStrength(0);
        Environment world = new Environment(300, 300);
        Cell parent = CellFactory.createCell(new Vector2D(150, 150), 10, 0.01, 0, 0);
        parent.setEnergy(0.8); parent.setGeneration(4); parent.setRotation(0.4);
        parent.setAngularVelocity(-0.8); parent.setVelocity(new Vector2D(2, 3));
        SensorActor actor = parent.getSensorActors().getFirst();
        actor.updateCachedPosition(); actor.setReproductionDesire(1);
        actor.setReproductionEnergyShareOutput(0.25); actor.setReproductionState(7);
        Cell child = ReproductionManagerService.reproduce(config, world, parent);
        assertNotNull(child);
        assertEquals(7, child.getCellState()); assertEquals(5, child.getGeneration());
        assertEquals(1, child.getBrain().getInputValue(GlobalInputFeature.MY_CELL_STATE_0.ordinal()), 0);
        assertEquals(1, child.getBrain().getInputValue(GlobalInputFeature.MY_CELL_STATE_1.ordinal()), 0);
        assertEquals(1, child.getBrain().getInputValue(GlobalInputFeature.MY_CELL_STATE_2.ordinal()), 0);
        assertEquals(0.8, parent.getEnergy() + child.getEnergy(), 1e-12);
        assertEquals(0.2, child.getEnergy(), 1e-12);
        assertEquals(parent.getAngularVelocity(), child.getAngularVelocity(), 0);
        assertEquals(2, child.getVelocity().getX(), 0);
        assertTrue(child.isGrowing());
    }

    @Test void reproductionWrapsChildIntoWorld() {
        SimulationConfig config = SimulationConfig.getInstance();
        config.setMutationRate(0);
        Environment world = new Environment(300, 300);
        Cell parent = CellFactory.createCell(new Vector2D(299, 150), 10, 0.01, 0, 0);
        SensorActor actor = parent.getSensorActors().getFirst();
        actor.updateCachedPosition(); actor.setReproductionDesire(1); actor.setReproductionEnergyShareOutput(0.25);
        Cell child = ReproductionManagerService.reproduce(config, world, parent);
        assertNotNull(child);
        assertEquals(10, child.getPosition().getX(), 1e-12);
    }

    @Test void reproductionAppliesInheritedMutationFactors() {
        SimulationConfig config = SimulationConfig.getInstance();
        config.setMutationRate(0.1); config.setMutationStrength(0.2);
        class RecordingBrain extends CellBrain {
            double rate, strength;
            RecordingBrain() {
                super(new NeuralNetwork(factory, CellFactory.calcInputCount(12), new int[]{1, 1, 1},
                        CellFactory.calcOutputCount(12), 0, 3));
            }
            @Override public NeuralNetwork mutate(double rate, double strength) {
                this.rate = rate; this.strength = strength;
                return getNeuralNetwork().mutate(0, 0);
            }
        }
        RecordingBrain brain = new RecordingBrain();
        Cell parent = new Cell(new Vector2D(100, 100), 10, brain);
        parent.setMutationRateFactor(1.5); parent.setMutationStrengthFactor(0.5);
        SensorActor actor = parent.getSensorActors().getFirst();
        actor.updateCachedPosition(); actor.setReproductionDesire(1); actor.setReproductionEnergyShareOutput(0.25);
        assertNotNull(ReproductionManagerService.reproduce(config, new Environment(300, 300), parent));
        assertEquals(0.15, brain.rate, 1e-12); assertEquals(0.1, brain.strength, 1e-12);
    }
}
