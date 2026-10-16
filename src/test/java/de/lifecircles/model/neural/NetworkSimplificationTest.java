package de.lifecircles.model.neural;

import org.junit.jupiter.api.*;
import java.util.*;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;

class NetworkSimplificationTest {
    private final NeuronValueFunctionFactory factory = new ValuesNeuronValueFunctionFactory();
    private Random previous;
    @BeforeEach void seed() {previous=NeuralNetwork.getRandom();NeuralNetwork.setRandom(new Random(511));}
    @AfterEach void restore() {NeuralNetwork.setRandom(previous);}
    static void connect(NeuralNetwork net, Synapse edge) {
        try {var m=NeuralNet.class.getDeclaredMethod("addSynapse",Synapse.class);m.setAccessible(true);m.invoke(net.getNeuralNet(),edge);}
        catch(ReflectiveOperationException e) {throw new AssertionError(e);}
    }
    static void sameSequence(NeuralNetwork original, NeuralNetwork compact, int steps) {
        NeuralNetwork reference=new NeuralNetwork(original);
        Random random=new Random(722);
        for(int t=0;t<steps;t++) {
            if(t%137==0) {
                int state=random.nextInt(8);
                for(int i=0;i<reference.getNeuralNet().getFixedHiddenLayerCount();i++) {
                    boolean active=(state&(1<<i))!=0;
                    reference.getHiddenLayerArr()[i].setActiveLayer(active);compact.getHiddenLayerArr()[i].setActiveLayer(active);
                }
            }
            double[] inputs=new double[reference.getInputNeuronArr().length];
            for(int i=0;i<inputs.length;i++) inputs[i]=random.nextDouble()*4-2;
            double[] a=reference.calcProcess(inputs),b=compact.calcProcess(inputs);
            for(int i=0;i<a.length;i++) assertEquals(Double.doubleToLongBits(a[i]),Double.doubleToLongBits(b[i]),"tick="+t+", output="+i);
            for(int i=0;i<reference.getHiddenLayerArr().length;i++) {
                assertEquals(reference.getHiddenLayerArr()[i].isActiveLayer(),compact.getHiddenLayerArr()[i].isActiveLayer());
                assertEquals(reference.getHiddenLayerArr()[i].getActivationCounter(),compact.getHiddenLayerArr()[i].getActivationCounter());
            }
        }
    }
    @Test void removesDisconnectedCycleButKeepsControllerAncestorsAndLiveRecurrentLoop() {
        NeuralNetwork net=new NeuralNetwork(factory,2,new int[]{6},1,0,0);
        var h=net.getHiddenLayerArr()[0].getNeuronsArr(); var in=net.getInputNeuronArr();
        connect(net,new Synapse(in[0],0,h[1],0,1));
        connect(net,new Synapse(h[1],0,h[0],0,1)); // Indirect controller dependency.
        connect(net,new Synapse(in[1],0,h[2],0,1));
        connect(net,new Synapse(h[2],0,h[2],0,.3));
        connect(net,new Synapse(h[2],0,net.getOutputNeuronArr()[0],0,1));
        connect(net,new Synapse(h[3],0,h[4],0,1));connect(net,new Synapse(h[4],0,h[3],0,1));
        NeuralNetwork compact=net.simplifiedCopy();
        assertEquals(3,compact.getHiddenLayerArr()[0].size()); assertEquals(5,compact.getSynapseCount());
        assertEquals(6,net.getHiddenLayerArr()[0].size()); assertEquals(7,net.getSynapseCount());
        sameSequence(net,compact,2200);
    }
    @Test void allStateSlotsSurviveIncludingEmptyFixedLayersAndTemporarilyDisabledPayload() {
        NeuralNetwork net=new NeuralNetwork(factory,1,new int[]{3,3,3,3},1,0,3);
        var h=net.getHiddenLayerArr();
        connect(net,new Synapse(net.getInputNeuronArr()[0],0,h[1].getNeuron(2),0,1));
        connect(net,new Synapse(h[1].getNeuron(2),0,net.getOutputNeuronArr()[0],0,1));
        h[1].setActiveLayer(false);
        NeuralNetwork compact=net.simplifiedCopy();
        assertArrayEquals(new int[]{1,0,1,0,1,1},compact.getLayerSizes());
        sameSequence(net,compact,2200);
        compact.setDisableLayerDeactivation(true);net.setDisableLayerDeactivation(true);
        compact.rnnClearPreviousState();sameSequence(net,compact,100);
    }
    @Test void zeroWeightDependencyAndNonfinitePropagationAreNotPruned() {
        NeuralNetwork net=new NeuralNetwork(factory,1,new int[]{2},1,0,0);
        var hidden=net.getHiddenLayerArr()[0].getNeuron(1);
        connect(net,new Synapse(net.getInputNeuronArr()[0],0,hidden,0,1));
        connect(net,new Synapse(hidden,0,net.getOutputNeuronArr()[0],0,0));
        var compact=net.simplifiedCopy();assertEquals(2,compact.getHiddenLayerArr()[0].size());
        assertArrayEquals(net.calcProcess(new double[]{Double.NaN}),compact.calcProcess(new double[]{Double.NaN}));
    }
    @Test void sharedMultiportModuleIsSimplifiedOnceWithIndependentExecutionMemory() throws Exception {
        NeuralNetwork inner=new NeuralNetwork(factory,2,new int[]{4},2,0,0);
        var ih=inner.getHiddenLayerArr()[0];
        connect(inner,new Synapse(inner.getInputNeuronArr()[0],0,ih.getNeuron(1),0,1));
        connect(inner,new Synapse(ih.getNeuron(1),0,inner.getOutputNeuronArr()[1],0,1));
        connect(inner,new Synapse(inner.getOutputNeuronArr()[1],0,ih.getNeuron(1),0,.2));
        NeuralNetwork outer=new NeuralNetwork(factory,2,new int[]{3},2,0,1);
        var type=new NeuronTypeInfoData(2,2,inner.getNeuralNet());
        for(int i=0;i<2;i++) {
            var module=new NeuronNetwork(outer.getHiddenLayerArr()[0].getNeuron(i).getId(),type,factory);
            outer.getHiddenLayerArr()[0].getNeuronsArr()[i]=module;
            connect(outer,new Synapse(outer.getInputNeuronArr()[i],0,module,i,1));
            connect(outer,new Synapse(module,1,outer.getOutputNeuronArr()[i],0,1));
        }
        var compact=outer.simplifiedCopy();
        var ca=compact.getHiddenLayerArr()[0].getNeuron(0).getNeuronTypeInfoData().getNeuralNet();
        assertSame(ca,compact.getHiddenLayerArr()[0].getNeuron(1).getNeuronTypeInfoData().getNeuralNet());
        assertEquals(2,ca.getHiddenLayerArr()[0].size());assertEquals(4,ih.size());
        sameSequence(outer,compact,2200);
        var twice=compact.simplifiedCopy();assertEquals(compact.getSynapseCount(),twice.getSynapseCount());
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(var out=new ObjectOutputStream(bytes)){out.writeObject(twice);}
        NeuralNetwork loaded;try(var in=new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))){loaded=(NeuralNetwork)in.readObject();}
        loaded.rnnClearPreviousState();sameSequence(outer,loaded,2200);
        StructuralMutationTest.assertGraph(compact);
        for(int seed=0;seed<10;seed++) {NeuralNetwork.setRandom(new Random(seed));StructuralMutationTest.assertGraph(compact.mutate(.5,.2));}
    }
}
