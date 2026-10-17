package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.EnergyInputLayout;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ParallelBrainFundingTest {
    @Test void allStorageDebitsPrecedeEveryInputAndAllInputsPrecedeEveryDecision() {
        var world=new Environment(6400,300);
        final double dt=.1;
        class ObservingBrain extends PhysicsRegressionTest.ConstantBrain {
            final int index;
            boolean captured;
            ObservingBrain(int index){this.index=index;}
            public int getSynapseCount(){return 10_000+index;}
            public long getMaximumProcessedSynapses(){return 7;}
            public long getProccessedSynapses(){return 3;}
            double afterStorage(){return index%3==0?0:.2-BrainEnergyCosts.DEFAULT.storageCost(getSynapseCount(),dt);}
            public int getInputCount() {
                // A decision must see even the last worker's storage debit, and
                // must not see any earlier worker's decision-work debit yet.
                for(Cell cell:world.getCellList())
                    assertEquals(((ObservingBrain)cell.getBrain()).afterStorage(),cell.getEnergy(EnergyType.SECONDARY));
                captured=true;
                return super.getInputCount();
            }
            public void setInputs(double[] inputs) {
                assertEquals(afterStorage(),inputs[EnergyInputLayout.ownEnergy(12,EnergyType.SECONDARY)]);
            }
            public double[] process() {
                for(Cell cell:world.getCellList()) {
                    var brain=(ObservingBrain)cell.getBrain();
                    if(brain.index%3!=0)assertTrue(brain.captured,"input barrier");
                }
                return super.process();
            }
        }
        for(int i=0;i<64;i++) {
            var cell=new Cell(new Vector2D(50+i*90,100),10,new ObservingBrain(i));
            cell.setEnergy(EnergyType.SECONDARY,i%3==0?0:.2);world.addCell(cell);
        }
        var grid=new SpatialGridPartitioningStrategy(6400,300,60);grid.build(world.getCellList());
        world.update(dt,grid,new SimulationStepPolicy() {
            public boolean useSunlight(){return false;}
            public boolean allowReproduction(Cell cell){return false;}
            public BrainEnergyCosts brainEnergyCosts(){return BrainEnergyCosts.DEFAULT;}
            public boolean useBasalBrainRespiration(){return false;}
        });
        for(Cell cell:world.getCellList()) {
            var brain=(ObservingBrain)cell.getBrain();
            assertEquals(brain.index%3==0,cell.isBrainPaused());
            assertEquals(brain.index%3!=0,brain.captured);
            assertEquals(brain.afterStorage()-(brain.captured?BrainEnergyCosts.DEFAULT.workCost(3):0),cell.getEnergy(EnergyType.SECONDARY));
        }
    }

    @Test void freeBrainsAndUnpaidStorageAvoidUnnecessaryNetworkWalks() {
        class CensusBrain extends PhysicsRegressionTest.ConstantBrain {
            int storageCalls,workCalls;
            public int getSynapseCount(){storageCalls++;return 100;}
            public long getMaximumProcessedSynapses(){workCalls++;return 10;}
        }
        var brain=new CensusBrain();var cell=new Cell(new Vector2D(50,50),10,brain);
        assertTrue(BrainEnergyService.prepare(cell,.1,BrainEnergyCosts.FREE));
        assertEquals(0,brain.storageCalls);assertEquals(0,brain.workCalls);
        assertFalse(BrainEnergyService.prepare(cell,.1,BrainEnergyCosts.DEFAULT,false));
        assertEquals(1,brain.storageCalls);assertEquals(0,brain.workCalls);
        cell.setEnergy(EnergyType.SECONDARY,.2);
        assertTrue(BrainEnergyService.prepare(cell,.1,BrainEnergyCosts.DEFAULT,false));
        assertEquals(2,brain.storageCalls);assertEquals(1,brain.workCalls);
    }
}
