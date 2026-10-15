package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Read-only file audit, followed by a local HOLD continuation without selection/mutation. */
@EnabledIfSystemProperty(named="brain.audit.file",matches=".+")
class BrainEnergyAuditTest {
    @Test void measureSavedHighPosition3Brains() throws Exception {
        Path source=Path.of(System.getProperty("brain.audit.file"));
        var cells=FileService.getInstance().loadCellsFromFile(source.toString());
        Environment world=new Environment(11200,1200);world.addGroundBlocker();world.resetCells(cells);
        var policy=new SimulationStepPolicy() {
            public boolean useSunlight(){return false;}
            public boolean isDead(Cell cell){return false;}
            public boolean allowReproduction(Cell cell){return false;}
            public void afterThinking(Cell cell){
                cell.setRadiusSize(20);
                for(var actor:cell.getSensorActors()) {
                    actor.setType(GREEN);actor.setEnergyDelivery(0);actor.setEnergyAbsorption(0);actor.setReproductionDesire(0);
                }
            }
            private final CellType GREEN=new CellType(0,1,0);
        };
        int steps=Integer.getInteger("brain.audit.steps",1200);
        assertTrue(steps>0);
        double[] executed=new double[cells.size()];
        var grid=new SpatialGridPartitioningStrategy(11200,1200,
                2*(SimulationConfig.getInstance().getCellMaxRadiusSize()+SimulationConfig.getInstance().getCellActorMaxFieldRadius()));
        world.getProfiler().setEnabled(true);
        for(int step=0;step<steps;step++) {
            if(step==120) world.getProfiler().reset(); // discard initial JIT/sensing warm-up from timing
            for(Cell cell:cells) {cell.setEnergy(.75);policy.afterThinking(cell);}
            grid.build(cells);world.update(1.0/60,grid,policy);
            for(int i=0;i<cells.size();i++) {
                executed[i]+=cells.get(i).getBrain().getProccessedSynapses();
                assertTrue(Double.isFinite(cells.get(i).getPosition().getX()));
                assertTrue(Double.isFinite(cells.get(i).getPosition().getY()));
            }
        }
        StringBuilder csv=new StringBuilder("cell,section,root_neurons,root_layers,stored_synapses,mean_executed_synapses_per_think,brain_energy_per_second_at_60Hz,brain_to_basal_ratio\n");
        for(int i=0;i<cells.size();i++) {
            Cell cell=cells.get(i);var brain=(CellBrain)cell.getBrain();var net=brain.getNeuralNetwork().getNeuralNet();
            double mean=executed[i]/steps;
            double energy=brain.getSynapseCount()*SimulationConfig.ENERGY_COST_PER_SYNAPSE
                    +60*mean*SimulationConfig.ENERGY_COST_PER_PROCESSED_SYNAPSE;
            csv.append(String.format(Locale.ROOT,"%d,%d,%d,%d,%d,%.9f,%.9f,%.9f%n",i,
                    (int)(cell.getPosition().getX()/1400),net.getAllNeuronsSize(),net.getHiddenLayerArr().length,
                    brain.getSynapseCount(),mean,energy,energy/SimulationConfig.ENERGY_DECAY_RATE));
        }
        Path out=Path.of(System.getProperty("brain.audit.output","target/brain-energy-audit"));Files.createDirectories(out);
        Files.writeString(out.resolve("cells.csv"),csv);
        StringBuilder timings=new StringBuilder("phase,calls,ms_per_update\n");
        long measured=steps>120?steps-120:steps;
        world.getProfiler().snapshot().forEach((phase,timing)->timings.append(String.format(Locale.ROOT,"%s,%d,%.9f%n",phase,timing.calls(),timing.nanos()/1e6/measured)));
        Files.writeString(out.resolve("timings.csv"),timings);
        Files.writeString(out.resolve("settings.txt"),"source="+source+"\nsteps="+steps+"\nHOLD continuation, no selection/mutation; not a replay of the original run.\nTransient executed counters are measured anew, not read from the file.\nRoot neuron/layer counts exclude module interiors. Timing excludes grid, audit bookkeeping and snapshots; one short probe.\n");
        System.out.println("Brain audit: "+cells.size()+" cells; results in "+out);
    }
}
