package de.lifecircles.model.neural;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** The cached census (work bound, stored genes) always equals a full walk of the network, as before. */
class BrainCensusTest {
    private final NeuronValueFunctionFactory factory = new ValuesNeuronValueFunctionFactory();
    private Random previous;
    private boolean verify;
    // Full content check: the in-place writes below are forbidden in production code (NETZKOMPILIERUNG.md).
    @BeforeEach void seed() { previous = NeuralNetwork.getRandom(); NeuralNetwork.setRandom(new Random(4242)); verify = CompiledForward.isVerifyContents(); CompiledForward.setVerifyContents(true); }
    @AfterEach void restore() { NeuralNetwork.setRandom(previous); CompiledForward.setVerifyContents(verify); }

    /** Former walks (before 24.10.2026). */
    static long work(NeuralNet net, Map<NeuralNet, Long> memo) {
        Long known = memo.get(net);
        if (known != null) return known;
        long work = 0;
        List<NeuronInterface> all = new ArrayList<>();
        for (Layer layer : net.getHiddenLayerArr()) all.addAll(Arrays.asList(layer.getNeuronsArr()));
        all.addAll(Arrays.asList(net.getOutputNeuronArr()));
        for (NeuronInterface neuron : all) {
            long own = 0;
            for (int port = 0; port < neuron.getNeuronTypeInfoData().getInputCount(); port++) own = add(own, neuron.getInputSynapseArr(port).length);
            NeuralNet module = neuron.getNeuronTypeInfoData().getNeuralNet();
            work = add(work, module == null ? own : add(own, work(module, memo)));
        }
        memo.put(net, work);
        return work;
    }
    @SuppressWarnings("unchecked")
    static List<NeuronTypeInfoData> types(NeuralNet net) {
        try { var f = NeuralNet.class.getDeclaredField("neuronTypeInfoDataList"); f.setAccessible(true); return (List<NeuronTypeInfoData>) f.get(net); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    static long add(long a, long b) { return a > Long.MAX_VALUE - b ? Long.MAX_VALUE : a + b; }
    static int stored(NeuralNet net, Set<NeuralNet> visited) {
        if (!visited.add(net)) return 0;
        int count = net.getSynapseArr().length;
        for (NeuronTypeInfoData type : types(net)) if (type.getNeuralNet() != null) count += stored(type.getNeuralNet(), visited);
        for (Layer layer : net.getHiddenLayerArr()) for (NeuronInterface neuron : layer.getNeuronsArr()) {
            NeuralNet definition = neuron.getNeuronTypeInfoData().getNeuralNet();
            if (definition != null) count += stored(definition, visited);
        }
        return count;
    }
    static void same(NeuralNetwork net, String where) {
        NeuralNet n = net.getNeuralNet();
        // Twice: building and then reusing the census.
        for (int i = 0; i < 2; i++) {
            assertEquals(work(n, new IdentityHashMap<>()), n.getMaximumProcessedSynapses(), where + " work");
            assertEquals(stored(n, Collections.newSetFromMap(new IdentityHashMap<>())), n.getStoredSynapseCount(), where + " stored");
        }
    }

    @Test void everyKindOfChangeIsSeen() {
        var net = new NeuralNetwork(factory, 4, new int[]{5, 6, 4}, 3, .6, 1);
        same(net, "start");
        var h = net.getHiddenLayerArr();
        NetworkSimplificationTest.connect(net, new Synapse(net.getInputNeuronArr()[0], 0, h[1].getNeuron(2), 0, .5));
        same(net, "added synapse");
        var out = net.getOutputNeuronArr()[1];
        Synapse[] edges = out.getInputSynapseArr(0);
        if (edges.length > 0) { out.removeInputSynapse(0, edges[0]); same(net, "removed input"); }
        out.setInputSynapseArr(0, new Synapse[0]); same(net, "replaced input array");
        var arr = h[2].getNeuronsArr(); var swap = arr[1]; arr[1] = arr[3]; arr[3] = swap; same(net, "swapped in layer");
        arr[2] = new Neuron(900, new NeuronTypeInfoData(1, 1, null)); same(net, "replaced neuron in place");
        arr[3] = new Neuron(901, new NeuronTypeInfoData(3, 1, null)); // multi-port scalar counts its array per port
        NetworkSimplificationTest.connect(net, new Synapse(net.getInputNeuronArr()[1], 0, arr[3], 0, .5)); same(net, "multi-port");
        net.getOutputNeuronArr()[0] = new Neuron(902, new NeuronTypeInfoData(1, 1, null)); same(net, "replaced output");
        // A module: in a layer, then only as an unused type candidate.
        var inner = new NeuralNetwork(factory, 1, new int[]{3}, 1, 1, 0);
        var module = new NeuronNetwork(903, new NeuronTypeInfoData(1, 1, inner.getNeuralNet()), factory);
        NeuronInterface before = h[1].getNeuronsArr()[4];
        h[1].getNeuronsArr()[4] = module; same(net, "module in layer");
        h[1].getNeuronsArr()[4] = before; same(net, "module removed");
        types(net.getNeuralNet()).add(new NeuronTypeInfoData(1, 1, inner.getNeuralNet())); same(net, "module type");
        types(net.getNeuralNet()).removeLast(); same(net, "module type removed");
    }

    @Test void mutatedDescendantsMatch() {
        var net = new NeuralNetwork(factory, 6, new int[]{8, 5}, 4, .5, 1);
        for (int generation = 0; generation < 120; generation++) {
            same(net, "generation " + generation);
            net = net.mutate(.4, .3);
        }
    }
}
