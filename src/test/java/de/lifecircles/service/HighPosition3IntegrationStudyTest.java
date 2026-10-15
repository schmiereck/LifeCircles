package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.trainStrategy.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Full application update path. An imposed known-good program is a control, not a learning result. */
@EnabledIfSystemProperty(named="high3.study",matches="true")
class HighPosition3IntegrationStudyTest {
    @Test void randomNetworksExecuteSelectionAndMutation() throws Exception {
        var config=SimulationConfig.getInstance();
        double width=config.getWidth(),height=config.getHeight(),scale=config.getScaleSimulation();
        Random previous=NeuralNetwork.getRandom();
        Path output=Path.of(System.getProperty("high3.random.output","target/high-position3-random"));Files.createDirectories(output);
        int generations=Integer.getInteger("high3.generations",5);
        try {
            NeuralNetwork.setRandom(new Random(574));
            var strategy=new HighPosition3TrainStrategy();var world=strategy.initializeEnvironment();
            var service=new CalculationService(world,strategy,new SpatialGridPartitioningStrategy(world.getWidth(),world.getHeight(),80));
            List<Cell> initial=new ArrayList<>();for(int i=0;i<480;i+=60) initial.add(world.getCellList().get(i));
            FileService.getInstance().saveCellsToFile(output.resolve("initial-genomes.cells").toString(),initial);
            StringBuilder csv=new StringBuilder("generation,stage,score,late_height,late_support,late_contacts,late_form_R,next_cells\n");
            for(int generation=1;generation<=generations;generation++) {
                for(int step=0;step<1800;step++) {
                    service.update(1.0/60);
                    assertTrue(world.getCellList().size()<=480);
                    if(step%60==0) for(Cell cell:world.getCellList()) {
                        assertTrue(Double.isFinite(cell.getPosition().getX()));assertTrue(Double.isFinite(cell.getPosition().getY()));
                    }
                }
                var result=strategy.getLastEvaluation();assertTrue(Double.isFinite(result.score()));
                csv.append(String.format(Locale.ROOT,"%d,%s,%.9f,%.9f,%.9f,%.9f,%.9f,%d%n",result.generation(),result.stage(),result.score(),
                        result.height(),result.support(),result.contacts(),result.form(),world.getCellList().size()));
                Files.writeString(output.resolve("generations.csv"),csv);
                List<Cell> leaders=new ArrayList<>();Set<Integer> sections=new HashSet<>();
                for(Cell cell:world.getCellList()) if(sections.add((int)(cell.getPosition().getX()/1400))) leaders.add(cell);
                FileService.getInstance().saveCellsToFile(output.resolve("next-genomes.cells").toString(),leaders);
            }
        } finally {
            config.setWidth(width);config.setHeight(height);config.setScaleSimulation(scale);NeuralNetwork.setRandom(previous);
        }
    }

    @Test void knownStableProgramPassesCurriculumAndBuildRemainsBounded() throws Exception {
        var config=SimulationConfig.getInstance();
        double width=config.getWidth(),height=config.getHeight(),scale=config.getScaleSimulation(),
                rate=config.getMutationRate(),strength=config.getMutationStrength(),age=config.getReproductionAgeThreshold();
        Random oldRandom=NeuralNetwork.getRandom();
        try {
            config.setMutationRate(0);config.setMutationStrength(0);config.setReproductionAgeThreshold(6);
            NeuralNetwork.setRandom(new Random(573));
            var strategy=new HighPosition3TrainStrategy();Environment world=strategy.initializeEnvironment();
            var service=new CalculationService(world,strategy,new SpatialGridPartitioningStrategy(world.getWidth(),world.getHeight(),80));
            var network=new NeuralNetwork(new ValuesNeuronValueFunctionFactory(),CellFactory.calcInputCount(12),new int[0],CellFactory.calcOutputCount(12),0,0);
            var outputs=network.getNeuralNet().getOutputNeuronArr();
            for(var neuron:outputs) neuron.setBias(0,-12);
            outputs[GlobalOutputFeature.SIZE.ordinal()].setBias(0,12);
            for(int a=0;a<12;a++) {
                int b=GlobalOutputFeature.values().length+a*ActorOutputFeature.values().length;
                outputs[b+ActorOutputFeature.TYPE_GREEN.ordinal()].setBias(0,12);
                outputs[b+ActorOutputFeature.REPRODUCTION_ENERGY_SHARE.ordinal()].setBias(0,0);
                if(a==9) outputs[b+ActorOutputFeature.REPRODUCTION_DESIRE.ordinal()].setBias(0,12);
            }
            Cell donor=new Cell(new Vector2D(700,1130),20,new CellBrain(network));donor.setType(new CellType(0,1,0));
            service.importCells(List.of(donor),false);
            assertEquals(480,service.getLatestState().getCells().size());
            StringBuilder csv=new StringBuilder("episode,stage_before,stage_after,max_cells,next_cells,score,late_height,late_contacts,late_form_R\n");
            double dt=Double.parseDouble(System.getProperty("high3.dt","0.016666666666666666"));
            int steps=(int)Math.round(30/dt);
            for(int episode=1;episode<=10;episode++) {
                var before=strategy.getStage();int max=0;
                for(int step=0;step<steps;step++) {
                    service.update(dt);max=Math.max(max,world.getCellList().size());
                    assertTrue(world.getCellList().size()<=480);
                }
                var result=strategy.getLastEvaluation();
                csv.append(String.format(Locale.ROOT,"%d,%s,%s,%d,%d,%.9f,%.9f,%.9f,%.9f%n",episode,before,strategy.getStage(),max,
                        world.getCellList().size(),result.score(),result.height(),result.contacts(),result.form()));
                if(episode==3) assertEquals(HighPosition3TrainStrategy.Stage.RECOVER,strategy.getStage());
                if(episode==6) assertEquals(HighPosition3TrainStrategy.Stage.SIZE,strategy.getStage());
                if(episode==9) {assertEquals(HighPosition3TrainStrategy.Stage.BUILD,strategy.getStage());assertEquals(40,world.getCellList().size());}
                if(episode==10) assertTrue(max>40,"Build must actually execute biological reproduction");
                System.out.print(csv.toString().lines().reduce((a,b)->b).orElseThrow()+"\n");
            }
            Path out=Path.of(System.getProperty("high3.output","target/high-position3-control.csv"));
            Files.createDirectories(out.toAbsolutePath().getParent());Files.writeString(out,csv);
        } finally {
            config.setWidth(width);config.setHeight(height);config.setScaleSimulation(scale);
            config.setMutationRate(rate);config.setMutationStrength(strength);config.setReproductionAgeThreshold(age);
            NeuralNetwork.setRandom(oldRandom);
        }
    }
}
