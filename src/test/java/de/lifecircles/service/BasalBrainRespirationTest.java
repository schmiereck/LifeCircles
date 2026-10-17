package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import java.util.Random;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** Innate, slow, lossy 000 -> 001 respiration keeps starved lineages alive; inherited enzymes stay better. */
class BasalBrainRespirationTest {
    static final double EFF=BrainEnergyService.BASAL_EFFICIENCY, RATE=BrainEnergyService.BASAL_MAX_INPUT_PER_SECOND;
    final SimulationConfig config=SimulationConfig.getInstance();
    double sun,price;
    @BeforeEach void save() {sun=config.getSunRayRate();price=config.getBrainEnergyCosts().work();config.setSunRayRate(0);config.setBrainEnergyMultiplier(1);}
    @AfterEach void restore(){config.setSunRayRate(sun);config.setBrainEnergyMultiplier(price);}

    static class FixedBrain extends PhysicsRegressionTest.ConstantBrain {
        final int stored; final long maximum, actual; int decisions;
        FixedBrain(int stored,long maximum,long actual){this.stored=stored;this.maximum=maximum;this.actual=actual;}
        public int getSynapseCount(){return stored;}
        public long getMaximumProcessedSynapses(){return maximum;}
        public long getProccessedSynapses(){return decisions==0?0:actual;}
        public double[] process(){decisions++;return super.process();}
    }
    static Cell cell(FixedBrain brain,double primary,double brainEnergy) {
        Cell cell=new Cell(new Vector2D(100,100),10,brain);cell.setEnergy(primary);cell.setEnergy(EnergyType.SECONDARY,brainEnergy);return cell;
    }
    static Environment world(Cell... cells) {
        Environment world=new Environment(600,300);for(Cell cell:cells)world.addCell(cell);return world;
    }
    static void step(Environment world,double dt,SimulationStepPolicy policy) {
        var grid=new SpatialGridPartitioningStrategy(world.getWidth(),world.getHeight(),60);grid.build(world.getCellList());world.update(dt,grid,policy);
    }
    static void step(Environment world,double dt) { step(world,dt,SimulationStepPolicy.NONE); }

    @Test void starvedBrainCoversOnlyItsShortfallWithHalfEnzymeYieldAndThinksAgain() {
        var brain=new FixedBrain(10,5,3);Cell cell=cell(brain,.5,0);step(world(cell),.1);
        double shortfall=BrainEnergyCosts.DEFAULT.storageCost(10,.1)+BrainEnergyCosts.DEFAULT.workCost(5);
        assertEquals(1,brain.decisions);assertFalse(cell.isBrainPaused());
        assertEquals(.5-shortfall/EFF-SimulationConfig.ENERGY_DECAY_RATE*.1,cell.getEnergy(),1e-15);
        assertEquals(BrainEnergyCosts.DEFAULT.workCost(2),cell.getEnergy(EnergyType.SECONDARY),1e-15);
        assertTrue(EFF<EnergyConversionService.EFFICIENCY);
    }
    @Test void respirationIsRateLimitedSoLargeBrainsPauseWhileSavingUp() {
        long work=20_000;double dt=.1,perStep=RATE*dt*EFF,bill=BrainEnergyCosts.DEFAULT.workCost(work);
        var brain=new FixedBrain(0,work,work);Cell cell=cell(brain,.5,0);Environment world=world(cell);
        int expectedPaused=(int)Math.ceil(bill/perStep-1e-9)-1;
        assertTrue(expectedPaused>1);
        for(int i=0;i<expectedPaused;i++) {
            double before=cell.getEnergy();step(world,dt);
            assertTrue(cell.isBrainPaused());assertEquals(0,brain.decisions);
            assertEquals(before-RATE*dt-SimulationConfig.ENERGY_DECAY_RATE*dt,cell.getEnergy(),1e-15);
            assertEquals((i+1)*perStep,cell.getEnergy(EnergyType.SECONDARY),1e-15);
        }
        step(world,dt);assertEquals(1,brain.decisions);assertFalse(cell.isBrainPaused());
    }
    @Test void respirationNeverSpendsThisStepsMaintenanceEnergy() {
        double dt=.1,maintenance=SimulationConfig.ENERGY_DECAY_RATE*dt;
        var brain=new FixedBrain(0,5000,5000);Cell cell=cell(brain,maintenance+1e-7,0);step(world(cell),dt);
        assertEquals(1e-7*EFF,cell.getEnergy(EnergyType.SECONDARY),1e-15);
        assertTrue(cell.isBrainPaused());assertEquals(0,brain.decisions);
        var exhausted=new FixedBrain(10,5,3);Cell poor=cell(exhausted,maintenance,0);
        assertFalse(BrainEnergyService.prepare(poor,dt,BrainEnergyCosts.DEFAULT,true));
        assertEquals(maintenance,poor.getEnergy());assertEquals(0,poor.getEnergy(EnergyType.SECONDARY));
    }
    @Test void freeBrainsRemainsAndDisabledMetabolismNeverRespire() {
        var free=new FixedBrain(10,5,3);Cell freeCell=cell(free,.5,0);
        step(world(freeCell),.1,new SimulationStepPolicy(){
            @Override public BrainEnergyCosts brainEnergyCosts(){return BrainEnergyCosts.FREE;}
        });
        assertEquals(1,free.decisions);assertEquals(0,freeCell.getEnergy(EnergyType.SECONDARY));
        assertEquals(.5-SimulationConfig.ENERGY_DECAY_RATE*.1,freeCell.getEnergy(),1e-15);
        var off=new FixedBrain(10,5,3);Cell offCell=cell(off,.5,0);
        step(world(offCell),.1,new SimulationStepPolicy(){@Override public boolean useEnergyConversion(){return false;}});
        assertTrue(offCell.isBrainPaused());assertEquals(0,offCell.getEnergy(EnergyType.SECONDARY));
        Cell remains=cell(new FixedBrain(10,5,3),.5,0);remains.becomeRemains();
        assertFalse(BrainEnergyService.prepare(remains,.1,BrainEnergyCosts.DEFAULT,true));assertEquals(.5,remains.getEnergy());
    }
    @Test void inheritedEnzymeThinksMoreOftenAndPaysHalfThePrimaryEnergyPerDecision() {
        long work=50_000;double dt=.1;int steps=200;
        var basalBrain=new FixedBrain(0,work,work);Cell basal=cell(basalBrain,1,0);
        var enzymeBrain=new FixedBrain(0,work,work);Cell enzyme=cell(enzymeBrain,1,1);
        enzyme.setEnergyConversion(new EnergyConversion(EnergyType.PRIMARY,EnergyType.SECONDARY,1));
        enzyme.setPosition(new Vector2D(400,100));
        Environment world=world(basal,enzyme);
        for(int i=0;i<steps;i++)step(world,dt);
        double maintenance=SimulationConfig.ENERGY_DECAY_RATE*dt*steps,bill=BrainEnergyCosts.DEFAULT.workCost(work);
        assertEquals(steps,enzymeBrain.decisions);
        assertTrue(basalBrain.decisions<steps/4,"basal decisions "+basalBrain.decisions);
        assertTrue(basalBrain.decisions>0);
        // Primary spent on the brain per unit of brain energy actually used or still stored.
        double basalPerUnit=(1-basal.getEnergy()-maintenance)/(basalBrain.decisions*bill+basal.getEnergy(EnergyType.SECONDARY));
        double enzymePerUnit=(1-enzyme.getEnergy()-maintenance)/(enzymeBrain.decisions*bill+enzyme.getEnergy(EnergyType.SECONDARY)-1);
        assertEquals(1/EFF,basalPerUnit,1e-9);assertEquals(1/EnergyConversionService.EFFICIENCY,enzymePerUnit,1e-9);
    }
    @Test void reversalMutationTurnsInertCarrionRouteIntoBrainSupplyInOneStep() {
        Random reverseOnly=new Random(){
            int call;@Override public double nextDouble(){return call++%4==3?0:.99;}
        };
        assertEquals(new EnergyConversion(EnergyType.PRIMARY,EnergyType.SECONDARY,0),EnergyConversion.NONE.mutate(reverseOnly,.5,.1));
        Random random=new Random(7);boolean reached=false;
        for(int i=0;i<2000;i++){
            EnergyConversion next=EnergyConversion.NONE.mutate(random,.3,.1);assertNotEquals(next.source(),next.target());
            reached|=next.source()==EnergyType.PRIMARY && next.target()==EnergyType.SECONDARY;
        }
        assertTrue(reached);
    }
}
