package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class ExecutionPlanWorldTest {
    private void connect(NeuralNetwork net,Synapse edge) throws Exception {
        var m=NeuralNet.class.getDeclaredMethod("addSynapse",Synapse.class);m.setAccessible(true);m.invoke(net.getNeuralNet(),edge);
    }
    @Test void fullWorldTrajectoryAndEnergyMatchWithOptimizationEnabled() throws Exception {
        Random previous=NeuralNetwork.getRandom();
        try {
            NeuralNetwork.setRandom(new Random(661));
            var net=new NeuralNetwork(new ValuesNeuronValueFunctionFactory(),CellFactory.calcInputCount(12),new int[]{4,4,4,5},CellFactory.calcOutputCount(12),0,3);
            var hidden=net.getHiddenLayerArr()[3];
            connect(net,new Synapse(net.getInputNeuronArr()[0],0,hidden.getNeuron(1),0,.5));
            connect(net,new Synapse(hidden.getNeuron(1),0,net.getOutputNeuronArr()[0],0,.5));
            connect(net,new Synapse(hidden.getNeuron(2),0,hidden.getNeuron(3),0,.5));
            connect(net,new Synapse(hidden.getNeuron(3),0,hidden.getNeuron(2),0,.5));
            Environment original=new Environment(300,300,new Random(17)),planned=new Environment(300,300,new Random(17));
            for(int i=0;i<8;i++)for(Environment world:new Environment[]{original,planned}) {
                // Reference: object path without plan; planned: plan and compiled forward pass (where eligible).
                var brain=new NeuralNetwork(net);brain.getNeuralNet().setExecutionOptimizationEnabled(world==planned);brain.getNeuralNet().setCompiledExecutionEnabled(world==planned);
                Cell cell=new Cell(new Vector2D(80+(i%4)*22,80+(i/4)*22),10,new CellBrain(brain));
                cell.setType(new CellType(0,1,0));cell.setEnergy(.8);cell.setEnergy(EnergyType.SECONDARY,.2);cell.setCellState(i);world.addCell(cell);
            }
            SpatialGridPartitioningStrategy aGrid=new SpatialGridPartitioningStrategy(300,300,60),bGrid=new SpatialGridPartitioningStrategy(300,300,60);
            SimulationStepPolicy policy=new SimulationStepPolicy() {
                @Override public boolean useSunlight(){return false;}
                @Override public boolean allowReproduction(Cell cell){return false;}
                @Override public BrainEnergyCosts brainEnergyCosts(){return BrainEnergyCosts.DEFAULT;}
            };
            for(int step=0;step<1000;step++) {
                if(step==300 || step==700)for(Environment world:new Environment[]{original,planned})
                    for(int i=0;i<world.getCellList().size();i++)world.getCellList().get(i).setCellState((i+step)%8);
                aGrid.build(original.getCellList());bGrid.build(planned.getCellList());
                original.update(1.0/60,aGrid,policy);planned.update(1.0/60,bGrid,policy);
                assertEquals(original.getCellList().size(),planned.getCellList().size());
                assertFalse(original.getCellList().isEmpty());
                for(int i=0;i<original.getCellList().size();i++) {
                    Cell a=original.getCellList().get(i),b=planned.getCellList().get(i);
                    assertEquals(a.getEnergy(),b.getEnergy(),"energy step="+step);
                    assertFalse(a.isBrainPaused());assertFalse(b.isBrainPaused());
                    assertEquals(a.getEnergy(EnergyType.SECONDARY),b.getEnergy(EnergyType.SECONDARY));
                    assertEquals(a.getPosition().getX(),b.getPosition().getX());assertEquals(a.getPosition().getY(),b.getPosition().getY());
                    assertEquals(a.getVelocity().getX(),b.getVelocity().getX());assertEquals(a.getVelocity().getY(),b.getVelocity().getY());
                    assertEquals(a.getRotation(),b.getRotation());assertEquals(a.getAngularVelocity(),b.getAngularVelocity());
                    assertEquals(a.getBrain().getSynapseCount(),b.getBrain().getSynapseCount());
                    assertEquals(a.getBrain().getProccessedSynapses(),b.getBrain().getProccessedSynapses());
                }
            }
        } finally {NeuralNetwork.setRandom(previous);}
    }
}
