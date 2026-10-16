package de.lifecircles.model.neural;

import de.lifecircles.model.Cell;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.lang.management.ManagementFactory;
import static org.junit.jupiter.api.Assertions.*;

/** Explicit short study of exact forward equivalence and isolated execution cost; no evolution. */
@EnabledIfSystemProperty(named="simplification.study.folder",matches=".+")
class NetworkSimplificationStudyTest {
    private static final int MODELS=18, STEPS=2200, REPEATS=7, BENCH_STEPS=2000;
    private final com.sun.management.ThreadMXBean allocation=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
    private volatile double sink;
    @SuppressWarnings("unchecked") private NeuralNetwork load(Path path) throws Exception {
        try(var input=new ObjectInputStream(Files.newInputStream(path))) {
            return ((CellBrain)((List<Cell>)input.readObject()).getFirst().getBrain()).getNeuralNetwork();
        }
    }
    private void state(NeuralNetwork net,int state) {
        for(int i=0;i<net.getNeuralNet().getFixedHiddenLayerCount();i++)net.getHiddenLayerArr()[i].setActiveLayer((state&(1<<i))!=0);
    }
    private double[] inputs(NeuralNetwork net,Random random) {
        double[] inputs=new double[net.getInputNeuronArr().length];
        for(int i=0;i<inputs.length;i++)inputs[i]=random.nextDouble()*4-2;
        // Include raw age/force magnitudes rather than testing only small normalized signals.
        for(int i=0;i<inputs.length;i+=17)inputs[i]=random.nextDouble()*200-100;
        return inputs;
    }
    private double[] process(NeuralNetwork net,double[] inputs) {
        net.setInputs(inputs);return net.getNeuralNet().process(net.getNeuronValueFunction(),false);
    }
    private void verify(NeuralNetwork source,NeuralNetwork compact,int birthState,StringBuilder out,int model) {
        NeuralNetwork original=new NeuralNetwork(source), reduced=new NeuralNetwork(compact);
        state(original,birthState);state(reduced,birthState);
        Random random=new Random(4117+birthState);long oldWork=0,newWork=0,comparisons=0;
        for(int t=0;t<STEPS;t++) {
            if(t==1100) {int next=(birthState+3)%8;state(original,next);state(reduced,next);}
            double[] inputs=inputs(original,random), a=process(original,inputs), b=process(reduced,inputs);
            for(int i=0;i<a.length;i++) {
                assertTrue(Double.isFinite(a[i]) && Double.isFinite(b[i]));
                assertEquals(Double.doubleToLongBits(a[i]),Double.doubleToLongBits(b[i]),"model="+model+", state="+birthState+", tick="+t+", output="+i);
                comparisons++;
            }
            for(int i=0;i<original.getHiddenLayerArr().length;i++) {
                assertEquals(original.getHiddenLayerArr()[i].isActiveLayer(),reduced.getHiddenLayerArr()[i].isActiveLayer());
                assertEquals(original.getHiddenLayerArr()[i].getActivationCounter(),reduced.getHiddenLayerArr()[i].getActivationCounter());
            }
            oldWork+=original.getProccessedSynapses();newWork+=reduced.getProccessedSynapses();
        }
        out.append(String.format(Locale.ROOT,"%d,%d,%d,%d,%d,%d%n",model,birthState,STEPS,comparisons,oldWork,newWork));
    }
    private void benchmark(NeuralNetwork template,String mode,int model,int repeat,StringBuilder out) {
        NeuralNetwork net=new NeuralNetwork(template);state(net,7);
        Random random=new Random(12);double[][] inputs=new double[64][];
        for(int i=0;i<inputs.length;i++)inputs[i]=inputs(net,random);
        for(int i=0;i<500;i++)process(net,inputs[i%64]);
        long id=Thread.currentThread().threadId(),bytes=allocation.getThreadAllocatedBytes(id),start=System.nanoTime();
        double checksum=0;long work=0;
        for(int i=0;i<BENCH_STEPS;i++) {
            double[] result=process(net,inputs[i%64]);
            checksum+=result[i%result.length];work+=net.getProccessedSynapses();
        }
        long elapsed=System.nanoTime()-start,allocated=allocation.getThreadAllocatedBytes(id)-bytes;
        sink=checksum;
        out.append(String.format(Locale.ROOT,"%d,%s,%d,%d,%d,%d,%d,%.17g%n",model,mode,repeat,BENCH_STEPS,elapsed,allocated,work,checksum));
    }
    @Test void compareSavedGenomes() throws Exception {
        Path source=Path.of(System.getProperty("simplification.study.folder"));
        Path output=Path.of(System.getProperty("simplification.study.output","target/network-simplification"));Files.createDirectories(output);
        StringBuilder counts=new StringBuilder("model,root_neurons_before,root_neurons_after,root_synapses_before,root_synapses_after,stored_synapses_before,stored_synapses_after,copy_bytes_before,copy_bytes_after\n");
        StringBuilder equivalence=new StringBuilder("model,birth_state,steps,output_comparisons,executed_before,executed_after\n");
        StringBuilder timing=new StringBuilder("model,mode,repeat,steps,nanos,allocated_bytes,executed_synapses,checksum\n");
        Random previous=NeuralNetwork.getRandom();
        try {
            NeuralNetwork.setRandom(new Random(119));
            for(int model=0;model<MODELS;model++) {
                NeuralNetwork original=new NeuralNetwork(load(source.resolve("model-"+model+".cells")));
                NeuralNetwork compact=original.simplifiedCopy();
                StructuralMutationTest.assertGraph(compact);
                counts.append(String.format(Locale.ROOT,"%d,%d,%d,%d,%d,%d,%d,%d,%d%n",model,
                        original.getNeuralNet().getAllNeuronsSize(),compact.getNeuralNet().getAllNeuronsSize(),
                        original.getSynapseCount(),compact.getSynapseCount(),original.getNeuralNet().getStoredSynapseCount(),compact.getNeuralNet().getStoredSynapseCount(),
                        bytes(original),bytes(compact)));
                for(int state=0;state<8;state++)verify(original,compact,state,equivalence,model);
                for(int repeat=0;repeat<REPEATS;repeat++) {
                    // Alternate order to reduce systematic warmup/temperature bias.
                    if(repeat%2==0) {benchmark(original,"original",model,repeat,timing);benchmark(compact,"compact",model,repeat,timing);}
                    else {benchmark(compact,"compact",model,repeat,timing);benchmark(original,"original",model,repeat,timing);}
                }
                Files.writeString(output.resolve("counts.csv"),counts);
                Files.writeString(output.resolve("equivalence.csv"),equivalence);
                Files.writeString(output.resolve("timing.csv"),timing);
                System.out.println("Exact simplification: model "+model+" checked in all 8 states");
            }
            Files.writeString(output.resolve("settings.txt"),"source="+source+"\nmodels=18\nstate_sequences=8 per model\nsteps_per_sequence=2200\nstate_change_at=1100\nfinite_random_inputs=[-2,2], every 17th [-100,100]\nbenchmark=single-thread no training trace, state 7; 500 warmup + 2000 timed steps; 7 alternating repeats\nsemantics=exact forward outputs and scheduling; fresh memory; fees and mutation distribution may change\nunused_type_candidates=retained\n");
        } finally {NeuralNetwork.setRandom(previous);}
    }
    private int bytes(NeuralNetwork network) throws Exception {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(var out=new ObjectOutputStream(bytes)){out.writeObject(network);}return bytes.size();
    }
}
