package de.lifecircles.model.neural;

import de.lifecircles.model.*;
import de.lifecircles.service.ReproductionManagerService;
import org.junit.jupiter.api.*;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in observations of CURRENT behavior, including defects. Rewrite expectations when fixing them. */
class ModuleMutationCostTest {
    private final NeuronValueFunctionFactory factory = new ValuesNeuronValueFunctionFactory();
    private Random previous;

    @Test void sharedDefinitionMutatesOnceAndStorageCountsItOnce() {
        var inner = new NeuralNetwork(factory,1,new int[0],1,1,0);
        var outer = new NeuralNetwork(factory,1,new int[]{2},1,0,1);
        var type = new NeuronTypeInfoData(1,1,inner.getNeuralNet());
        var neurons = outer.getHiddenLayerArr()[0].getNeuronsArr();
        for (int i=0;i<2;i++) {
            var module = new NeuronNetwork(neurons[i].getId(),type,factory); neurons[i]=module;
            connect(outer,new Synapse(outer.getInputNeuronArr()[0],0,module,0,1));
            connect(outer,new Synapse(module,0,outer.getOutputNeuronArr()[0],0,1));
        }
        double original = inner.getSynapseList().getFirst().getWeight();
        NeuralNetwork.setRandom(new Random(1) {
            @Override public double nextDouble() { return .75; }
        });
        var child = outer.mutate(1,.2);
        var copied = child.getHiddenLayerArr()[0].getNeuron(0).getNeuronTypeInfoData().getNeuralNet();
        assertSame(copied,child.getHiddenLayerArr()[0].getNeuron(1).getNeuronTypeInfoData().getNeuralNet());
        assertEquals(original+.1,copied.getSynapseList().getFirst().getWeight(),1e-15);
        assertEquals(original,inner.getSynapseList().getFirst().getWeight());
        assertEquals(5,outer.getNeuralNet().getStoredSynapseCount());
        outer.calcProcess(new double[]{.5}); assertEquals(6,outer.getProccessedSynapses());
    }

    @Test void boundedUnusedTypeNurseryPreservesLocalIdsDuringInterfaceInnovation() throws Exception {
        var outer = new NeuralNetwork(factory,2,new int[]{3},1,1,0);
        for (int i=0;i<80;i++)
            outer.getNeuralNet().mutateNeuronTypeInfoDataList(factory,outer.getNeuronValueFunction(),new Random(i),1,1);
        var field = NeuralNet.class.getDeclaredField("neuronTypeInfoDataList"); field.setAccessible(true);
        @SuppressWarnings("unchecked") var types = (List<NeuronTypeInfoData>)field.get(outer.getNeuralNet());
        assertTrue(types.size() <= NeuralNet.UNUSED_TYPE_LIMIT+1);
        StructuralMutationTest.assertGraph(outer);
        for (var type : types) if (type.getNeuralNet()!=null)
            StructuralMutationTest.assertGraph(new NeuralNetwork(type.getNeuralNet(),factory));
        for (int i=0;i<20;i++) outer.getNeuralNet().addNeuronToHiddenLayer(factory,outer.getNeuronValueFunction(),new Random(i),0,.2);
        StructuralMutationTest.assertGraph(outer);
    }
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

    @Test void moduleBiasMutationHonorsZeroMutationStrength() {
        var inner = new NeuralNetwork(factory, 1, new int[]{1}, 1, 1, 0);
        var module = new NeuronNetwork(10, new NeuronTypeInfoData(1, 1, inner.getNeuralNet()), factory);
        module.mutateNeuron(new Random(1) {
            @Override public double nextDouble() { return .75; }
            @Override public int nextInt(int bound) { return 0; }
        }, 1, 0);
        assertEquals(0, module.getBias(0));
    }

    @Test void nestedModuleCountsStoredGenesAndAllExecutedConnections() {
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
        assertEquals(5, outer.getProccessedSynapses());
        assertEquals(5,outer.getNeuralNet().getStoredSynapseCount());
        assertEquals(5,new CellBrain(outer).getSynapseCount()); // 5 edges actually participated.
    }
}
