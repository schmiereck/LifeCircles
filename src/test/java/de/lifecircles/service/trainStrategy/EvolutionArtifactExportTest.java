package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Exports saved experimental populations and optionally performs declared follow-up interventions. */
@EnabledIfSystemProperty(named="evolution.artifacts",matches=".+")
class EvolutionArtifactExportTest {
    private final SimulationConfig config=SimulationConfig.getInstance();

    @Test void exportAndEvaluate() throws Exception {
        Path root=Path.of(System.getProperty("evolution.artifacts"));
        List<Path> snapshots;
        try(var paths=Files.walk(root)) {
            snapshots=paths.filter(p->p.getFileName().toString().matches("(generation-[0-9]+|best)\\.cells")).sorted().toList();
        }
        double oldRate=config.getSunRayRate(),oldAge=config.getReproductionAgeThreshold(),oldEnergy=config.getEnergyPerRay(),
                oldWidth=config.getWidth(),oldHeight=config.getHeight(),oldScale=config.getScaleSimulation();
        long oldDelay=config.getSizeChangeDelay();
        try {
            for(Path snapshot:snapshots) {
                List<Cell> cells=FileService.getInstance().loadCellsFromFile(snapshot.toString());
                Environment world=new Environment(6400,1200); world.addGroundBlocker(); world.resetCells(cells);
                export(snapshot.resolveSibling(snapshot.getFileName()+".csv"),world);
                if(!Boolean.getBoolean("evolution.followup") || !snapshot.getFileName().toString().equals("best.cells")) continue;
                for(String mode:List.of("no_division","frozen_mechanics")) {
                    // Reload before each intervention; the saved population is never overwritten.
                    var strategy=new HighPosition2TrainStrategy(); world=strategy.initializeEnvironment(); strategy.initialize(world);
                    world.resetCells(FileService.getInstance().loadCellsFromFile(snapshot.toString()));
                    config.setSunRayRate(snapshot.toString().contains("standard")?10:20);
                    int generation=Integer.parseInt(Files.readString(snapshot.resolveSibling("best.txt")).split(",")[1]);
                    var sunField=Environment.class.getDeclaredField("energySunCalcService"); sunField.setAccessible(true);
                    Object sun=sunField.get(world);
                    var phase=EnergySunCalcService.class.getDeclaredField("timeInCycle"); phase.setAccessible(true);
                    phase.setDouble(sun,(generation*50.0)%SimulationConfig.SUN_DAY_NIGHT_CYCLE_DURATION);
                    var rng=EnergySunCalcService.class.getDeclaredField("random"); rng.setAccessible(true);
                    ((Random)rng.get(sun)).setSeed(901+generation);
                    config.setReproductionAgeThreshold(Double.MAX_VALUE);
                    config.setSizeChangeDelay(oldDelay);
                    if(mode.equals("frozen_mechanics")) {
                        config.setSunRayRate(0); config.setSizeChangeDelay(Long.MAX_VALUE);
                        var brain=Cell.class.getDeclaredField("brain"); brain.setAccessible(true);
                        for(Cell cell:world.getCellList()) {
                            brain.set(cell,new FrozenBrain(cell)); cell.setIsGrowing(false); cell.setEnergy(1);
                        }
                    }
                    var grid=new SpatialGridPartitioningStrategy(6400,1200,80);
                    StringBuilder csv=new StringBuilder("seconds,cells,supported_height,tracked_supported_height,grounded_fraction,mean_energy,mean_speed\n");
                    var fitness=new GroundedColonyFitness(16);
                    int trackedSection=(int)(fitness.current(world).entrySet().stream()
                            .max(Comparator.comparingDouble(entry->entry.getValue().score())).orElseThrow().getKey().getPosition().getX()/400);
                    export(snapshot.resolveSibling("followup-"+mode+"-initial-positions.csv"),world);
                    int initial=world.getCellList().size();
                    for(int step=0;step<=1800;step++) {
                        if(step%60==0) {
                            var ratings=fitness.current(world); var population=world.getCellList();
                            csv.append(String.format(Locale.ROOT,"%d,%d,%.9f,%.9f,%.9f,%.9f,%.9f%n",step/60,population.size(),
                                    ratings.values().stream().mapToDouble(GroundedColonyFitness.Rating::score).max().orElse(0)*1150,
                                    ratings.entrySet().stream().filter(entry->(int)(entry.getKey().getPosition().getX()/400)==trackedSection)
                                            .mapToDouble(entry->entry.getValue().score()).max().orElse(0)*1150,
                                    ratings.size()/(double)Math.max(1,population.size()),population.stream().mapToDouble(Cell::getEnergy).average().orElse(0),
                                    population.stream().mapToDouble(c->c.getVelocity().length()).average().orElse(0)));
                        }
                        if(step==1800) break;
                        grid.build(world.getCellList()); world.update(1.0/60,grid);
                        assertTrue(world.getCellList().size()<=initial,"Division was not disabled");
                        for(Cell cell:world.getCellList()) assertTrue(Double.isFinite(cell.getPosition().getY()));
                    }
                    Path target=snapshot.resolveSibling("followup-"+mode+".csv"); Files.writeString(target,csv);
                    Files.writeString(snapshot.resolveSibling("followup-"+mode+"-settings.txt"),"Tracked initial section="+trackedSection+"\nSun phase="+generation*50%120+"\n");
                    export(snapshot.resolveSibling("followup-"+mode+"-positions.csv"),world);
                    System.out.printf("FOLLOWUP %s %s%n%s",snapshot,mode,csv);
                }
            }
        } finally {
            config.setSunRayRate(oldRate); config.setReproductionAgeThreshold(oldAge);
            config.setEnergyPerRay(oldEnergy); config.setSizeChangeDelay(oldDelay);
            config.setWidth(oldWidth); config.setHeight(oldHeight); config.setScaleSimulation(oldScale);
        }
    }

    private void export(Path output,Environment world) throws Exception {
        var ratings=new GroundedColonyFitness(16).current(world);
        StringBuilder csv=new StringBuilder("id,x,y,radius,energy,state,generation,red,green,blue,supported_height,base\n");
        for(Cell cell:world.getCellList()) {
            var color=cell.getType(); var rating=ratings.get(cell);
            csv.append(String.format(Locale.ROOT,"%d,%.9f,%.9f,%.9f,%.9f,%d,%d,%.9f,%.9f,%.9f,%.9f,%s%n",
                    cell.getSensingId(),cell.getPosition().getX(),cell.getPosition().getY(),cell.getRadiusSize(),cell.getEnergy(),cell.getCellState(),cell.getGeneration(),
                    color.getRed(),color.getGreen(),color.getBlue(),rating==null?0:rating.score()*1150,rating!=null && rating.base()));
        }
        Files.writeString(output,csv);
    }

    private static class FrozenBrain implements CellBrainInterface {
        final double[] outputs,inputs;
        FrozenBrain(Cell cell) {
            outputs=new double[CellFactory.calcOutputCount(12)]; inputs=new double[cell.getBrain().getInputCount()];
            for(int i=0;i<outputs.length;i++) outputs[i]=cell.getBrain().getOutputValue(i);
            SimulationConfig config=SimulationConfig.getInstance();
            outputs[GlobalOutputFeature.SIZE.ordinal()]=(cell.getRadiusSize()-config.getCellMinRadiusSize())/(config.getCellMaxRadiusSize()-config.getCellMinRadiusSize());
            for(int i=0;i<12;i++) {
                int base=GlobalOutputFeature.values().length+i*ActorOutputFeature.values().length;
                outputs[base+ActorOutputFeature.REPRODUCTION_DESIRE.ordinal()]=0;
                outputs[base+ActorOutputFeature.ENERGY_DELIVERY.ordinal()]=0;
                outputs[base+ActorOutputFeature.ENERGY_ABSORPTION.ordinal()]=0;
            }
        }
        public int getSynapseCount(){return 0;}
        public boolean[] determineActiveHiddenLayers(int state){return new boolean[0];}
        public void setInputs(double[] values){System.arraycopy(values,0,inputs,0,inputs.length);}
        public double[] process(){return outputs;}
        public int getInputCount(){return inputs.length;}
        public long getProccessedSynapses(){return 0;}
        public NeuralNetwork mutate(double rate,double strength){throw new UnsupportedOperationException();}
        public double getOutputValue(int index){return outputs[index];}
        public double getInputValue(int index){return inputs[index];}
    }
}
