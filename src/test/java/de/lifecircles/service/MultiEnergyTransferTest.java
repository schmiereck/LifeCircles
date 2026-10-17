package de.lifecircles.service;

import de.lifecircles.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MultiEnergyTransferTest {
    private final Environment world=new Environment(300,300);
    private Cell cell(double primary,double secondary) {
        Cell c=new Cell(new Vector2D(100,100),10,new PhysicsRegressionTest.ConstantBrain());
        c.setEnergy(primary);c.setEnergy(EnergyType.SECONDARY,secondary);world.addCell(c);return c;
    }
    private void contact(Cell from,Cell to,int port,double delivery,double absorption) {
        SensorActor a=from.getSensorActors().get(port),b=to.getSensorActors().get(port);
        a.setSensedCell(to);a.setSensedActor(b);a.setEnergyDelivery(delivery);a.setEnergyAbsorption(absorption);
        a.updateCachedPosition();b.updateCachedPosition();
    }
    private void step(double dt){EnergyTransferCellCalcService.processEnergyTransfers(world.getCellList(),world,dt);}
    private double total(EnergyType type){return world.getCellList().stream().mapToDouble(c->c.getEnergy(type)).sum();}

    @ParameterizedTest @EnumSource(EnergyType.class)
    void deliveryKeepsEachTypeSeparateAndSharesAThinReserve(EnergyType type) {
        Cell donor=cell(0,0),a=cell(0,0),b=cell(0,0);
        donor.setEnergy(type,.101);a.setEnergy(type,.2);b.setEnergy(type,.2);
        contact(donor,a,0,1,0);contact(donor,b,1,1,0);step(1.0/60);
        assertEquals(.1,donor.getEnergy(type),1e-14);
        assertEquals(.2005,a.getEnergy(type),1e-14);assertEquals(a.getEnergy(type),b.getEnergy(type),1e-14);
        for(EnergyType other:EnergyType.ALL)if(other!=type)assertEquals(0,total(other));
    }
    @Test void fullPrimaryReceiverCanStillReceiveSecondaryWithoutSpilloverOrConversion() {
        Cell donor=cell(.8,.8),receiver=cell(1,.2);contact(donor,receiver,0,1,0);step(1.0/60);
        assertEquals(.8,donor.getEnergy());assertEquals(1,receiver.getEnergy());
        assertEquals(.795,donor.getEnergy(EnergyType.SECONDARY),1e-14);
        assertEquals(.205,receiver.getEnergy(EnergyType.SECONDARY),1e-14);
        assertEquals(1.8,total(EnergyType.PRIMARY),1e-14);assertEquals(1,total(EnergyType.SECONDARY),1e-14);
    }
    @Test void absorptionCanEmptySecondaryEvenWhenPrimaryIsAlreadyEmpty() {
        Cell receiver=cell(.2,.2),donor=cell(0,.003);contact(receiver,donor,0,0,1);step(1.0/60);
        assertEquals(.2,receiver.getEnergy());assertEquals(.203,receiver.getEnergy(EnergyType.SECONDARY),1e-14);
        assertEquals(0,donor.getTotalEnergy());
    }
    @ParameterizedTest @EnumSource(EnergyType.class)
    void throughputIsIndependentOfTimeStepForEachEnergyType(EnergyType type) {
        Cell donor=cell(0,0),receiver=cell(0,0);contact(donor,receiver,0,.5,0);
        for(int hz:new int[]{30,60,120}) {
            donor.setEnergy(type,.9);receiver.setEnergy(type,.1);
            for(int i=0;i<hz;i++)step(1.0/hz);
            assertEquals(.25,receiver.getEnergy(type),1e-13);assertEquals(1,total(type),1e-13);
        }
    }
    @Test void denseMixedTransfersKeepBothBudgetsAndIgnoreCellOrder() {
        Random random=new Random(892);List<Cell> cells=new ArrayList<>();
        double[][] initial=new double[24][2];
        for(int i=0;i<24;i++){initial[i][0]=random.nextDouble();initial[i][1]=random.nextDouble();cells.add(cell(initial[i][0],initial[i][1]));}
        for(Cell c:cells)for(int p=0;p<12;p++){Cell to=cells.get(random.nextInt(cells.size()));if(to!=c)contact(c,to,p,random.nextDouble(),random.nextDouble());}
        double primary=total(EnergyType.PRIMARY),secondary=total(EnergyType.SECONDARY);step(2);
        double[][] expected=new double[24][2];
        for(int i=0;i<24;i++)for(EnergyType type:EnergyType.ALL)expected[i][type.ordinal()]=cells.get(i).getEnergy(type);
        for(int repeat=0;repeat<20;repeat++) {
            for(int i=0;i<24;i++)for(EnergyType type:EnergyType.ALL)cells.get(i).setEnergy(type,initial[i][type.ordinal()]);
            Collections.shuffle(world.getCellList(),random);step(2);
            for(int i=0;i<24;i++)for(EnergyType type:EnergyType.ALL)assertEquals(expected[i][type.ordinal()],cells.get(i).getEnergy(type),1e-14);
            assertEquals(primary,total(EnergyType.PRIMARY),1e-13);assertEquals(secondary,total(EnergyType.SECONDARY),1e-13);
        }
    }
    @Test void secondaryTransferUsesPeriodicContactDistance() {
        Cell donor=cell(.8,.8),receiver=cell(.2,.2);
        donor.setPosition(new Vector2D(100,100));receiver.setPosition(new Vector2D(122,100));
        contact(donor,receiver,0,1,0);step(1.0/60);double interior=receiver.getEnergy(EnergyType.SECONDARY);
        donor.setEnergy(EnergyType.SECONDARY,.8);receiver.setEnergy(EnergyType.SECONDARY,.2);
        donor.setPosition(new Vector2D(290,100));receiver.setPosition(new Vector2D(12,100));
        contact(donor,receiver,0,1,0);step(1.0/60);
        assertEquals(interior,receiver.getEnergy(EnergyType.SECONDARY),1e-14);
    }
}
