package de.lifecircles.model.neural;

import de.lifecircles.model.CellFactory;
import de.lifecircles.service.SimulationConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Short single-thread study. Allocation and timing are observations, never thresholds. */
@EnabledIfSystemProperty(named="network.performance",matches="true")
class NetworkPerformanceStudyTest {
    private final NeuronValueFunctionFactory factory = new ValuesNeuronValueFunctionFactory();
    private final com.sun.management.ThreadMXBean allocation =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    private final StringBuilder csv = new StringBuilder("layout,repeat,phase,operations,nanos,allocated_bytes,checksum\n");

    private NeuralNetwork create(boolean modules) {
        if (!modules) return new CellBrain(factory,CellFactory.calcInputCount(12),CellFactory.calcOutputCount(12),
                SimulationConfig.hiddenCountFactorDefault,SimulationConfig.stateHiddenLayerSynapseConnectivityDefault,
                SimulationConfig.hiddenLayerSynapseConnectivityDefault).getNeuralNetwork();
        var inner = new NeuralNetwork(factory,4,new int[]{8,8},2,.5,0);
        var outer = new NeuralNetwork(factory,4,new int[]{16},2,0,0);
        var type = new NeuronTypeInfoData(4,2,inner.getNeuralNet());
        try {
            var add = NeuralNet.class.getDeclaredMethod("addSynapse",Synapse.class); add.setAccessible(true);
            var neurons = outer.getHiddenLayerArr()[0].getNeuronsArr();
            for (int i=0;i<neurons.length;i++) {
                var module = new NeuronNetwork(neurons[i].getId(),type,factory); neurons[i]=module;
                for (int p=0;p<4;p++) add.invoke(outer.getNeuralNet(),new Synapse(outer.getInputNeuronArr()[p],0,module,p,.2));
                for (int p=0;p<2;p++) add.invoke(outer.getNeuralNet(),new Synapse(module,p,outer.getOutputNeuronArr()[p],0,.2));
            }
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        return outer;
    }

    private void record(String layout,int repeat,String phase,int count,Runnable task,java.util.function.DoubleSupplier checksum) {
        long thread = Thread.currentThread().threadId();
        long bytes = allocation.getThreadAllocatedBytes(thread), start = System.nanoTime();
        task.run();
        long elapsed = System.nanoTime()-start, allocated = allocation.getThreadAllocatedBytes(thread)-bytes;
        csv.append(String.format(Locale.ROOT,"%s,%d,%s,%d,%d,%d,%.9f%n",layout,repeat,phase,count,elapsed,allocated,checksum.getAsDouble()));
    }

    @Test void measure() throws Exception {
        Random previous = NeuralNetwork.getRandom();
        try {
            for (boolean modules : new boolean[]{false,true}) for (int repeat=0;repeat<3;repeat++) {
                String layout = modules ? "modules" : "scalar";
                NeuralNetwork.setRandom(new Random(701));
                List<NeuralNetwork> networks = new ArrayList<>();
                record(layout,repeat,"build",16,()->{for(int i=0;i<16;i++) networks.add(create(modules));},
                        ()->networks.stream().mapToInt(NeuralNetwork::getSynapseCount).sum());
                double[] inputs = new double[networks.getFirst().getInputNeuronArr().length]; Arrays.fill(inputs,.25);
                for (var net : networks) net.setInputs(inputs);
                for (int i=0;i<100;i++) for(var net:networks) net.getNeuralNet().process(net.getNeuronValueFunction(),false);
                record(layout,repeat,"forward",3200,()->{for(int i=0;i<200;i++) for(var net:networks)
                    net.getNeuralNet().process(net.getNeuronValueFunction(),false);},
                    ()->networks.stream().mapToDouble(net->net.getNeuralNet().getOutputValue(net.getNeuronValueFunction(),0)).sum());
                double[] storage = {0};
                record(layout,repeat,"storage",3200,()->{for(int i=0;i<200;i++) for(var net:networks)
                    storage[0]+=net.getNeuralNet().getStoredSynapseCount();},()->storage[0]);
                List<NeuralNetwork> children = new ArrayList<>();
                record(layout,repeat,"mutation",16,()->{for(var net:networks) children.add(net.mutate(.1,.2));},
                        ()->children.stream().mapToInt(NeuralNetwork::getSynapseCount).sum());
                assertEquals(16,children.size());
            }
            Path output = Path.of(System.getProperty("network.performance.output","target/network-performance.csv"));
            Files.createDirectories(output.toAbsolutePath().getParent()); Files.writeString(output,csv);
            System.out.print(csv);
        } finally { NeuralNetwork.setRandom(previous); }
    }
}
