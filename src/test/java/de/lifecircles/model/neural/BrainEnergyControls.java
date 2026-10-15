package de.lifecircles.model.neural;

import java.lang.reflect.Method;
import java.util.Random;

/** Explicit positive controls. Never used to seed the acquisition experiments. */
final class BrainEnergyControls {
    private static void edge(NeuralNetwork n,NeuronInterface a,NeuronInterface b,double weight) {
        try {
            Method add=NeuralNet.class.getDeclaredMethod("addSynapse",Synapse.class);add.setAccessible(true);
            add.invoke(n.getNeuralNet(),new Synapse(a,0,b,0,weight));
        } catch(ReflectiveOperationException exception) {throw new AssertionError(exception);}
    }
    static NeuralNetwork known(BrainEnergyLab.Task task,boolean bloat) {
        NeuralNetwork.setRandom(new Random(917));
        int count=switch(task){case XOR,DELAY->3;case COUNTER->13;default->0;};
        var n=new NeuralNetwork(new ValuesNeuronValueFunctionFactory(),2,count==0?new int[0]:new int[]{count},1,0,0);
        n.setEnableNeuronType(true);
        var in=n.getInputNeuronArr();var out=n.getOutputNeuronArr()[0];
        out.setBias(0,-10);
        NeuronInterface[] h=count==0?new NeuronInterface[0]:n.getHiddenLayerArr()[0].getNeuronsArr();
        for(NeuronInterface node:h) {((Neuron)node).setActivationFunction(ActivationFunction.ReLU);node.setBias(0,0);}
        if(count>0) h[0].setBias(0,1); // scheduling controller: payload active on every call
        switch(task) {
            case CONSTANT -> out.setBias(0,Math.log(.7/.3));
            case AND -> {out.setBias(0,-30);edge(n,in[0],out,20);edge(n,in[1],out,20);}
            case OR -> {edge(n,in[0],out,20);edge(n,in[1],out,20);}
            case XOR -> {
                edge(n,in[0],h[1],1);edge(n,in[1],h[1],-1);
                edge(n,in[0],h[2],-1);edge(n,in[1],h[2],1);
                edge(n,h[1],out,20);edge(n,h[2],out,20);
            }
            case DELAY -> {edge(n,h[2],h[1],1);edge(n,in[0],h[2],1);edge(n,h[1],out,20);}
            case COUNTER -> {
                // Six conjunctions compute new flags 1/2/3 from the old late buffers 10/11/12.
                edge(n,in[0],h[1],1);
                for(int old=10;old<=12;old++)edge(n,h[old],h[1],-1);
                edge(n,h[10],h[2],1);edge(n,in[0],h[2],-1);
                edge(n,h[10],h[3],1);edge(n,in[0],h[3],1);h[3].setBias(0,-1);
                edge(n,h[11],h[4],1);edge(n,in[0],h[4],-1);
                edge(n,h[11],h[5],1);edge(n,in[0],h[5],1);h[5].setBias(0,-1);
                edge(n,h[12],h[6],1);edge(n,in[0],h[6],-1);
                for(int gate=1;gate<=6;gate++)edge(n,in[1],h[gate],-2);
                for(int state=0;state<3;state++) {
                    edge(n,h[1+state*2],h[7+state],1);edge(n,h[2+state*2],h[7+state],1);
                    edge(n,h[7+state],h[10+state],1);
                }
                edge(n,h[7],out,10-Math.log(2));edge(n,h[8],out,10+Math.log(2));edge(n,h[9],out,20);
            }
        }
        if(bloat) {
            n.getNeuralNet().addHiddenLayer(n.getNeuronValueFunctionFactory(),n.getNeuronValueFunction(),n.getHiddenLayerArr().length,12,0);
            var extra=n.getHiddenLayerArr()[n.getHiddenLayerArr().length-1].getNeuronsArr();
            for(var node:extra) {edge(n,in[0],node,.25);edge(n,in[1],node,-.25);}
        }
        return n;
    }
}
