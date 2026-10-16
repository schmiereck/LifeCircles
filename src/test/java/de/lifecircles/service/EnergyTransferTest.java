package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EnergyTransferTest {
    private final Environment world = new Environment(300, 300);
    private Cell cell(double energy) {
        Cell c = new Cell(new Vector2D(100, 100), 10, new PhysicsRegressionTest.ConstantBrain());
        c.setEnergy(energy); world.addCell(c); return c;
    }
    private SensorActor contact(Cell from, Cell to, int port, double delivery, double absorption) {
        SensorActor actor = from.getSensorActors().get(port), target = to.getSensorActors().get(port);
        actor.setSensedCell(to); actor.setSensedActor(target);
        actor.updateCachedPosition(); target.updateCachedPosition();
        actor.setEnergyDelivery(delivery); actor.setEnergyAbsorption(absorption); return actor;
    }
    private void step() { EnergyTransferCellCalcService.processEnergyTransfers(world.getCellList(), world); }
    private double total() { return world.getCellList().stream().mapToDouble(Cell::getEnergy).sum(); }

    @Test void deliveryIsProportionalToOutputInsteadOfSaturatingAtTinySignals() {
        Cell donor = cell(.8), receiver = cell(.2);
        SensorActor actor = contact(donor, receiver, 0, .5, 0);
        step(); assertEquals(.2025, receiver.getEnergy(), 1e-14);
        donor.setEnergy(.8); receiver.setEnergy(.2); actor.setEnergyDelivery(.001);
        step(); assertEquals(.200005, receiver.getEnergy(), 1e-14);
        assertEquals(1, total(), 1e-14);
    }
    @Test void absorptionIsProportionalAndCanDrainDonor() {
        Cell receiver = cell(.2), donor = cell(.003);
        contact(receiver, donor, 0, 0, .25); step();
        assertEquals(.2025, receiver.getEnergy(), 1e-14);
        step(); assertEquals(0, donor.getEnergy(), 1e-14); assertEquals(.203, total(), 1e-14);
    }
    @Test void deliveryPreservesReserveAcrossAllPortsAndSharesScarceEnergy() {
        Cell donor = cell(.101), a = cell(.2), b = cell(.2);
        contact(donor, a, 0, 1, 0); contact(donor, b, 1, 1, 0); step();
        assertEquals(.1, donor.getEnergy(), 1e-14);
        assertEquals(.2005, a.getEnergy(), 1e-14); assertEquals(a.getEnergy(), b.getEnergy(), 1e-14);
        assertEquals(.501, total(), 1e-14);
    }
    @Test void multipleDonorsShareReceiverCapacityFairly() {
        Cell a = cell(.8), b = cell(.8), receiver = cell(.999);
        contact(a, receiver, 0, 1, 0); contact(b, receiver, 0, 1, 0); step();
        assertEquals(1, receiver.getEnergy(), 1e-14);
        assertEquals(.7995, a.getEnergy(), 1e-14); assertEquals(a.getEnergy(), b.getEnergy(), 1e-14);
        assertEquals(2.599, total(), 1e-14);
    }
    @Test void freshEnergyDoesNotTeleportThroughChainDependingOnIterationOrder() {
        Cell a = cell(.8), b = cell(.1), c = cell(.2);
        contact(a,b,0,1,0); contact(b,c,0,1,0); step();
        assertEquals(.105,b.getEnergy(),1e-14); assertEquals(.2,c.getEnergy(),1e-14);
        a.setEnergy(.8); b.setEnergy(.1); c.setEnergy(.2);
        Collections.reverse(world.getCellList()); step();
        assertEquals(.105,b.getEnergy(),1e-14); assertEquals(.2,c.getEnergy(),1e-14);
        step(); assertTrue(c.getEnergy() > .2); assertEquals(1.1,total(),1e-14);
    }
    @Test void noCommandsAndFullReceiverLeaveEnergyUnchanged() {
        Cell a = cell(.8), b = cell(1); contact(a,b,0,1,0); step();
        assertEquals(.8,a.getEnergy()); assertEquals(1,b.getEnergy());
        b.setEnergy(.2); a.getSensorActors().getFirst().setEnergyDelivery(0); step();
        assertEquals(.8,a.getEnergy()); assertEquals(.2,b.getEnergy());
    }

    @Test void throughputMatchesAt30_60And120HzWithoutBudgetSaturation() {
        Cell a = cell(.9), b = cell(.1); contact(a,b,0,.5,0);
        for (int hz : new int[]{30,60,120}) {
            a.setEnergy(.9); b.setEnergy(.1);
            for (int i=0;i<hz;i++) EnergyTransferCellCalcService.processEnergyTransfers(world.getCellList(),world,1.0/hz);
            assertEquals(.25,b.getEnergy(),1e-13); assertEquals(1,total(),1e-13);
        }
    }
    @Test void zeroTimeDoesNothingAndInvalidTimeIsRejected() {
        Cell a=cell(.8), b=cell(.2); contact(a,b,0,1,1);
        EnergyTransferCellCalcService.processEnergyTransfers(world.getCellList(),world,0);
        assertEquals(.8,a.getEnergy()); assertEquals(.2,b.getEnergy());
        for(double dt:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY})
            assertThrows(IllegalArgumentException.class,()->EnergyTransferCellCalcService.processEnergyTransfers(world.getCellList(),world,dt));
    }
    @Test void contactDistanceAttenuatesEvenAtFullOutput() {
        Cell a=cell(.8),b=cell(.2); SensorActor actor=contact(a,b,0,1,0);
        step(); double near=b.getEnergy()-.2;
        a.setEnergy(.8); b.setEnergy(.2); b.setPosition(new Vector2D(110,100));
        ((SensorActor)actor.getSensedActor()).updateCachedPosition(); step();
        assertTrue(b.getEnergy()-.2 < near); assertEquals(1,total(),1e-14);
    }
    @Test void crowdedMixedTransfersConserveEnergyAndIgnoreIterationOrder() {
        Random random=new Random(771);
        List<Cell> cells=new ArrayList<>();
        double[] initial=new double[30];
        for(int i=0;i<30;i++) {initial[i]=random.nextDouble();cells.add(cell(initial[i]));}
        for(Cell from:cells) for(int p=0;p<12;p++) {
            Cell to=cells.get(random.nextInt(cells.size()));
            if(to!=from) contact(from,to,p,random.nextDouble(),random.nextDouble());
        }
        double original=total();
        EnergyTransferCellCalcService.processEnergyTransfers(world.getCellList(),world,2);
        double[] expected=cells.stream().mapToDouble(Cell::getEnergy).toArray();
        assertEquals(original,total(),1e-13);
        for(int iteration=0;iteration<20;iteration++) {
            for(int i=0;i<cells.size();i++) cells.get(i).setEnergy(initial[i]);
            Collections.shuffle(world.getCellList(),random);
            EnergyTransferCellCalcService.processEnergyTransfers(world.getCellList(),world,2);
            for(int i=0;i<cells.size();i++) {
                assertEquals(expected[i],cells.get(i).getEnergy(),1e-14);
                assertTrue(cells.get(i).getEnergy()>=0 && cells.get(i).getEnergy()<=1);
            }
            assertEquals(original,total(),1e-13);
        }
    }
    @Test void realWorldSensingDeliversEnergyBeforeMetabolicCosts() {
        SimulationConfig config=SimulationConfig.getInstance(); double sun=config.getSunRayRate();
        try {
            config.setSunRayRate(0);
            Cell a=cell(.8), b=cell(.2); b.setPosition(new Vector2D(122,100));
            a.getSensorActors().getFirst().setEnergyDelivery(1);
            SpatialGridPartitioningStrategy grid=new SpatialGridPartitioningStrategy(300,300,60);
            grid.build(world.getCellList());
            double dt=1.0/60;
            world.update(dt,grid);
            assertSame(b,a.getSensorActors().getFirst().getSensedCell());
            assertTrue(b.getEnergy()>.2); assertTrue(a.getEnergy()<.8);
            assertEquals(1-2*SimulationConfig.ENERGY_DECAY_RATE*dt,total(),1e-12);
        } finally {config.setSunRayRate(sun);}
    }
    @Test void suppliedThreeCellChainFeedsShadedCellsWhilePayingMetabolism() {
        // Isolate a tissue's energy budget: geometry and commands stay fixed, no births or brains.
        Cell source=cell(.8), relay=cell(.2), shaded=cell(.2);
        contact(source,relay,0,.04,0); contact(relay,shaded,0,.02,0);
        double dt=1.0/60;
        for(int i=0;i<3600;i++) {
            source.setEnergy(source.getEnergy()+.015*dt);
            EnergyTransferCellCalcService.processEnergyTransfers(world.getCellList(),world,dt);
            for(Cell cell:world.getCellList()) EnergyCellCalcService.decayEnergy(cell,dt,false);
        }
        assertEquals(.8,source.getEnergy(),1e-11);
        assertEquals(.38,relay.getEnergy(),1e-11); assertEquals(.38,shaded.getEnergy(),1e-11);
        assertEquals(1.2 + .015*60 - 3*SimulationConfig.ENERGY_DECAY_RATE*60,total(),1e-11);
        // With the same metabolism but no delivery a shaded cell would fall from .2 to .02.
    }
}
