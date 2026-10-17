package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RemainsTest {
    private final SimulationConfig config=SimulationConfig.getInstance();
    private double sun,deathAge;
    @BeforeEach void setup(){sun=config.getSunRayRate();deathAge=config.getCellDeathAge();config.setSunRayRate(0);config.setCellDeathAge(100);}
    @AfterEach void restore(){config.setSunRayRate(sun);config.setCellDeathAge(deathAge);}
    private Cell cell(double x){return new Cell(new Vector2D(x,100),10,new PhysicsRegressionTest.ConstantBrain());}
    private void step(Environment world,double dt) {
        var grid=new SpatialGridPartitioningStrategy(300,300,60);grid.build(world.getCellList());world.update(dt,grid);
    }
    @Test void ageDeathPreservesAllEightReservesAndDiscardsBrainAndCommands() {
        Cell cell=cell(100);for(EnergyType type:EnergyType.ALL)cell.setEnergy(type,.1+.1*type.getCode());
        cell.setAge(100);Environment world=new Environment(300,300);world.addCell(cell);step(world,0);
        assertSame(cell,world.getCellList().getFirst());assertTrue(cell.isRemains());
        assertSame(RemainsBrain.INSTANCE,cell.getBrain());assertEquals(0,cell.getBrain().getInputCount());
        for(EnergyType type:EnergyType.ALL)assertEquals(.1+.1*type.getCode(),cell.getEnergy(type));
        for(SensorActor actor:cell.getSensorActors()) {
            assertEquals(0,actor.getForceStrength());assertEquals(0,actor.getEnergyAbsorption());
            assertEquals(0,actor.getEnergyDelivery());assertEquals(0,actor.getReproductionDesire());
        }
        assertFalse(ReproductionManagerService.canReproduce(config,cell));
        assertNull(ReproductionManagerService.reproduceClonally(config,world,cell));
        assertThrows(IllegalArgumentException.class,()->ReproductionManagerService.createTrainingFounder(config,world,cell,new Vector2D(180,100),0,false));
    }
    @Test void primaryStarvationLeavesOtherReservesButNoEmptyCorpse() {
        Environment world=new Environment(300,300);Cell residual=cell(100),empty=cell(200);
        residual.setEnergy(0);empty.setEnergy(0);residual.setEnergy(EnergyType.RGB_111,.7);
        world.addCell(residual);world.addCell(empty);step(world,0);
        assertEquals(1,world.getCellList().size());assertTrue(residual.isRemains());assertEquals(.7,residual.getTotalEnergy());
    }
    @Test void inertStateStopsGrowingThinkingAgingAndMetabolismButKeepsPassivePhysics() {
        Cell corpse=cell(100);corpse.startGrowthProcess();corpse.setRealGrowRadiusSize(.7);corpse.setTargetRadiusSize(20);
        corpse.setVelocity(new Vector2D(3,0));corpse.setAngularVelocity(.5);corpse.becomeRemains();
        Environment world=new Environment(300,300);world.addCell(corpse);
        double radius=corpse.getRadiusSize(),age=corpse.getAge(),energy=corpse.getEnergy();
        assertDoesNotThrow(()->CellBrainService.think(corpse));
        CellBrainService.applyOutputs(corpse,new double[CellFactory.calcOutputCount(12)]);
        EnergyCellCalcService.decayEnergy(corpse,10,true);
        for(int i=0;i<1000;i++)step(world,1.0/60);
        assertEquals(1,world.getCellList().size());assertEquals(radius,corpse.getRadiusSize());
        assertEquals(age,corpse.getAge());assertEquals(energy,corpse.getEnergy());assertNotEquals(100,corpse.getPosition().getX());
        assertEquals(0,corpse.getBrain().getProccessedSynapses());
    }
    private static class EaterBrain extends PhysicsRegressionTest.ConstantBrain {
        @Override public double[] process() {
            double[] outputs=super.process();outputs[GlobalOutputFeature.values().length+ActorOutputFeature.ENERGY_ABSORPTION.ordinal()]=1;return outputs;
        }
    }
    @ParameterizedTest @ValueSource(doubles={100,290})
    void realSensorAbsorptionEmptiesAllTypesAndRemovesCorpseIncludingAtTheWorldSeam(double x) {
        Environment world=new Environment(300,300);Cell eater=new Cell(new Vector2D(x,100),10,new EaterBrain());
        Cell corpse=cell(Vector2D.wrapCoordinate(x+22,300));eater.setEnergy(.5);
        for(EnergyType type:EnergyType.ALL)corpse.setEnergy(type,.001);corpse.becomeRemains();
        eater.getSensorActors().getFirst().setEnergyAbsorption(1);world.addCell(eater);world.addCell(corpse);
        step(world,1.0/60);
        assertEquals(1,world.getCellList().size());assertSame(eater,world.getCellList().getFirst());
        assertEquals(0,corpse.getTotalEnergy(),1e-14);
        assertEquals(.501-SimulationConfig.ENERGY_DECAY_RATE/60,eater.getEnergy(),1e-14);
        for(EnergyType type:EnergyType.ALL)if(type!=EnergyType.PRIMARY)assertEquals(.001,eater.getEnergy(type),1e-14);
    }
    private void contact(Cell from,Cell to) {
        var a=from.getSensorActors().getFirst();var b=to.getSensorActors().getFirst();
        a.setSensedCell(to);a.setSensedActor(b);a.updateCachedPosition();b.updateCachedPosition();
    }
    @Test void corpsesCannotReceiveDonationsOrActivelyDrainLivingCellsEvenWithCorruptedCommands() {
        Cell living=cell(100),corpse=cell(100);living.setEnergy(.8);corpse.setEnergy(.2);corpse.becomeRemains();
        contact(living,corpse);contact(corpse,living);
        living.getSensorActors().getFirst().setEnergyDelivery(1);
        corpse.getSensorActors().getFirst().setEnergyAbsorption(1);corpse.getSensorActors().getFirst().setEnergyDelivery(1);
        EnergyTransferCellCalcService.processEnergyTransfers(List.of(living,corpse),new Environment(300,300));
        assertEquals(.8,living.getEnergy());assertEquals(.2,corpse.getEnergy());
    }
    @Test void corpseIsAnOpaqueBodyWithoutSolarRefillInAnyEnergyForm() {
        double spacing=config.getSunRaySpacingPx();EnergyType source=config.getSunEnergyType();
        try {
            config.setSunRayRate(10);config.setSunRaySpacingPx(10);
            Cell corpse=new Cell(new Vector2D(150-100*Math.tan(Math.toRadians(35)),200),10,new PhysicsRegressionTest.ConstantBrain());
            corpse.setEnergy(.2);corpse.becomeRemains();
            for(EnergyType type:EnergyType.ALL) {
                config.setSunEnergyType(type);
                var sun=new EnergySunCalcService(new Random(1){@Override public double nextDouble(){return .5;}});
                assertFalse(sun.calculateSunEnergy(List.of(corpse),List.of(),300,300,config,.01).isEmpty());
                assertEquals(.2,corpse.getTotalEnergy());assertFalse(corpse.isSunRayHit());
            }
        } finally {config.setSunRaySpacingPx(spacing);config.setSunEnergyType(source);}
    }
    @Test void savingAndLoadingRetainsDeadStateAllReservesAndSharedInertController() throws Exception {
        Cell corpse=cell(100);for(EnergyType type:EnergyType.ALL)corpse.setEnergy(type,.1*type.getCode());corpse.becomeRemains();
        var bytes=new ByteArrayOutputStream();try(var out=new ObjectOutputStream(bytes)){out.writeObject(corpse);}
        Cell loaded;try(var in=new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))){loaded=(Cell)in.readObject();}
        assertTrue(loaded.isRemains());assertSame(RemainsBrain.INSTANCE,loaded.getBrain());
        for(EnergyType type:EnergyType.ALL)assertEquals(corpse.getEnergy(type),loaded.getEnergy(type));
        Environment world=new Environment(300,300);world.addCell(loaded);step(world,.1);assertTrue(loaded.isRemains());
    }
    @Test void explicitEpisodePolicyCanDiscardDyingCellsAsBefore() {
        Cell c=cell(100);c.setAge(100);Environment world=new Environment(300,300);world.addCell(c);
        var grid=new SpatialGridPartitioningStrategy(300,300,60);grid.build(world.getCellList());world.update(0,grid,SimulationStepPolicy.DISCARD_REMAINS);
        assertTrue(world.getCellList().isEmpty());
    }
}
