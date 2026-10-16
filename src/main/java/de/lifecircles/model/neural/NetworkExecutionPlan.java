package de.lifecircles.model.neural;

import java.util.*;

/** Read-only execution mask. Every module is a root to preserve its input-dependent work bill. */
final class NetworkExecutionPlan {
    private final Neuron[] inputs, outputs;
    private final NeuronInterface[][] topology;
    private final boolean[][] needed;
    private final NeuronInterface[] dependencies;
    private final Synapse[][][] incoming;
    final long[] skippedWork;
    final boolean skipsNeurons;

    NetworkExecutionPlan(NeuralNet net) {
        inputs=net.getInputNeuronArr().clone(); outputs=net.getOutputNeuronArr().clone();
        Layer[] layers=net.getHiddenLayerArr();
        topology=new NeuronInterface[layers.length][]; needed=new boolean[layers.length][];
        skippedWork=new long[layers.length];
        Set<NeuronInterface> live=Collections.newSetFromMap(new IdentityHashMap<>());
        ArrayDeque<NeuronInterface> pending=new ArrayDeque<>();
        for(Neuron output:outputs)if(live.add(output))pending.add(output);
        for(int i=0;i<layers.length;i++) {
            topology[i]=layers[i].getNeuronsArr().clone();needed[i]=new boolean[topology[i].length];
            for(int j=0;j<topology[i].length;j++) {
                NeuronInterface neuron=topology[i][j];
                if((i>=net.getFixedHiddenLayerCount() && j==0) || !(neuron instanceof Neuron))
                    if(live.add(neuron))pending.add(neuron);
            }
        }
        while(!pending.isEmpty()) {
            NeuronInterface target=pending.removeFirst();
            for(int p=0;p<target.getNeuronTypeInfoData().getInputCount();p++)
                for(Synapse edge:target.getInputSynapseArr(p))
                    if(live.add(edge.getSourceNeuron()))pending.add(edge.getSourceNeuron());
        }
        boolean skips=false;
        for(int i=0;i<layers.length;i++)for(int j=0;j<topology[i].length;j++) {
            needed[i][j]=live.contains(topology[i][j]);skips|=!needed[i][j];
        }
        skipsNeurons=skips;
        dependencies=live.toArray(NeuronInterface[]::new);
        incoming=new Synapse[dependencies.length][][];
        for(int i=0;i<dependencies.length;i++) {
            incoming[i]=new Synapse[dependencies[i].getNeuronTypeInfoData().getInputCount()][];
            for(int p=0;p<incoming[i].length;p++)incoming[i][p]=dependencies[i].getInputSynapseArr(p).clone();
        }
    }

    boolean executes(int layer,int neuron) {return needed[layer][neuron];}

    /** Check public mutable arrays too; weights and biases do not affect reachability. No allocation. */
    boolean matches(NeuralNet net) {
        if(!Arrays.equals(inputs,net.getInputNeuronArr()) || !Arrays.equals(outputs,net.getOutputNeuronArr()))return false;
        Layer[] layers=net.getHiddenLayerArr();if(layers.length!=topology.length)return false;
        for(int i=0;i<layers.length;i++)if(!Arrays.equals(topology[i],layers[i].getNeuronsArr()))return false;
        for(int i=0;i<dependencies.length;i++)for(int p=0;p<incoming[i].length;p++)
            if(!Arrays.equals(incoming[i][p],dependencies[i].getInputSynapseArr(p)))return false;
        // Unobservable scalar inputs may change without becoming reachable. Still bill current work.
        for(int i=0;i<topology.length;i++) {
            long work=0;
            for(int j=0;j<topology[i].length;j++)if(!needed[i][j])
                for(int p=0;p<topology[i][j].getNeuronTypeInfoData().getInputCount();p++)
                    work+=topology[i][j].getInputSynapseArr(p).length;
            skippedWork[i]=work;
        }
        return true;
    }
}
