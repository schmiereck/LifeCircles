package de.lifecircles.model.neural;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="brain.energy.study",matches="true")
class BrainEnergyCalibrationTest {
    @Test void calibrate() throws Exception {
        Path output=Path.of(System.getProperty("brain.energy.output","target/brain-energy-calibration"));Files.createDirectories(output);
        int generations=Integer.getInteger("brain.energy.generations",1200),population=Integer.getInteger("brain.energy.population",32),
                seeds=Integer.getInteger("brain.energy.seeds",10),offset=Integer.getInteger("brain.energy.seed_offset",0);
        String tasks=System.getProperty("brain.energy.tasks","CONSTANT,AND,OR,XOR,DELAY,COUNTER");
        String phase=System.getProperty("brain.energy.phase","acquire");
        double reward=Double.parseDouble(System.getProperty("brain.energy.reward","0.003"));
        String[] storage=System.getProperty("brain.energy.storage","0,1,100,1000").split(","),
                work=System.getProperty("brain.energy.work","0,0.25,1,4").split(",");
        assertTrue(Set.of("acquire","compress").contains(phase));assertTrue(population>=4 && generations>0 && seeds>0);
        Files.writeString(output.resolve("settings.txt"),"phase="+phase+"\ngenerations="+generations+"\npopulation="+population+"\nseeds="+seeds+"\nseed_offset="+offset+"\ntasks="+tasks+"\nreward_scale="+reward+"\nstorage="+Arrays.toString(storage)+"\nwork="+Arrays.toString(work)+"\nfrequency=60\nmutation=0.1,0.2 plus seeded factors\nDynamic scheduling and module mutation enabled. No backpropagation/crossover.\nLogic: reset per independent example. Memory: reset per sequence.\nAcquisition starts random; compression starts with explicit working control plus 24 neutral connections in a separate layer.\nGuards: reject stored edges>4096 or root neurons>128, invalid outputs.\nTiming includes reset and evaluation bookkeeping; one JVM, three warmed short repetitions.\n");
        String header="phase,task,storage_factor,work_factor,reward_scale,seed,generations,first_solved,solved,loss,accuracy,stored,executed,root_neurons,root_activations,energy_per_second,net_gain,ns_per_decision,rejected_evaluations,wall_seconds\n";
        Files.writeString(output.resolve("results.csv"),header);
        Files.writeString(output.resolve("trajectory.csv"),"phase,task,storage_factor,work_factor,seed,generation,first_solved,loss,accuracy,stored,executed\n");
        Random previous=NeuralNetwork.getRandom();
        try {
            for(String taskName:tasks.split(",")) for(String sf:storage) for(String wf:work) for(int trial=0;trial<seeds;trial++) {
                var task=BrainEnergyLab.Task.valueOf(taskName);int seed=offset+trial;
                var price=new BrainEnergyLab.Price(Double.parseDouble(sf),Double.parseDouble(wf),reward);
                String label=phase+","+task+","+sf+","+wf+","+seed;
                StringBuilder trace=new StringBuilder();long start=System.nanoTime();
                var result=BrainEnergyLab.evolve(task,price,seed,generations,population,
                        phase.equals("compress")?BrainEnergyControls.known(task,true):null,(generation,m,first)->{
                            trace.append(String.format(Locale.ROOT,"%s,%d,%d,%.9f,%.9f,%d,%.9f%n",label,generation,first,m.loss(),m.accuracy(),m.stored(),m.executed()));
                        });
                var m=result.metrics();assertTrue(Double.isFinite(m.loss()));
                var validation=BrainEnergyLab.data(task,BrainEnergyLab.seed(seed,100000,task.ordinal()),true);
                String line=String.format(Locale.ROOT,"%s,%s,%s,%s,%.9f,%d,%d,%d,%s,%.9f,%.9f,%d,%.9f,%d,%.9f,%.12f,%.12f,%.3f,%d,%.3f%n",
                        phase,task,sf,wf,reward,seed,generations,result.firstSolved(),m.solved(task),m.loss(),m.accuracy(),m.stored(),m.executed(),m.neurons(),m.activations(),
                        price.cost(m.stored(),m.executed()),price.utility(validation,m),result.nanos(),result.rejected(),(System.nanoTime()-start)/1e9);
                Files.writeString(output.resolve("results.csv"),line,StandardOpenOption.APPEND);
                Files.writeString(output.resolve("trajectory.csv"),trace,StandardOpenOption.APPEND);
                Path model=output.resolve(phase+"-"+task+"-s"+sf+"-w"+wf+"-seed"+seed+".nn");
                try(var stream=new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(model)))) {stream.writeObject(result.champion().network());}
                System.out.print(line);
            }
        } finally {NeuralNetwork.setRandom(previous);}
    }
}
