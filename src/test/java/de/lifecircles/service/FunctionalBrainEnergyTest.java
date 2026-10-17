package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import de.lifecircles.service.trainStrategy.TrainStrategy;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class FunctionalBrainEnergyTest {
    final SimulationConfig config=SimulationConfig.getInstance();
    double sun,price;
    @BeforeEach void save() {sun=config.getSunRayRate();price=config.getBrainEnergyCosts().work();config.setSunRayRate(0);config.setBrainEnergyMultiplier(1);}
    @AfterEach void restore(){config.setSunRayRate(sun);config.setBrainEnergyMultiplier(price);}
    static class CountingBrain extends PhysicsRegressionTest.ConstantBrain {
        int decisions;
        public int getSynapseCount(){return 10;}
        public long getMaximumProcessedSynapses(){return 5;}
        public long getProccessedSynapses(){return decisions==0?0:3;}
        public double[] process(){decisions++;return super.process();}
    }
    Cell cell(CountingBrain brain) {return new Cell(new Vector2D(100,100),10,brain);}
    void step(Environment world,double dt) {
        var grid=new SpatialGridPartitioningStrategy(world.getWidth(),world.getHeight(),60);grid.build(world.getCellList());world.update(dt,grid);
    }
    /** Pause semantics in isolation: no innate 000 -> 001 respiration may rescue the brain. */
    static final SimulationStepPolicy NO_RESPIRATION=new SimulationStepPolicy() {
        @Override public boolean keepRemains() { return true; }
        @Override public boolean useBasalBrainRespiration() { return false; }
    };
    void stepWithoutRespiration(Environment world,double dt) {
        var grid=new SpatialGridPartitioningStrategy(world.getWidth(),world.getHeight(),60);grid.build(world.getCellList());world.update(dt,grid,NO_RESPIRATION);
    }
    @Test void separatesMaintenanceFromFullyFundedActualWorkAndChargesStorageOnce() {
        var brain=new CountingBrain();Cell cell=cell(brain);cell.setEnergy(.5);cell.setEnergy(EnergyType.SECONDARY,.2);
        Environment world=new Environment(300,300);world.addCell(cell);step(world,.25);
        assertEquals(1,brain.decisions);assertFalse(cell.isBrainPaused());
        assertEquals(.5-SimulationConfig.ENERGY_DECAY_RATE*.25,cell.getEnergy(),1e-14);
        assertEquals(.2-BrainEnergyCosts.DEFAULT.cost(10,3,.25),cell.getEnergy(EnergyType.SECONDARY),1e-14);
    }
    @Test void insufficientBudgetDoesNotComputeButPaysStorageAndPreservesChosenAbsorption() {
        var brain=new CountingBrain();Cell cell=cell(brain);cell.setEnergy(.5);
        cell.setEnergy(EnergyType.SECONDARY,BrainEnergyCosts.DEFAULT.cost(10,4,.1));
        var actor=cell.getSensorActors().getFirst();actor.setEnergyAbsorption(.7);actor.setEnergyDelivery(1);actor.setReproductionDesire(1);actor.setForceStrength(10);
        cell.setIsGrowing(true);double growth=cell.getGrowthAge(),radius=cell.getRadiusSize();
        Environment world=new Environment(300,300);world.addCell(cell);stepWithoutRespiration(world,.1);
        assertEquals(0,brain.decisions);assertTrue(cell.isBrainPaused());assertEquals(.7,actor.getEnergyAbsorption());
        assertEquals(0,actor.getEnergyDelivery());assertEquals(0,actor.getReproductionDesire());assertEquals(0,actor.getForceStrength());
        assertEquals(growth,cell.getGrowthAge());assertEquals(radius,cell.getRadiusSize());
        assertEquals(BrainEnergyCosts.DEFAULT.workCost(4),cell.getEnergy(EnergyType.SECONDARY),1e-20);
        assertEquals(1,world.getCellList().size());assertFalse(cell.isRemains());
    }
    @Test void storageShortageDrainsOnlyRemainingBrainReserveWithoutTouchingOtherForms() {
        var brain=new CountingBrain();Cell cell=cell(brain);cell.setEnergy(EnergyType.SECONDARY,1e-12);cell.setEnergy(EnergyType.RGB_111,.3);
        Environment world=new Environment(300,300);world.addCell(cell);stepWithoutRespiration(world,1);
        assertEquals(0,brain.decisions);assertEquals(0,cell.getEnergy(EnergyType.SECONDARY));assertEquals(.3,cell.getEnergy(EnergyType.RGB_111));
        assertEquals(1-SimulationConfig.ENERGY_DECAY_RATE,cell.getEnergy(),1e-14);
    }
    @Test void conversionCanWakeAPausedBrainWithoutAnyDecisionInBetween() {
        var brain=new CountingBrain();Cell cell=cell(brain);cell.setEnergy(.5);Environment world=new Environment(300,300);world.addCell(cell);
        stepWithoutRespiration(world,.1);assertTrue(cell.isBrainPaused());assertEquals(0,brain.decisions);
        cell.setEnergyConversion(new EnergyConversion(EnergyType.PRIMARY,EnergyType.SECONDARY,1));
        double before=cell.getTotalEnergy();stepWithoutRespiration(world,.1);
        assertFalse(cell.isBrainPaused());assertEquals(1,brain.decisions);
        assertEquals(before-.002-SimulationConfig.ENERGY_DECAY_RATE*.1-BrainEnergyCosts.DEFAULT.cost(10,3,.1),cell.getTotalEnergy(),1e-14);
    }
    @Test void ordinaryAbsorptionCanWakeAStarvedBrainAndEmptyTheDonorCorpse() {
        var brain=new CountingBrain();Cell cell=cell(brain);cell.getSensorActors().getFirst().setEnergyAbsorption(1);
        Environment world=new Environment(300,300);world.addCell(cell);stepWithoutRespiration(world,1.0/60);assertTrue(cell.isBrainPaused());
        Cell food=new Cell(new Vector2D(122,100),10,new PhysicsRegressionTest.ConstantBrain());food.setEnergy(0);food.setEnergy(EnergyType.SECONDARY,.001);food.becomeRemains();
        world.addCell(food);stepWithoutRespiration(world,1.0/60);
        assertEquals(1,brain.decisions);assertFalse(cell.isBrainPaused());assertEquals(0,food.getTotalEnergy());assertEquals(1,world.getCellList().size());
    }
    @Test void zeroFeesExplicitlyAllowFreeBrainsAndEachAuthorizationCanBeUsedOnlyOnce() {
        var brain=new CountingBrain();Cell cell=cell(brain);
        assertThrows(IllegalStateException.class,()->BrainEnergyService.thinkPrepared(cell,new double[316],BrainEnergyCosts.FREE));assertEquals(0,brain.decisions);
        assertTrue(BrainEnergyService.prepare(cell,.1,BrainEnergyCosts.FREE));
        BrainEnergyService.thinkPrepared(cell,new double[316],BrainEnergyCosts.FREE);assertEquals(1,brain.decisions);
        assertThrows(IllegalStateException.class,()->BrainEnergyService.thinkPrepared(cell,new double[316],BrainEnergyCosts.FREE));
    }
    @Test void foundersRedistributeOneBudgetAndOldSaveMigrationIsConservativeAndIdempotent() throws Exception {
        Cell founder=CellFactory.createCell(new Vector2D(100,100),10);
        assertEquals(.8,founder.getEnergy());assertEquals(.2,founder.getEnergy(EnergyType.SECONDARY));assertEquals(1,founder.getTotalEnergy());
        EnergyAllocationService.initializeFounder(founder,.18);assertEquals(.144,founder.getEnergy());assertEquals(.036,founder.getEnergy(EnergyType.SECONDARY));assertEquals(.18,founder.getTotalEnergy());
        try(var input=new ObjectInputStream(Objects.requireNonNull(getClass().getResourceAsStream("/energy/single-conversion-v1.cells")))) {
            List<?> loaded=(List<?>)input.readObject();Cell old=(Cell)loaded.get(0);double total=old.getTotalEnergy();
            old.migrateEnergyUsage();assertEquals(.16,old.getEnergy(),1e-14);assertEquals(.04,old.getEnergy(EnergyType.SECONDARY),1e-14);
            assertEquals(total,old.getTotalEnergy(),1e-14);old.migrateEnergyUsage();assertEquals(.16,old.getEnergy(),1e-14);
            Cell remains=(Cell)loaded.get(1);remains.becomeRemains();remains.migrateEnergyUsage();assertEquals(.4,remains.getEnergy());assertEquals(.3,remains.getEnergy(EnergyType.SECONDARY));
        }
    }
    @Test void primaryStarvationStillKillsAndLeavesOtherReservesEvenWhenBrainIsPaused() {
        var brain=new CountingBrain();Cell cell=cell(brain);cell.setEnergy(0);cell.setEnergy(EnergyType.RGB_111,.7);
        Environment world=new Environment(300,300);world.addCell(cell);step(world,.1);
        assertTrue(cell.isRemains());assertEquals(.7,cell.getEnergy(EnergyType.RGB_111));assertEquals(0,brain.decisions);
    }
    @Test void pauseLeavesRealRecurrentMemoryAndSchedulingUntouchedAndCanResume() {
        var previous=NeuralNetwork.getRandom();NeuralNetwork.setRandom(new Random(912));
        try {
            Cell cell=CellFactory.createCell(new Vector2D(100,100),10);var brain=(CellBrain)cell.getBrain();
            CellBrainService.think(cell);NeuralNetwork network=brain.getNeuralNetwork();
            Map<String,Double> memory=network.getNeuralNet().getNeuronActivations(network.getNeuronValueFunction());
            double[] counters=Arrays.stream(network.getHiddenLayerArr()).mapToDouble(Layer::getActivationCounter).toArray();
            cell.setEnergy(EnergyType.SECONDARY,0);Environment world=new Environment(300,300);world.addCell(cell);
            for(int i=0;i<5;i++)stepWithoutRespiration(world,1.0/60);
            assertTrue(cell.isBrainPaused());assertEquals(memory,network.getNeuralNet().getNeuronActivations(network.getNeuronValueFunction()));
            assertArrayEquals(counters,Arrays.stream(network.getHiddenLayerArr()).mapToDouble(Layer::getActivationCounter).toArray());
            cell.setEnergy(EnergyType.SECONDARY,.2);stepWithoutRespiration(world,1.0/60);assertFalse(cell.isBrainPaused());assertTrue(brain.getProccessedSynapses()>0);
        } finally {NeuralNetwork.setRandom(previous);}
    }
    @Test void pausedStepsNeverBillLastDecisionsWorkAgain() {
        var brain=new CountingBrain();Cell cell=cell(brain);cell.setEnergy(EnergyType.SECONDARY,.01);
        Environment world=new Environment(300,300);world.addCell(cell);stepWithoutRespiration(world,.1);assertEquals(1,brain.decisions);
        double reserve=BrainEnergyCosts.DEFAULT.workCost(4);cell.setEnergy(EnergyType.SECONDARY,reserve);
        for(int i=0;i<3;i++)stepWithoutRespiration(world,.1);
        assertEquals(1,brain.decisions);assertTrue(cell.isBrainPaused());
        assertEquals(reserve-3*BrainEnergyCosts.DEFAULT.storageCost(10,.1),cell.getEnergy(EnergyType.SECONDARY),1e-20);
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void importingOldPopulationPublishesMigratedBudgetBeforeStartAndSavingDoesNotMigrateTwice(boolean merge) throws Exception {
        Environment world=new Environment(300,300);world.addCell(cell(new CountingBrain()));
        CalculationService service=new CalculationService(world,new TrainStrategy(){
            public void initialize(Environment w){}
            public void selectAndMutate(Environment w){}
            public Environment initializeEnvironment(){return world;}
        },new SpatialGridPartitioningStrategy(300,300,60));
        service.pause();
        List<Cell> loaded;
        try(var input=new ObjectInputStream(Objects.requireNonNull(getClass().getResourceAsStream("/energy/single-conversion-v1.cells")))) {
            @SuppressWarnings("unchecked") List<Cell> cells=(List<Cell>)input.readObject();loaded=cells;
        }
        service.importCells(loaded,merge);
        var state=service.getLatestState();var first=state.getCells().get(merge?1:0);
        assertEquals(.16,first.getEnergy(),1e-14);assertEquals(.04,first.getEnergy(EnergyType.SECONDARY),1e-14);
        assertEquals(.8,loaded.getFirst().getTotalEnergy(),1e-14);assertEquals(0,service.getStepCount());assertTrue(service.isPaused());
        var bytes=new ByteArrayOutputStream();try(var output=new ObjectOutputStream(bytes)){output.writeObject(loaded);}
        try(var input=new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            @SuppressWarnings("unchecked") List<Cell> saved=(List<Cell>)input.readObject();service.importCells(saved,false);
        }
        assertEquals(.16,service.getLatestState().getCells().getFirst().getEnergy(),1e-14);
        assertEquals(.04,service.getLatestState().getCells().getFirst().getEnergy(EnergyType.SECONDARY),1e-14);
    }
}
