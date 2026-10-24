package de.lifecircles.model.neural;

import de.lifecircles.model.Cell;
import de.lifecircles.service.FileService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Opt-in: forward passes of a whole saved population, brain after brain like one simulation step, so memory
 * traffic is realistic (one brain alone stays in cache). Single thread. Compares the object path and the compiled
 * forward pass, each with and without the execution plan; all must give bit-identical outputs and work bills to the
 * object path without plan. Example:
 * mvn test -Dtest=BrainPopulationBenchmarkTest -Dbrain.bench.cells=target/trainmode-study/prof-world2.cells
 */
@EnabledIfSystemProperty(named="brain.bench.cells",matches=".+")
class BrainPopulationBenchmarkTest {
    static final int ROUNDS=Integer.getInteger("brain.bench.rounds",300);

    record Variant(String name,boolean plan,boolean compiled) {}
    // -Dbrain.bench.variants=objects,compiled selects variants; the first one is the reference.
    static final List<Variant> VARIANTS=List.of(new Variant("objects",false,false),new Variant("objects+plan",true,false),
            new Variant("compiled",false,true),new Variant("compiled+plan",true,true)).stream()
            .filter(x->Arrays.asList(System.getProperty("brain.bench.variants","objects,objects+plan,compiled,compiled+plan").split(",")).contains(x.name())).toList();

    record Population(List<NeuralNetwork> nets,List<double[][]> inputs) {}

    static Population load(String file,Variant variant) throws Exception {
        List<Cell> cells=FileService.getInstance().loadCellsFromFile(file);
        List<NeuralNetwork> nets=new ArrayList<>();
        for(Cell cell:cells)if(!cell.isRemains()&&cell.getBrain() instanceof CellBrain brain) {
            NeuralNetwork net=brain.getNeuralNetwork();
            net.getNeuralNet().setExecutionOptimizationEnabled(variant.plan());net.getNeuralNet().setCompiledExecutionEnabled(variant.compiled());
            nets.add(net);
        }
        Random random=new Random(5);List<double[][]> inputs=new ArrayList<>();
        for(NeuralNetwork net:nets) {
            double[][] series=new double[8][net.getNeuralNet().getInputCount()];
            for(double[] in:series)for(int i=0;i<in.length;i++)in[i]=random.nextDouble()*2-1;
            inputs.add(series);
        }
        return new Population(nets,inputs);
    }

    static long round(Population p,int round,double[][] outputs,long[] work) {
        long start=System.nanoTime();
        for(int i=0;i<p.nets.size();i++) {
            NeuralNetwork net=p.nets.get(i);
            net.setInputs(p.inputs.get(i)[round%8]);
            outputs[i]=net.getNeuralNet().process(net.getNeuronValueFunction(),false);
            work[i]=net.getProccessedSynapses();
        }
        return System.nanoTime()-start;
    }

    /** Funding census of every brain per round: former full walks against the cached census. */
    @Test void census() throws Exception {
        List<NeuralNet> nets=new ArrayList<>();
        for(NeuralNetwork net:load(System.getProperty("brain.bench.cells"),VARIANTS.getFirst()).nets)nets.add(net.getNeuralNet());
        long[] time=new long[2];long check=0;
        for(int r=0;r<ROUNDS;r++)for(int k=0;k<2;k++) {
            int x=(k+r)%2;long t=System.nanoTime(),sum=0;
            for(NeuralNet net:nets)sum+=x==0?BrainCensusTest.work(net,new IdentityHashMap<>())+BrainCensusTest.stored(net,Collections.newSetFromMap(new IdentityHashMap<>()))
                    :net.getMaximumProcessedSynapses()+net.getStoredSynapseCount();
            if(r>=ROUNDS/3)time[x]+=System.nanoTime()-t;
            if(x==0)check=sum;else assertEquals(check,sum);
        }
        int measured=ROUNDS-ROUNDS/3;
        System.out.printf(Locale.ROOT,"BRAINBENCH census: full walk %.3f ms/round, cached %.3f ms/round (%d brains, single thread)%n",
                time[0]/1e6/measured,time[1]/1e6/measured,nets.size());
    }

    @Test void population() throws Exception {
        String file=System.getProperty("brain.bench.cells");
        List<Population> populations=new ArrayList<>();
        for(Variant variant:VARIANTS)populations.add(load(file,variant));
        List<NeuralNetwork> reference=populations.getFirst().nets;
        int n=reference.size(),v=VARIANTS.size();
        long synapses=0;int withModules=0,layers=0;
        for(NeuralNetwork net:reference) {
            synapses+=net.getNeuralNet().getSynapseCount();layers+=net.getHiddenLayerArr().length;
            if(Arrays.stream(net.getHiddenLayerArr()).flatMap(l->Arrays.stream(l.getNeuronsArr())).anyMatch(x->!(x instanceof Neuron)))withModules++;
        }
        System.out.printf(Locale.ROOT,"BRAINBENCH brains=%d synapses/brain=%.0f hiddenLayers/brain=%.1f withModules=%d%n",n,(double)synapses/n,(double)layers/n,withModules);
        double[][][] outputs=new double[v][n][];long[][] work=new long[v][n];long[] time=new long[v];
        for(int r=0;r<ROUNDS;r++) {
            // Rotate the order so no variant profits from a warmer cache.
            for(int k=0;k<v;k++) {
                int x=(k+r)%v;long t=round(populations.get(x),r,outputs[x],work[x]);
                if(r>=ROUNDS/3)time[x]+=t;
            }
            for(int x=1;x<v;x++)for(int i=0;i<n;i++) {
                assertEquals(work[0][i],work[x][i],VARIANTS.get(x).name()+" work of brain "+i+" round "+r);
                for(int o=0;o<outputs[0][i].length;o++)assertEquals(Double.doubleToLongBits(outputs[0][i][o]),Double.doubleToLongBits(outputs[x][i][o]),
                        VARIANTS.get(x).name()+" brain "+i+" output "+o+" round "+r);
            }
        }
        int measured=ROUNDS-ROUNDS/3;
        for(int x=0;x<v;x++)System.out.printf(Locale.ROOT,"BRAINBENCH %-14s %.3f ms/round (%d rounds, single thread)%n",VARIANTS.get(x).name(),time[x]/1e6/measured,measured);
    }
}
