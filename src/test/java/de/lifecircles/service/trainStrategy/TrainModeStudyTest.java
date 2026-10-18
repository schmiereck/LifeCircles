package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.NeuralNetwork;
import de.lifecircles.service.*;
import de.lifecircles.service.partitioningStrategy.PartitioningStrategyFactory;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Opt-in run of one training mode with or without adhesion in training worlds (BODENHALT.md). Example:
 * mvn test -Dtest=TrainModeStudyTest -Dtrainmode.study.mode=HIGH_POSITION2 -Dtrainmode.study.variant=full
 * -Dtrainmode.study.seconds=1800 -Dtrainmode.study.seed=4711 -Dtrainmode.study.name=hp2-full-4711
 * Variants: off (as before), adhesion (tonus, hold, recognition), full (adhesion plus bonds, birth bonds,
 * ground field and anchors). Writes target/trainmode-study/NAME.csv (ZoneSeasonStudyTest columns every 30 s
 * plus the mode's own fitness, at 29, 59, ... s), NAME-generations.csv and bond snapshots at 599, 1199, ... s.
 */
@EnabledIfSystemProperty(named="trainmode.study.seconds", matches="\\d+")
class TrainModeStudyTest {
    static final double DT=1.0/60;

    @Test void run() throws Exception {
        TrainMode mode=TrainMode.valueOf(System.getProperty("trainmode.study.mode","HIGH_POSITION2"));
        String variant=System.getProperty("trainmode.study.variant","off");
        int seconds=Integer.getInteger("trainmode.study.seconds");
        long seed=Long.getLong("trainmode.study.seed",4711);
        String name=System.getProperty("trainmode.study.name",mode+"-"+variant+"-"+seed);
        if(!Set.of("off","adhesion","full").contains(variant))throw new IllegalArgumentException(variant);
        var config=SimulationConfig.getInstance();
        boolean full=variant.equals("full");
        config.setCellAdhesion(true);config.setTrainingAdhesion(!variant.equals("off"));
        config.setAdhesionHoldForces(true);config.setAdhesionRecognition(true);
        config.setAdhesionBonds(full);config.setAdhesionBirthBonds(full);
        config.setGroundAdhesion(full);config.setGroundAnchors(full);
        NeuralNetwork.setRandom(new Random(seed));
        Path dir=Paths.get("target","trainmode-study");Files.createDirectories(dir);
        var generations=new PrintWriter(Files.newBufferedWriter(dir.resolve(name+"-generations.csv")));
        long[] steps={0};
        String[] fitness={""};
        TrainStrategy strategy=switch(mode) {
            // HIGH_POSITION2 reports the sustained height of its winners (px above the ground), like HighPosition2EvolutionStudyTest.
            case HIGH_POSITION2 -> { generations.println("t,generation,cells,sustainedHeight");
                int[] generation={0};
                yield new HighPosition2TrainStrategy(new Random(seed+2),(world,colony,winners)->{
                    double floor=world.getHeight()-Environment.GroundBlockerHeight;
                    double sustained=winners.stream().mapToDouble(colony::persistentScore).max().orElse(0)*floor;
                    fitness[0]=String.format(Locale.ROOT,"%.1f",sustained);
                    generations.printf(Locale.ROOT,"%.0f,%d,%d,%.3f%n",steps[0]*DT,++generation[0],world.getCellList().size(),sustained);generations.flush();
                }); }
            case HIGH_POSITION3 -> { generations.println("t,generation,stage,score,height,support,contacts,form"); yield new HighPosition3TrainStrategy(new Random(seed+2)); }
            default -> { generations.println("t,status"); yield CalculationService.strategyFor(mode); }
        };
        Environment world=strategy.initializeEnvironment();strategy.initialize(world);
        double radius=(config.getCellMaxRadiusSize()+config.getCellActorMaxFieldRadius())*2;
        var grid=PartitioningStrategyFactory.createStrategy(world.getWidth(),world.getHeight(),radius);
        var policy=strategy.stepPolicy();
        HighPosition3TrainStrategy.Evaluation lastEvaluation=null;
        long start=System.nanoTime();
        try(var csv=new PrintWriter(Files.newBufferedWriter(dir.resolve(name+".csv")))) {
            csv.println(ZoneSeasonStudyTest.header()+",fitness");
            for(int s=1;s<=seconds;s++) {
                for(int i=0;i<60;i++){strategy.beforeStep(world,DT);grid.build(world.getCellList());world.update(DT,grid,policy);strategy.selectAndMutate(world,DT);steps[0]++;}
                if(strategy instanceof HighPosition3TrainStrategy hp3 && hp3.getLastEvaluation()!=lastEvaluation) {
                    var e=lastEvaluation=hp3.getLastEvaluation();
                    fitness[0]=String.format(Locale.ROOT,"%s %.3f",e.stage(),e.score());
                    generations.printf(Locale.ROOT,"%d,%d,%s,%.4f,%.4f,%.4f,%.4f,%.4f%n",s,e.generation(),e.stage(),e.score(),e.height(),e.support(),e.contacts(),e.form());generations.flush();
                }
                // One second before each 30 s mark: HIGH_POSITION3 restarts its episode exactly on it.
                if((s+1)%30==0) {
                    if(mode!=TrainMode.HIGH_POSITION2&&mode!=TrainMode.HIGH_POSITION3){fitness[0]=strategy.getStatus().replace(',',';');generations.println(s+",\""+fitness[0]+"\"");generations.flush();}
                    String row=ZoneSeasonStudyTest.sample(world,s,1)+",\""+fitness[0]+"\"";
                    csv.println(row);csv.flush();
                    System.out.printf("TRAINSTUDY %s t=%ds wall=%.0fs %s%n",name,s,(System.nanoTime()-start)/1e9,row);
                    if((s+1)%600==0)ZoneSeasonStudyTest.writeSnapshots(world,dir,name,s);
                }
            }
        } finally { generations.close(); }
        FileService.getInstance().saveCellsToFile(dir.resolve(name+".cells").toString(),new ArrayList<>(world.getCellList().stream().filter(c->!c.isRemains()).toList()));
    }
}
