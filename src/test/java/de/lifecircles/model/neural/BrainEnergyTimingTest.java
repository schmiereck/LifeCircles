package de.lifecircles.model.neural;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Separate sequential timing after the evolutionary jobs have finished. */
@EnabledIfSystemProperty(named="brain.energy.bench.folders",matches=".+")
class BrainEnergyTimingTest {
    private static volatile double blackhole;
    record Model(String dataset,String phase,String task,String storage,String work,String seed,
                 boolean solved,NeuralNetwork network,BrainEnergyLab.Data data) {}
    @Test void timeSavedChampions() throws Exception {
        Set<String> prices=new HashSet<>(Arrays.asList(System.getProperty("brain.energy.bench.prices","0:0,1:1,100:0,100:0.1").split(",")));
        List<Model> models=new ArrayList<>();
        for(String folder:System.getProperty("brain.energy.bench.folders").split(",")) {
            Path root=Path.of(folder);List<String> lines=Files.readAllLines(root.resolve("results.csv"));
            for(String line:lines.subList(1,lines.size())) {
                String[] r=line.split(",");if(!prices.contains(r[2]+":"+r[3]))continue;
                String name=r[0]+"-"+r[1]+"-s"+r[2]+"-w"+r[3]+"-seed"+r[5]+".nn";
                NeuralNetwork network;
                try(var stream=new ObjectInputStream(new BufferedInputStream(Files.newInputStream(root.resolve(name))))) {network=(NeuralNetwork)stream.readObject();}
                var task=BrainEnergyLab.Task.valueOf(r[1]);int seed=Integer.parseInt(r[5]);
                var data=BrainEnergyLab.data(task,BrainEnergyLab.seed(seed,100000,task.ordinal()),true);
                var metrics=BrainEnergyLab.evaluate(network,data,task);
                assertEquals(Double.parseDouble(r[9]),metrics.loss(),1e-8,"Serialized model changed behavior");
                assertEquals(Integer.parseInt(r[11]),metrics.stored());
                models.add(new Model(root.getFileName().toString(),r[0],r[1],r[2],r[3],r[5],Boolean.parseBoolean(r[8]),network,data));
            }
        }
        assertFalse(models.isEmpty());
        // Warm all models first; shuffle each repetition to spread temporal/JIT/GC effects.
        Collections.shuffle(models,new Random(928));for(Model model:models)run(model,10000);
        StringBuilder csv=new StringBuilder("dataset,phase,task,storage,work,seed,solved,repeat,decisions,ns_per_decision\n");
        for(int repeat=0;repeat<3;repeat++) {
            Collections.shuffle(models,new Random(929+repeat));
            for(Model model:models) {
                long start=System.nanoTime();int decisions=run(model,20000);double nanos=(System.nanoTime()-start)/(double)decisions;
                csv.append(String.format(Locale.ROOT,"%s,%s,%s,%s,%s,%s,%s,%d,%d,%.3f%n",model.dataset,model.phase,model.task,
                        model.storage,model.work,model.seed,model.solved,repeat,decisions,nanos));
            }
        }
        Path output=Path.of(System.getProperty("brain.energy.bench.output","target/brain-energy-timing.csv"));
        Files.createDirectories(output.toAbsolutePath().getParent());Files.writeString(output,csv);
        assertTrue(Double.isFinite(blackhole));System.out.println("Timed "+models.size()+" serialized models, three repetitions: "+output);
    }
    private static int run(Model model,int budget) {
        int count=0;double checksum=0;
        while(count<budget) for(var episode:model.data.episodes()) {
            model.network.rnnClearPreviousState();
            for(var sample:episode) {
                model.network.setInputs(sample.input());
                checksum+=model.network.getNeuralNet().process(model.network.getNeuronValueFunction(),false)[0];count++;
            }
        }
        blackhole=checksum;return count;
    }
}
