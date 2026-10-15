package de.lifecircles.model.neural;

import de.lifecircles.model.*;
import de.lifecircles.service.ReproductionManagerService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in observations of CURRENT behavior, including defects. Rewrite expectations when fixing them. */
@EnabledIfSystemProperty(named = "network.audit", matches = "true")
class NetworkLogicAuditTest {
    private final NeuronValueFunctionFactory factory = new ValuesNeuronValueFunctionFactory();
    private Random previous;
    @BeforeEach void seed() { previous = NeuralNetwork.getRandom(); NeuralNetwork.setRandom(new Random(81)); }
    @AfterEach void restore() { NeuralNetwork.setRandom(previous); }

    private void connect(NeuralNetwork net, Synapse edge) {
        try {
            var method = NeuralNet.class.getDeclaredMethod("addSynapse", Synapse.class);
            method.setAccessible(true); method.invoke(net.getNeuralNet(), edge);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private NeuralNetwork heldNetwork() {
        NeuralNetwork net = new NeuralNetwork(factory, 1, new int[]{2}, 1, 0, 0);
        net.getNeuralNet().applyToAllNeurons(n -> n.setBias(0, 0));
        var hidden = net.getNeuralNet().getHiddenLayerArr()[0].getNeuronsArr();
        ((Neuron) hidden[1]).setActivationFunction(ActivationFunction.ReLU);
        connect(net, new Synapse(net.getNeuralNet().getInputNeuronArr()[0], 0, hidden[1], 0, 1));
        connect(net, new Synapse(hidden[1], 0, net.getNeuralNet().getOutputNeuronArr()[0], 0, 1));
        return net;
    }

    @Test void sleepingDynamicLayerHoldsPayloadButStillEvaluatesControllerAndOutput() {
        var net = heldNetwork();
        double first = net.calcProcess(new double[]{1})[0];
        assertEquals(2, net.getProccessedSynapses());
        assertFalse(net.getNeuralNet().getHiddenLayerArr()[0].isActiveLayer());
        assertEquals(first, net.calcProcess(new double[]{5})[0]);
        assertEquals(1, net.getProccessedSynapses());
        assertTrue(net.getNeuralNet().getHiddenLayerArr()[0].isActiveLayer());
        assertTrue(net.calcProcess(new double[]{5})[0] > first);
    }

    @Test void zeroControllerCanSleepForeverDespiteCounterFloor() {
        var net = heldNetwork();
        Neuron control = (Neuron) net.getNeuralNet().getHiddenLayerArr()[0].getNeuronsArr()[0];
        control.setActivationFunction(ActivationFunction.ReLU); control.setBias(0, -1);
        net.calcProcess(new double[]{1});
        for (int i = 0; i < 1500; i++) net.calcProcess(new double[]{5});
        var layer = net.getNeuralNet().getHiddenLayerArr()[0];
        assertFalse(layer.isActiveLayer()); assertEquals(.001, layer.getActivationCounter());
    }

    @Test void resetDoesNotResetSchedulingAndChangesRepeatedSequenceResult() {
        var net = heldNetwork(); double first = net.calcProcess(new double[]{1})[0];
        net.rnnClearPreviousState();
        assertFalse(net.getNeuralNet().getHiddenLayerArr()[0].isActiveLayer());
        assertNotEquals(first, net.calcProcess(new double[]{1})[0]);
        assertEquals(first, new NeuralNetwork(net).calcProcess(new double[]{1})[0]);
    }

    @Test void backpropSkipsJustExecutedLayerBecauseItsNextStepFlagIsOff() {
        var net = heldNetwork(); net.setLearningRate(.1);
        var inputWeight = net.getSynapseList().get(0);
        var numerical = new NeuralNetwork(net); numerical.setDisableLayerDeactivation(true);
        double epsilon = 1e-6;
        numerical.getSynapseList().get(0).setWeight(1 + epsilon);
        double high = Math.pow(numerical.calcProcess(new double[]{1})[0], 2) / 2;
        numerical.getSynapseList().get(0).setWeight(1 - epsilon);
        double low = Math.pow(numerical.calcProcess(new double[]{1})[0], 2) / 2;
        assertTrue((high - low) / (2 * epsilon) > .1);
        net.calcTrain(new double[]{1}, new double[]{0});
        assertEquals(1, inputWeight.getWeight());
        assertNotEquals(1, net.getSynapseList().get(1).getWeight());
    }

    @Test void backwardEdgeGradientUsesCurrentOutputAlthoughForwardUsedPreviousZero() {
        var net = new NeuralNetwork(factory, 1, new int[]{1}, 1, 0, 1);
        net.getNeuralNet().applyToAllNeurons(n -> n.setBias(0, 0)); net.setLearningRate(.1);
        NeuronInterface h = net.getHiddenLayerArr()[0].getNeuron(0);
        connect(net, new Synapse(net.getInputNeuronArr()[0], 0, h, 0, 1));
        connect(net, new Synapse(h, 0, net.getOutputNeuronArr()[0], 0, 1));
        Synapse feedback = new Synapse(net.getOutputNeuronArr()[0], 0, h, 0, .5); connect(net, feedback);
        var high = new NeuralNetwork(net); high.getSynapseList().get(2).setWeight(.500001);
        var low = new NeuralNetwork(net); low.getSynapseList().get(2).setWeight(.499999);
        // The source value was zero at the first forward pass, so this weight cannot affect its loss.
        assertEquals(high.calcProcess(new double[]{1})[0], low.calcProcess(new double[]{1})[0]);
        net.calcTrain(new double[]{1}, new double[]{0});
        assertNotEquals(.5, feedback.getWeight());
    }

    @Test void moduleBiasMutationIgnoresZeroMutationStrength() {
        var inner = new NeuralNetwork(factory, 1, new int[]{1}, 1, 1, 0);
        var module = new NeuronNetwork(10, new NeuronTypeInfoData(1, 1, inner.getNeuralNet()), factory);
        module.mutateNeuron(new Random(1) {
            @Override public double nextDouble() { return .75; }
            @Override public int nextInt(int bound) { return 0; }
        }, 1, 0);
        assertEquals(.025, module.getBias(0), 1e-15);
    }

    @Test void defaultMutationPerturbsManyWeightsEvenWithoutTopologyChange() {
        var cell = CellFactory.createCell(new Vector2D(100, 100), 10);
        var parent = ((CellBrain) cell.getBrain()).getNeuralNetwork();
        NeuralNetwork.setRandom(new Random(42));
        var child = parent.mutate(.1, .2);
        assertArrayEquals(parent.getLayerSizes(), child.getLayerSizes());
        assertEquals(parent.getSynapseCount(), child.getSynapseCount());
        int changed = 0;
        for (int i = 0; i < parent.getSynapseCount(); i++)
            if (parent.getSynapseList().get(i).getWeight() != child.getSynapseList().get(i).getWeight()) changed++;
        assertTrue(changed > 100);
        System.out.printf(Locale.ROOT, "Default genome: layers=%s neurons=%d synapses=%d mutated_weights=%d%n",
                Arrays.toString(parent.getLayerSizes()), parent.getNeuralNet().getAllNeuronsSize(), parent.getSynapseCount(), changed);
    }

    @Test void factoryStateZeroAndExplicitStateGateInitializationDisagree() {
        Cell cell = CellFactory.createCell(new Vector2D(100, 100), 10, .05, 0, 0);
        Layer[] layers = ((CellBrain) cell.getBrain()).getNeuralNetwork().getNeuralNet().getHiddenLayerArr();
        assertEquals(0, cell.getCellState());
        for (int i = 0; i < 3; i++) assertTrue(layers[i].isActiveLayer());
        ReproductionManagerService.calcActiveLayersByState(cell);
        for (int i = 0; i < 3; i++) assertFalse(layers[i].isActiveLayer());
        cell.setCellState(7);
        for (int i = 0; i < 3; i++) assertFalse(layers[i].isActiveLayer());
    }

    @Test void initialStateLayersAreSerialAndEarlierGatesCanBeCutOff() {
        CellBrain brain = new CellBrain(factory, 4, 1, .5, 1, 1);
        NeuralNetwork definition = brain.getNeuralNetwork();
        definition.getNeuralNet().applyToAllNeurons(n -> n.setBias(0, 0));
        for (Synapse edge : definition.getSynapseList()) edge.setWeight(1);
        double[] outputs = new double[8];
        for (int state = 0; state < 8; state++) {
            var net = new NeuralNetwork(definition);
            for (int bit = 0; bit < 3; bit++) net.getNeuralNet().getHiddenLayerArr()[bit].setActiveLayer((state & 1 << bit) != 0);
            outputs[state] = net.calcProcess(new double[]{1, 0, 0, 0})[0];
        }
        assertEquals(outputs[0], outputs[1]); assertEquals(outputs[0], outputs[2]); assertEquals(outputs[0], outputs[3]);
        assertEquals(outputs[4], outputs[5]); assertNotEquals(outputs[4], outputs[6]);
        assertNotEquals(outputs[6], outputs[7]);
    }

    @Test void nestedModuleIncomingEdgesAreMissingFromProcessedCountAndInnerEdgesFromStorageCount() {
        var inner = new NeuralNetwork(factory, 2, new int[0], 1, 1, 0);
        var outer = new NeuralNetwork(factory, 2, new int[]{1}, 1, 0, 0);
        var layer = outer.getNeuralNet().getHiddenLayerArr()[0];
        var module = new NeuronNetwork(layer.getNeuronsArr()[0].getId(), new NeuronTypeInfoData(2, 1, inner.getNeuralNet()), factory);
        layer.getNeuronsArr()[0] = module;
        for (int port = 0; port < 2; port++) connect(outer, new Synapse(
                outer.getNeuralNet().getInputNeuronArr()[port], 0, module, port, 1));
        connect(outer, new Synapse(module, 0, outer.getNeuralNet().getOutputNeuronArr()[0], 0, 1));
        outer.calcProcess(new double[]{1, 1});
        assertEquals(2, inner.getSynapseCount()); assertEquals(3, outer.getSynapseCount());
        assertEquals(3, outer.getProccessedSynapses()); // 5 edges actually participated.
    }
}
