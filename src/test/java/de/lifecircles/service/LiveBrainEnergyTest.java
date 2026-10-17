package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import de.lifecircles.view.CellPanel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LiveBrainEnergyTest {
    @Test void sliderScaleCoversZeroStandardAndLargeMultipliersContinuously() {
        for(double multiplier:new double[]{0,.01,.1,1,10,100,1000})
            assertEquals(multiplier,CellPanel.multiplier(CellPanel.toPosition(multiplier)),1e-10);
    }
    @Test void defaultWorldUsesLivePriceButExplicitExperimentPriceTakesPrecedence() {
        var config=SimulationConfig.getInstance();double previous=config.getBrainEnergyCosts().work(),sun=config.getSunRayRate();
        try {
            config.setSunRayRate(0);
            var net=new NeuralNetwork(new ValuesNeuronValueFunctionFactory(),CellFactory.calcInputCount(12),new int[]{2},CellFactory.calcOutputCount(12),.1,0);
            Cell cell=new Cell(new Vector2D(100,100),10,new CellBrain(net));
            Environment world=new Environment(300,300);world.addCell(cell);
            var grid=new SpatialGridPartitioningStrategy(300,300,60);
            for(double multiplier:new double[]{0,1,100,1000}) {
                config.setBrainEnergyMultiplier(multiplier);cell.setEnergy(.8);cell.setEnergy(EnergyType.SECONDARY,.2);
                grid.build(world.getCellList());world.update(1.0/60,grid);
                double cost=new BrainEnergyCosts(multiplier,multiplier).cost(cell.getBrain().getSynapseCount(),cell.getBrain().getProccessedSynapses(),1.0/60);
                assertFalse(cell.isBrainPaused());
                assertEquals(.8-SimulationConfig.ENERGY_DECAY_RATE/60,cell.getEnergy(),1e-12);
                assertEquals(.2-cost,cell.getEnergy(EnergyType.SECONDARY),1e-12);
            }
            cell.setEnergy(.8);grid.build(world.getCellList());
            world.update(1.0/60,grid,new SimulationStepPolicy(){@Override public BrainEnergyCosts brainEnergyCosts(){return new BrainEnergyCosts(0,0);}});
            assertEquals(.8-SimulationConfig.ENERGY_DECAY_RATE/60,cell.getEnergy(),1e-12);
            assertThrows(IllegalArgumentException.class,()->config.setBrainEnergyMultiplier(Double.NaN));
            assertThrows(IllegalArgumentException.class,()->config.setBrainEnergyMultiplier(-1));
        } finally {config.setBrainEnergyMultiplier(previous);config.setSunRayRate(sun);}
    }
}
