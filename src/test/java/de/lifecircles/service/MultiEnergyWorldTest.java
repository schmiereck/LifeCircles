package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MultiEnergyWorldTest {
    private static class SharingBrain extends PhysicsRegressionTest.ConstantBrain {
        @Override public double[] process() {
            double[] outputs=super.process();
            outputs[GlobalOutputFeature.values().length+ActorOutputFeature.ENERGY_DELIVERY.ordinal()]=.04;
            return outputs;
        }
    }
    @Test void realThreeCellWorldConservesSecondaryWhileOnlyPrimaryPaysMetabolism() {
        Environment world=new Environment(300,300);
        for(int i=0;i<3;i++) {
            Cell c=new Cell(new Vector2D(80+22*i,100),10,new SharingBrain());c.setEnergy(.8);
            c.setEnergy(EnergyType.SECONDARY,new double[]{.9,.2,.1}[i]);world.addCell(c);
        }
        SimulationStepPolicy policy=new SimulationStepPolicy() {
            @Override public boolean useSunlight(){return false;}
            @Override public boolean allowReproduction(Cell cell){return false;}
        };
        var grid=new SpatialGridPartitioningStrategy(300,300,60);
        for(int step=1;step<=1000;step++) {
            grid.build(world.getCellList());world.update(1.0/60,grid,policy);assertEquals(3,world.getCellList().size());
            assertEquals(2.4-3*SimulationConfig.ENERGY_DECAY_RATE*step/60,
                    world.getCellList().stream().mapToDouble(Cell::getEnergy).sum(),1e-11);
            assertEquals(1.2,world.getCellList().stream().mapToDouble(c->c.getEnergy(EnergyType.SECONDARY)).sum(),1e-11);
        }
        assertTrue(world.getCellList().getFirst().getEnergy(EnergyType.SECONDARY)<.9);
        assertTrue(world.getCellList().getLast().getEnergy(EnergyType.SECONDARY)>.1);
    }
}
