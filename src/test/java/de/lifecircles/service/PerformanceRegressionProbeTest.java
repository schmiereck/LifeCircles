package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.NeuralNetwork;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import de.lifecircles.service.trainStrategy.DefaultTrainStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.io.*;
import java.security.*;
import java.util.*;

/** Short, opt-in diagnostic of a saved population; never writes the source archive. */
@EnabledIfSystemProperty(named="performance.file", matches=".+")
class PerformanceRegressionProbeTest {
    @Test void profileSavedPopulation() throws Exception {
        var config=SimulationConfig.getInstance();
        var strategy=new DefaultTrainStrategy();
        strategy.initializeEnvironment(); // Same dimensions/settings as GUI NONE.
        var world=new Environment(config.getWidth(),config.getHeight(),new Random(713));
        strategy.initialize(world);
        var cells=FileService.getInstance().loadCellsFromFile(System.getProperty("performance.file"));
        world.resetCells(cells);
        NeuralNetwork.setRandom(new Random(421));
        var birthRandom=new Random(835);
        var policy=new SimulationStepPolicy() {
            public boolean keepRemains(){return true;}
            public Cell reproduce(Environment environment,Cell parent) {
                return ReproductionManagerService.reproduce(config,environment,parent,birthRandom);
            }
        };
        var digest=MessageDigest.getInstance("SHA-256");
        var state=new DataOutputStream(new DigestOutputStream(OutputStream.nullOutputStream(),digest));
        System.out.printf("PROBE cells=%d remains=%d%n",cells.size(),cells.stream().filter(Cell::isRemains).count());
        long start=System.nanoTime(); long stored=0;
        for(var cell:cells) stored+=cell.getBrain().getSynapseCount();
        System.out.printf("PROBE stored=%d census_ms=%.3f%n",stored,(System.nanoTime()-start)/1e6);
        for(int pass=0;pass<3;pass++) {
            start=System.nanoTime(); long maximum=0;
            for(var cell:cells)maximum+=cell.getBrain().getMaximumProcessedSynapses();
            System.out.printf("PROBE max=%d bound_ms=%.3f%n",maximum,(System.nanoTime()-start)/1e6);
        }
        var grid=new SpatialGridPartitioningStrategy(world.getWidth(),world.getHeight(),
                2*(config.getCellMaxRadiusSize()+config.getCellActorMaxFieldRadius()));
        int warm=Integer.getInteger("performance.warm",10),steps=Integer.getInteger("performance.steps",30);
        world.getProfiler().setEnabled(true);
        for(int i=-warm;i<steps;i++) {
            if(i==0)world.getProfiler().reset();
            start=world.getProfiler().start();grid.build(world.getCellList());
            world.getProfiler().finish(SimulationProfiler.Phase.GRID,start);
            world.update(config.getCalcTimeStep(),grid,policy);
            state.writeInt(world.getCellList().size());
            for(var cell:world.getCellList()) {
                state.writeDouble(cell.getPosition().getX());state.writeDouble(cell.getPosition().getY());
                state.writeDouble(cell.getVelocity().getX());state.writeDouble(cell.getVelocity().getY());
                state.writeDouble(cell.getRotation());state.writeDouble(cell.getAngularVelocity());
                state.writeDouble(cell.getRadiusSize());state.writeDouble(cell.getAge());state.writeInt(cell.getCellState());
                state.writeBoolean(cell.isBrainPaused());state.writeBoolean(cell.isRemains());
                for(var energy:EnergyType.ALL)state.writeDouble(cell.getEnergy(energy));
                state.writeLong(cell.getBrain().getProccessedSynapses());
                for(var actor:cell.getSensorActors()) {
                    state.writeDouble(actor.getForceStrength());state.writeDouble(actor.getEnergyAbsorption());
                    state.writeDouble(actor.getEnergyDelivery());state.writeDouble(actor.getReproductionDesire());
                }
            }
        }
        world.getProfiler().snapshot().forEach((phase,t)->System.out.printf("PROBE %s %.3f ms/step%n",phase,t.nanos()/1e6/steps));
        System.out.printf("PROBE final_cells=%d paused=%d%n",world.getCellList().size(),world.getCellList().stream().filter(Cell::isBrainPaused).count());
        System.out.println("PROBE trajectory_sha256="+HexFormat.of().formatHex(digest.digest()));
    }
}
