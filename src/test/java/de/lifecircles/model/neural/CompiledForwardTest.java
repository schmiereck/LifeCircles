package de.lifecircles.model.neural;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** The compiled forward pass gives bit-identical outputs, memory, layer timing and work bills to the object path. */
class CompiledForwardTest {
    private final NeuronValueFunctionFactory factory=new ValuesNeuronValueFunctionFactory();
    private Random previous;
    @BeforeEach void seed(){previous=NeuralNetwork.getRandom();NeuralNetwork.setRandom(new Random(818));}
    @AfterEach void restore(){NeuralNetwork.setRandom(previous);}

    /** One fixed and two dynamic layers, sparse, with recurrent edges in a layer and back to an earlier layer. */
    private NeuralNetwork network() {
        var net=new NeuralNetwork(factory,4,new int[]{5,6,4},3,.6,1);
        var h=net.getHiddenLayerArr();
        NetworkSimplificationTest.connect(net,new Synapse(h[0].getNeuron(2),0,h[0].getNeuron(1),0,.7));
        NetworkSimplificationTest.connect(net,new Synapse(h[2].getNeuron(3),0,h[1].getNeuron(0),0,-.4));
        NetworkSimplificationTest.connect(net,new Synapse(net.getOutputNeuronArr()[2],0,h[0].getNeuron(0),0,.9));
        return net;
    }

    /** Reference on the object path without plan; the flag is transient, so copies get it again here. */
    private static NeuralNetwork reference(NeuralNetwork net) {
        net.getNeuralNet().setCompiledExecutionEnabled(false);net.getNeuralNet().setExecutionOptimizationEnabled(false);
        return net;
    }

    private static void sameStep(NeuralNetwork ref,NeuralNetwork compiled,double[] inputs,String where) {
        double[] a=reference(ref).calcProcess(inputs),b=compiled.calcProcess(inputs);
        for(int i=0;i<a.length;i++)assertEquals(Double.doubleToLongBits(a[i]),Double.doubleToLongBits(b[i]),where+" output "+i);
        assertEquals(ref.getProccessedSynapses(),compiled.getProccessedSynapses(),where+" work");
        for(int i=0;i<ref.getHiddenLayerArr().length;i++) {
            assertEquals(ref.getHiddenLayerArr()[i].isActiveLayer(),compiled.getHiddenLayerArr()[i].isActiveLayer(),where+" layer "+i);
            assertEquals(ref.getHiddenLayerArr()[i].getActivationCounter(),compiled.getHiddenLayerArr()[i].getActivationCounter(),where+" counter "+i);
        }
        var x=ref.getNeuralNet();var y=compiled.getNeuralNet();
        for(int i=0;i<x.getHiddenLayerArr().length;i++)for(int j=0;j<x.getHiddenLayerArr()[i].getNeuronsArr().length;j++) {
            var n=x.getHiddenLayerArr()[i].getNeuronsArr()[j];var m=y.getHiddenLayerArr()[i].getNeuronsArr()[j];
            assertEquals(Double.doubleToLongBits(ref.getNeuronValueFunction().readValue(x,n,0)),
                    Double.doubleToLongBits(compiled.getNeuronValueFunction().readValue(y,m,0)),where+" memory "+i+"/"+j);
            assertEquals(Double.doubleToLongBits(ref.getNeuronValueFunction().readInputSum(x,n,0)),
                    Double.doubleToLongBits(compiled.getNeuronValueFunction().readInputSum(y,m,0)),where+" input sum "+i+"/"+j);
        }
    }

    private static Neuron scalar(NeuralNetwork net,int pick) {
        List<Neuron> all=new ArrayList<>();
        for(Layer layer:net.getHiddenLayerArr())for(NeuronInterface n:layer.getNeuronsArr())if(n instanceof Neuron s)all.add(s);
        for(NeuronInterface n:net.getOutputNeuronArr())if(n instanceof Neuron s)all.add(s);
        return all.get(Math.floorMod(pick,all.size()));
    }

    @Test void identicalUnderGeneChangesInPlaceRewiringAndMutation() {
        var compiled=network();var ref=reference(new NeuralNetwork(compiled));
        Random random=new Random(3);var functions=ActivationFunction.values();
        for(int t=0;t<4000;t++) {
            double[] inputs=new double[compiled.getInputNeuronArr().length];
            for(int i=0;i<inputs.length;i++)inputs[i]=random.nextDouble()*4-2;
            int pick=random.nextInt(1000);
            switch(t%50) {
                case 7->{int s=Math.floorMod(pick,compiled.getSynapseCount());double w=random.nextDouble()*2-1;
                    ref.getSynapseList().get(s).setWeight(w);compiled.getSynapseList().get(s).setWeight(w);}
                case 13->{double b=random.nextDouble()*2-1;scalar(ref,pick).setBias(0,b);scalar(compiled,pick).setBias(0,b);}
                case 19->{var f=functions[random.nextInt(functions.length)];scalar(ref,pick).setActivationFunction(f);scalar(compiled,pick).setActivationFunction(f);}
                case 23->{ // Swap two edges inside the live array: same set, different summation order.
                    for(var net:List.of(ref,compiled)) {var arr=scalar(net,pick).getInputSynapseArr(0);
                        if(arr.length>1){var e=arr[0];arr[0]=arr[arr.length-1];arr[arr.length-1]=e;}}}
                case 29->{for(var net:List.of(ref,compiled)) {var target=scalar(net,pick);
                    NetworkSimplificationTest.connect(net,new Synapse(net.getInputNeuronArr()[pick%4],0,target,0,.25));}}
                case 37->{ // Structural mutation with identical seeds; the children start with fresh memory.
                    long s=random.nextLong();
                    NeuralNetwork.setRandom(new Random(s));var a=ref.mutate(.3,.2);
                    NeuralNetwork.setRandom(new Random(s));var b=compiled.mutate(.3,.2);
                    ref=a;compiled=b;}
                case 41->{ // Fixed layers switched from outside, like cell-state scheduling.
                    boolean active=random.nextBoolean();ref.getHiddenLayerArr()[0].setActiveLayer(active);compiled.getHiddenLayerArr()[0].setActiveLayer(active);}
                default->{}
            }
            sameStep(ref,compiled,inputs,"tick "+t);
        }
    }

    @Test void layerDeactivationDisabledStaysIdentical() {
        var compiled=network();var ref=reference(new NeuralNetwork(compiled));
        compiled.getNeuralNet().setDisableLayerDeactivation(true);ref.getNeuralNet().setDisableLayerDeactivation(true);
        for(int t=0;t<300;t++)sameStep(ref,compiled,new double[]{t%5,-t%3,.5,1},"tick "+t);
    }

    @Test void modulesAndMultiPortWiringFallBackToTheObjectPath() {
        var net=network();var inner=new NeuralNetwork(factory,1,new int[]{3},1,1,0);
        var h=net.getHiddenLayerArr()[1];
        var module=new NeuronNetwork(h.getNeuron(4).getId(),new NeuronTypeInfoData(1,1,inner.getNeuralNet()),factory);
        h.getNeuronsArr()[4]=module;
        NetworkSimplificationTest.connect(net,new Synapse(net.getInputNeuronArr()[1],0,module,0,1));
        NetworkSimplificationTest.connect(net,new Synapse(module,0,net.getOutputNeuronArr()[0],0,.5));
        // Two copies: the hand-inserted module leaves stale edges in the original that a copy does not keep.
        var compiled=new NeuralNetwork(net);var ref=reference(new NeuralNetwork(net));
        for(int t=0;t<500;t++)sameStep(ref,compiled,new double[]{t%7,t%3,-1,.2},"tick "+t);
        assertFalse(new CompiledForward(compiled.getNeuralNet()).eligible);
        assertTrue(new CompiledForward(network().getNeuralNet()).eligible);
    }

    @Test void inPlaceNeuronReplacementInALayerIsDetected() {
        var compiled=network();var ref=reference(new NeuralNetwork(compiled));
        sameStep(ref,compiled,new double[]{1,1,1,1},"before");
        for(var net:List.of(ref,compiled)) { // Same payload object in both layer arrays, swapped in place.
            var arr=net.getHiddenLayerArr()[1].getNeuronsArr();var e=arr[1];arr[1]=arr[2];arr[2]=e;
        }
        for(int t=0;t<200;t++)sameStep(ref,compiled,new double[]{t%3,1,-.5,.25},"tick "+t);
    }
}
