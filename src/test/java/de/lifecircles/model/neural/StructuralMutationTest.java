package de.lifecircles.model.neural;

import de.lifecircles.model.*;
import de.lifecircles.service.ReproductionManagerService;
import org.junit.jupiter.api.*;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Structural mutations must preserve a closed graph with unique value-storage IDs. */
class StructuralMutationTest {
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

    @Test void neuronDeletionRemovesNeuronAndSafelyReusesId() {
        var net = heldNetwork(); var layer = net.getNeuralNet().getHiddenLayerArr()[0];
        var removed = layer.getNeuronsArr()[1];
        net.getNeuralNet().removeNeuronFromHiddenLayer(net.getNeuronValueFunction(), new Random(1), 0);
        assertEquals(1, layer.getNeuronsArr().length);
        assertEquals(0, removed.getInputSynapseArr(0).length);
        assertTrue(removed.getOutputSynapseList(0).isEmpty());
        assertEquals(0, net.getSynapseCount());
        net.getNeuralNet().addNeuronToHiddenLayer(factory, net.getNeuronValueFunction(), new Random(1), 0, 0);
        assertEquals(2, layer.getNeuronsArr().length);
        assertEquals(removed.getId(), layer.getNeuronsArr()[1].getId());
    }

    @Test void layerDeletionRemovesAllIncidentEdges() {
        var net = new NeuralNetwork(factory, 1, new int[]{1}, 1, 1, 0);
        NeuralNetwork.setRandom(new Random(1) {
            int call;
            @Override public double nextDouble() { return call++ == 1 ? 0 : .9; }
            @Override public int nextInt(int bound) { return 0; }
        });
        net.getNeuralNet().applyStructuralMutations(factory, net.getNeuronValueFunction(), 1, .2, false);
        assertEquals(0, net.getNeuralNet().getHiddenLayerArr().length);
        assertEquals(0, net.getSynapseCount());
        Set<NeuronInterface> live = new HashSet<>(); net.getNeuralNet().applyToAllNeurons(live::add);
        assertGraph(net);
        assertEquals(0, new NeuralNetwork(net).getSynapseCount());
    }

    @Test void randomEdgeNeverTargetsInput() {
        var net = new NeuralNetwork(factory, 1, new int[0], 1, 0, 0);
        net.getNeuralNet().addRandomSynapse(new Random(1) {
            int call;
            @Override public int nextInt(int bound) { return call++ == 0 ? 1 : 0; }
        });
        assertEquals(0, net.getSynapseCount());
    }

    @Test void aggressiveMutationHandlesNoHiddenLayer() {
        var net = new NeuralNetwork(factory, 1, new int[0], 1, 0, 0);
        NeuralNetwork.setRandom(new Random(1) {
            int call;
            @Override public double nextDouble() { return call++ == 0 ? .9 : 0; }
        });
        assertDoesNotThrow(() -> net.getNeuralNet().mutateAggressively(
                factory, net.getNeuronValueFunction(), 0, .2));
    }

    static void assertGraph(NeuralNetwork net) {
        Set<Integer> ids = new HashSet<>();
        Set<NeuronInterface> live = Collections.newSetFromMap(new IdentityHashMap<>());
        net.getNeuralNet().applyToAllNeurons(n -> { assertTrue(ids.add(n.getId()), "duplicate ID"); live.add(n); });
        Set<Synapse> edges = Collections.newSetFromMap(new IdentityHashMap<>());
        edges.addAll(net.getSynapseList());
        assertEquals(net.getSynapseCount(), edges.size());
        for (Synapse e : edges) {
            assertTrue(live.contains(e.getSourceNeuron())); assertTrue(live.contains(e.getTargetNeuron()));
            assertTrue(e.getSourceNeuron().getOutputSynapseList(e.getSourceOutputTypePos()).contains(e));
            assertTrue(Arrays.asList(e.getTargetNeuron().getInputSynapseArr(e.getTargetInputTypePos())).contains(e));
            assertFalse(Arrays.asList(net.getInputNeuronArr()).contains(e.getTargetNeuron()));
        }
        for (NeuronInterface n : live) {
            for (int p=0; p<n.getNeuronTypeInfoData().getInputCount(); p++)
                for (Synapse e : n.getInputSynapseArr(p)) { assertTrue(edges.contains(e)); assertSame(n,e.getTargetNeuron()); assertEquals(p,e.getTargetInputTypePos()); }
            for (int p=0; p<n.getNeuronTypeInfoData().getOutputCount(); p++)
                for (Synapse e : n.getOutputSynapseList(p)) { assertTrue(edges.contains(e)); assertSame(n,e.getSourceNeuron()); assertEquals(p,e.getSourceOutputTypePos()); }
        }
    }

    @Test void seededMutationChainsAndCopiesPreserveGraph() {
        for (int seed=0; seed<12; seed++) {
            NeuralNetwork.setRandom(new Random(seed));
            var net = new NeuralNetwork(factory, 3, new int[]{3,2}, 2, .5, 1);
            Layer fixed = net.getHiddenLayerArr()[0];
            for (int step=0; step<80; step++) {
                net.getNeuralNet().applyStructuralMutations(factory, net.getNeuronValueFunction(), 1, .2, false);
                if (step%10==0) net.getNeuralNet().mutateAggressively(factory,net.getNeuronValueFunction(),1,.2);
                assertSame(fixed,net.getHiddenLayerArr()[0]);
                assertGraph(net); assertGraph(new NeuralNetwork(net));
                for (double out : net.calcProcess(new double[]{.1,.2,.3})) assertTrue(Double.isFinite(out));
            }
        }
    }

    @Test void entireMultiNeuronLayerIsRemoved() throws Exception {
        var net = new NeuralNetwork(factory, 2, new int[]{3,3}, 1, 1, 1);
        var method = NeuralNet.class.getDeclaredMethod("removeHiddenLayer", NeuronValueFunction.class, int.class);
        method.setAccessible(true); method.invoke(net.getNeuralNet(),net.getNeuronValueFunction(),1);
        assertEquals(1,net.getHiddenLayerArr().length); assertGraph(net);
    }
}
