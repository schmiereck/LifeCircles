package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="function.study.folder",matches=".+")
class FunctionComparisonStudyTest {
    @Test void compareLiveAndFixedCommands() throws Exception {
        Path root=Path.of(System.getProperty("function.study.folder"));
        Path out=Path.of(System.getProperty("function.study.output","target/function-comparison"));Files.createDirectories(out);
        int steps=Integer.getInteger("function.study.steps",7200),calibration=Integer.getInteger("function.study.calibration",7200);
        int repeats=Integer.getInteger("function.study.repeats",2),limit=Integer.getInteger("function.study.models",18);
        assertTrue(steps>0 && calibration>0 && repeats>0 && limit>0);
        String header="model,origin_seed,storage,work,snapshot_step,source_index,source_state,stored,fee,mode,sun_seed,steps,status,cell_seconds,energy_seconds,births,deaths,end_cells,end_energy,mean_executed,command_deviation,wall_seconds\n";
        Files.writeString(out.resolve("results.csv"),header);
        Files.writeString(out.resolve("progress.csv"),"model,fee,mode,sun_seed,step,cells,births,deaths,total_energy\n");
        Files.writeString(out.resolve("profiles.csv"),"model,state,samples,rms_variation,output,value\n");
        Files.writeString(out.resolve("settings.txt"),"source="+root+"\nsteps="+steps+"\ncalibration_steps="+calibration+"\nrepeats="+repeats
                +"\nmodels_limit="+limit+"\nworld=600x400 ground only\nfounders=8\nfounder_energy=.75\nfounder_age=9\nfounder_radius=10\nfounder_state=source_state\nfounder_angles=0,45,90,135,180,225,270,315 degrees"
                +"\nsource_snapshots=60000,600000\nrepresentative=median stored-synapse count; original file index breaks ties\ncalibration_sun_seed=991000"
                +"\ntrial_sun_seeds=992000+repeat\nfees=source-price,zero\nfreeze=calibration mean per birth state; unseen states use 64 canonical decisions"
                +"\nbrain executes in both modes; no cost refund, no mutation, no refill/reseeding. Newborn commands overridden before insertion."
                +"\nPrimary readouts: cell-seconds and births over fixed duration. Free-fee trials isolate control benefit from brain fees."
                +"\nCalibration uses source price; fixed profiles are examples of static policies, not optimized static controls.\n");
        Random original=NeuralNetwork.getRandom();int model=0;
        try {
            List<Path> folders;
            try(var stream=Files.list(root)){folders=stream.filter(p->p.getFileName().toString().startsWith("seed-")).sorted().toList();}
            outer:for(Path folder:folders)for(int snapshot:new int[]{60000,600000}) {
                if(model>=limit)break outer;
                String[] parts=folder.getFileName().toString().split("-");
                String origin=parts[1];double storage=Double.parseDouble(parts[2].substring(1)),work=Double.parseDouble(parts[3].substring(1));
                var price=new BrainEnergyCosts(storage,work);
                List<Cell> population=NoneEnergyStudyTest.load(Files.readAllBytes(folder.resolve("step-"+snapshot+".cells")));
                List<Integer> indices=new ArrayList<>();for(int i=0;i<population.size();i++)indices.add(i);
                indices.sort(Comparator.comparingInt((Integer i)->population.get(i).getBrain().getSynapseCount()).thenComparingInt(i->i));
                int index=indices.get(indices.size()/2);Cell genome=population.get(index);int id=model++;
                NoneEnergyStudyTest.save(out.resolve("model-"+id+".cells"),List.of(genome));
                NeuralNetwork.setRandom(new Random(990000+id));
                var profiles=FunctionComparisonLab.calibrate(genome,price,calibration);
                StringBuilder profileCsv=new StringBuilder();
                for(int state=0;state<8;state++)for(int j=0;j<FunctionComparisonLab.OUTPUTS;j++)
                    profileCsv.append(String.format(Locale.ROOT,"%d,%d,%d,%.12g,%d,%.12g%n",id,state,profiles.counts()[state],profiles.rms()[state],j,profiles.mean()[state][j]));
                Files.writeString(out.resolve("profiles.csv"),profileCsv,StandardOpenOption.APPEND);
                for(String fee:new String[]{"source","zero"})for(int repeat=0;repeat<repeats;repeat++)for(int order=0;order<2;order++) {
                    boolean frozen=((order+repeat+id)%2)==1;String mode=frozen?"frozen":"live";long seed=992000+repeat;
                    NeuralNetwork.setRandom(new Random(993000+id));long begin=System.nanoTime();
                    var result=FunctionComparisonLab.run(genome,fee.equals("zero")?new BrainEnergyCosts(0,0):price,
                            frozen?profiles.mean():null,seed,steps,(step,world,policy)->{
                                try{Files.writeString(out.resolve("progress.csv"),String.format(Locale.ROOT,"%d,%s,%s,%d,%d,%d,%d,%d,%.9f%n",
                                        id,fee,mode,seed,step,world.getCellList().size(),policy.births,policy.deaths,FunctionComparisonLab.energy(world)),StandardOpenOption.APPEND);}
                                catch(Exception e){throw new RuntimeException(e);}
                            });
                    String row=String.format(Locale.ROOT,"%d,%s,%.1f,%.1f,%d,%d,%d,%d,%s,%s,%d,%d,%s,%.9f,%.9f,%d,%d,%d,%.9f,%.6f,%.9f,%.6f%n",
                            id,origin,storage,work,snapshot,index,genome.getCellState(),genome.getBrain().getSynapseCount(),fee,mode,seed,result.steps(),result.status(),
                            result.cellSeconds(),result.energySeconds(),result.births(),result.deaths(),result.cells(),result.energy(),result.meanExecuted(),result.commandDeviation(),(System.nanoTime()-begin)/1e9);
                    Files.writeString(out.resolve("results.csv"),row,StandardOpenOption.APPEND);
                    System.out.printf(Locale.ROOT,"FUNCTION model %d/18 %s %s seed %d: cells=%d births=%d status=%s%n",id+1,fee,mode,seed,result.cells(),result.births(),result.status());
                }
            }
            assertTrue(model>0);
        } finally {NeuralNetwork.setRandom(original);}
    }
}
