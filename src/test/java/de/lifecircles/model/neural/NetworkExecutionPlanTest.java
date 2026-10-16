package de.lifecircles.model.neural;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class NetworkExecutionPlanTest {
    private final NeuronValueFunctionFactory factory=new ValuesNeuronValueFunctionFactory();
    private Random previous;
    @BeforeEach void seed(){previous=NeuralNetwork.getRandom();NeuralNetwork.setRandom(new Random(334));}
    @AfterEach void restore(){NeuralNetwork.setRandom(previous);}
    private NeuralNetwork network() {
        var net=new NeuralNetwork(factory,2,new int[]{5},2,0,0);
        var h=net.getHiddenLayerArr()[0];
        NetworkSimplificationTest.connect(net,new Synapse(net.getInputNeuronArr()[0],0,h.getNeuron(1),0,1));
        NetworkSimplificationTest.connect(net,new Synapse(h.getNeuron(1),0,net.getOutputNeuronArr()[0],0,1));
        NetworkSimplificationTest.connect(net,new Synapse(net.getInputNeuronArr()[1],0,h.getNeuron(2),0,.2));
        NetworkSimplificationTest.connect(net,new Synapse(h.getNeuron(2),0,h.getNeuron(3),0,1));
        NetworkSimplificationTest.connect(net,new Synapse(h.getNeuron(3),0,h.getNeuron(2),0,.2));
        return net;
    }
    private double[] run(NeuralNetwork net,double[] inputs) {
        net.setInputs(inputs);return net.getNeuralNet().process(net.getNeuronValueFunction(),false);
    }
    @Test void outputsSchedulingAndOriginalWorkBillMatchWithLatentCycleStillInDna() {
        var original=network();original.getNeuralNet().setExecutionOptimizationEnabled(false);
        var planned=new NeuralNetwork(original);Random random=new Random(2);
        int nodes=planned.getNeuralNet().getAllNeuronsSize(),genes=planned.getSynapseCount();
        for(int t=0;t<2200;t++) {
            double[] input={random.nextDouble(),random.nextDouble()};
            double[] a=run(original,input),b=run(planned,input);
            for(int i=0;i<a.length;i++)assertEquals(Double.doubleToLongBits(a[i]),Double.doubleToLongBits(b[i]));
            assertEquals(original.getProccessedSynapses(),planned.getProccessedSynapses());
            assertEquals(original.getHiddenLayerArr()[0].getActivationCounter(),planned.getHiddenLayerArr()[0].getActivationCounter());
        }
        assertEquals(nodes,planned.getNeuralNet().getAllNeuronsSize());assertEquals(genes,planned.getSynapseCount());
        StructuralMutationTest.assertGraph(planned);
        // Mutation still sees exactly the same genotype, using identical seeds.
        NeuralNetwork.setRandom(new Random(21));var a=original.mutate(.3,.2);
        NeuralNetwork.setRandom(new Random(21));var b=planned.mutate(.3,.2);
        assertArrayEquals(a.getLayerSizes(),b.getLayerSizes());assertEquals(a.getSynapseCount(),b.getSynapseCount());
        for(int i=0;i<a.getSynapseCount();i++)assertEquals(a.getSynapseList().get(i).getWeight(),b.getSynapseList().get(i).getWeight());
        NetworkSimplificationTest.sameSequence(a,b,1200);
    }
    @Test void publicArrayRewiringIsDetectedAndRequiresFreshMemory() {
        var net=network();run(net,new double[]{1,1});
        var out=net.getOutputNeuronArr()[0];var old=out.getInputSynapseArr(0)[0];
        // Deliberately mutate the existing array in-place, bypassing all mutation methods.
        var replacement=new Synapse(net.getHiddenLayerArr()[0].getNeuron(3),0,out,0,1);
        out.setInputSynapseArr(0,new Synapse[]{old});
        out.getInputSynapseArr(0)[0]=replacement;
        assertThrows(IllegalStateException.class,()->run(net,new double[]{2,2}));
        net.rnnClearPreviousState();assertDoesNotThrow(()->run(net,new double[]{2,2}));
    }
    @Test void replacingPayloadInPublicLayerArrayIsDetectedEvenWhenLengthStaysTheSame() {
        var net=network();run(net,new double[]{1,1});
        var h=net.getHiddenLayerArr()[0];
        h.getNeuronsArr()[4]=new Neuron(100,new NeuronTypeInfoData(1,1,null));
        assertThrows(IllegalStateException.class,()->run(net,new double[]{1,1}));
        net.rnnClearPreviousState();assertDoesNotThrow(()->run(net,new double[]{1,1}));
    }
    @Test void weightsAndBiasesMayChangeWithoutInvalidatingReachability() {
        var original=network();original.getNeuralNet().setExecutionOptimizationEnabled(false);
        var planned=new NeuralNetwork(original);
        for(int t=0;t<50;t++) {
            original.getSynapseList().getFirst().setWeight(t*.01);planned.getSynapseList().getFirst().setWeight(t*.01);
            original.getHiddenLayerArr()[0].getNeuron(1).setBias(0,t*.02);planned.getHiddenLayerArr()[0].getNeuron(1).setBias(0,t*.02);
            assertArrayEquals(run(original,new double[]{t,1}),run(planned,new double[]{t,1}));
            assertEquals(original.getProccessedSynapses(),planned.getProccessedSynapses());
        }
    }
    @Test void unreachableModulesStillExecuteAndBillTheirStateDependentWork() {
        var net=network();var inner=new NeuralNetwork(factory,1,new int[]{3},1,1,0);
        var h=net.getHiddenLayerArr()[0];
        var module=new NeuronNetwork(h.getNeuron(4).getId(),new NeuronTypeInfoData(1,1,inner.getNeuralNet()),factory);
        h.getNeuronsArr()[4]=module;
        NetworkSimplificationTest.connect(net,new Synapse(net.getInputNeuronArr()[1],0,module,0,1));
        var original=new NeuralNetwork(net);original.getNeuralNet().setExecutionOptimizationEnabled(false);
        for(int t=0;t<2200;t++) {
            assertArrayEquals(run(original,new double[]{t%7,t%3}),run(net,new double[]{t%7,t%3}));
            assertEquals(original.getProccessedSynapses(),net.getProccessedSynapses());
        }
    }
    @Test void denseNetworkAvoidsRepeatedPlanValidationWhenThereIsNothingToSkip() {
        var net=new NeuralNetwork(factory,2,new int[]{3},2,1,0);
        run(net,new double[]{1,1});assertTrue(net.getNeuralNet().isExecutionOptimizationEnabled());
        NetworkSimplificationTest.connect(net,new Synapse(net.getInputNeuronArr()[0],0,net.getOutputNeuronArr()[0],0,.3));
        assertDoesNotThrow(()->run(net,new double[]{1,1}));
    }
}
