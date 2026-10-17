package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import java.io.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;

class EnergyConversionTest {
    private Cell cell() { return new Cell(new Vector2D(100,100),10,new PhysicsRegressionTest.ConstantBrain()); }
    static Stream<Arguments> routes() {
        return EnergyType.ALL.stream().flatMap(source->EnergyType.ALL.stream().filter(target->source!=target).map(target->Arguments.of(source,target)));
    }
    @ParameterizedTest @MethodSource("routes")
    void everyDirectedRouteConservesEnergyIncludingItsExplicitLoss(EnergyType source,EnergyType target) {
        Cell cell=cell();for(EnergyType type:EnergyType.ALL)cell.setEnergy(type,.3);
        cell.setEnergy(source,.7);cell.setEnergyConversion(new EnergyConversion(source,target,.4));
        double before=cell.getTotalEnergy();double loss=EnergyConversionService.convert(cell,.5);
        assertEquals(.68,cell.getEnergy(source),1e-14);assertEquals(.316,cell.getEnergy(target),1e-14);
        for(EnergyType type:EnergyType.ALL)if(type!=source && type!=target)assertEquals(.3,cell.getEnergy(type));
        assertEquals(.004,loss,1e-14);assertEquals(before,cell.getTotalEnergy()+loss,1e-14);
    }
    @Test void scarcityFullStoresAndSubstepsDoNotWasteOrCreateReserves() {
        Cell c=cell();c.setEnergy(0);c.setEnergy(EnergyType.RGB_111,.01);
        c.setEnergyConversion(new EnergyConversion(EnergyType.RGB_111,EnergyType.PRIMARY,1));
        assertEquals(.002,EnergyConversionService.convert(c,1),1e-14);
        assertEquals(.008,c.getEnergy(),1e-14);assertEquals(0,c.getEnergy(EnergyType.RGB_111));
        c.setEnergy(.999);c.setEnergy(EnergyType.RGB_111,.5);
        assertEquals(.00025,EnergyConversionService.convert(c,1),1e-14);
        assertEquals(1,c.getEnergy());assertEquals(.49875,c.getEnergy(EnergyType.RGB_111),1e-14);
        assertEquals(0,EnergyConversionService.convert(c,1));
        Cell full=cell(),split=cell();
        for(Cell cell:List.of(full,split)) {
            cell.setEnergy(0);cell.setEnergy(EnergyType.SECONDARY,.8);
            cell.setEnergyConversion(new EnergyConversion(EnergyType.SECONDARY,EnergyType.PRIMARY,.25));
        }
        double loss=EnergyConversionService.convert(full,2), splitLoss=0;
        for(int i=0;i<120;i++)splitLoss+=EnergyConversionService.convert(split,1.0/60);
        for(EnergyType type:EnergyType.ALL)assertEquals(full.getEnergy(type),split.getEnergy(type),1e-13);
        assertEquals(loss,splitLoss,1e-13);
    }
    @Test void repeatedReverseReactionsLoseEnergyAndCorpsesStayInert() {
        Cell cell=cell();cell.setEnergy(0);cell.setEnergy(EnergyType.SECONDARY,1);
        var forward=new EnergyConversion(EnergyType.SECONDARY,EnergyType.PRIMARY,1);
        var backward=new EnergyConversion(EnergyType.PRIMARY,EnergyType.SECONDARY,1);
        double loss=0;
        for(int i=0;i<100;i++) {
            cell.setEnergyConversion(i%2==0?forward:backward);loss+=EnergyConversionService.convert(cell,10);
        }
        assertEquals(1,cell.getTotalEnergy()+loss,1e-13);assertTrue(cell.getTotalEnergy()<1e-8);
        cell.setEnergy(EnergyType.SECONDARY,.5);cell.setEnergyConversion(forward);cell.becomeRemains();
        double before=cell.getTotalEnergy();assertEquals(0,EnergyConversionService.convert(cell,100));assertEquals(before,cell.getTotalEnergy());
    }
    @Test void inactiveLegacyCellsAndInvalidParametersCannotConvert() {
        Cell cell=cell();cell.setEnergy(EnergyType.SECONDARY,.5);
        assertEquals(EnergyConversion.NONE,cell.getEnergyConversion());assertEquals(0,EnergyConversionService.convert(cell,10));
        assertThrows(IllegalArgumentException.class,()->new EnergyConversion(EnergyType.PRIMARY,EnergyType.PRIMARY,1));
        for(double bad:new double[]{Double.NaN,Double.POSITIVE_INFINITY,-1,1.1})
            assertThrows(IllegalArgumentException.class,()->new EnergyConversion(EnergyType.PRIMARY,EnergyType.SECONDARY,bad));
        for(double bad:new double[]{Double.NaN,Double.POSITIVE_INFINITY,-1})
            assertThrows(IllegalArgumentException.class,()->EnergyConversionService.convert(cell,bad));
    }
    private static class RecordingBrain extends PhysicsRegressionTest.ConstantBrain {
        double[] inputs;
        public void setInputs(double[] values) { inputs=values.clone(); }
    }
    @Test void worldConvertsBeforeFreezingBothOwnAndNeighborSensors() {
        var config=SimulationConfig.getInstance();double sun=config.getSunRayRate();config.setSunRayRate(0);
        try {
            Environment world=new Environment(300,300);var a=new RecordingBrain();var b=new RecordingBrain();
            Cell converter=new Cell(new Vector2D(100,100),10,a),observer=new Cell(new Vector2D(122,100),10,b);
            converter.setEnergy(.5);converter.setEnergy(EnergyType.RGB_111,.5);
            converter.setEnergyConversion(new EnergyConversion(EnergyType.RGB_111,EnergyType.SECONDARY,1));
            world.addCell(converter);world.addCell(observer);
            var grid=new SpatialGridPartitioningStrategy(300,300,60);grid.build(world.getCellList());world.update(.1,grid);
            assertEquals(.008,a.inputs[EnergyInputLayout.ownEnergy(12,EnergyType.SECONDARY)],1e-14);
            assertEquals(.49,a.inputs[EnergyInputLayout.ownEnergy(12,EnergyType.RGB_111)],1e-14);
            int sensed=0;
            for(int i=0;i<12;i++)if(observer.getSensorActors().get(i).getSensedCell()==converter) {
                sensed++;assertEquals(.008,b.inputs[EnergyInputLayout.neighborEnergy(12,i,EnergyType.SECONDARY)],1e-14);
                assertEquals(.49,b.inputs[EnergyInputLayout.neighborEnergy(12,i,EnergyType.RGB_111)],1e-14);
            }
            assertTrue(sensed>0);
        } finally {config.setSunRayRate(sun);}
    }
    @Test void worldCanUseAConvertedReserveToSurviveButABypassedReactionCannotRescueIt() {
        var config=SimulationConfig.getInstance();double sun=config.getSunRayRate();config.setSunRayRate(0);
        try {
            for(boolean enabled:new boolean[]{true,false}) {
                Environment world=new Environment(300,300);Cell cell=cell();cell.setEnergy(0);cell.setEnergy(EnergyType.SECONDARY,.1);
                cell.setEnergyConversion(new EnergyConversion(EnergyType.SECONDARY,EnergyType.PRIMARY,1));world.addCell(cell);
                var grid=new SpatialGridPartitioningStrategy(300,300,60);grid.build(world.getCellList());
                world.update(1.0/60,grid,new SimulationStepPolicy(){
                    public boolean useEnergyConversion(){return enabled;}
                    public boolean keepRemains(){return true;}
                });
                assertEquals(!enabled,cell.isRemains());
                if(enabled)assertTrue(cell.getEnergy()>0);else assertEquals(.1,cell.getEnergy(EnergyType.SECONDARY));
            }
        } finally {config.setSunRayRate(sun);}
    }
    @Test void oldSaveDefaultsToInactiveAndNewSaveRetainsGenesAndAllReserves() throws Exception {
        try(var input=new ObjectInputStream(Objects.requireNonNull(getClass().getResourceAsStream("/energy/single-energy-v1.cells")))) {
            for(Object object:(List<?>)input.readObject())assertEquals(EnergyConversion.NONE,((Cell)object).getEnergyConversion());
        }
        Cell original=CellFactory.createCell(new Vector2D(100,100),10);
        original.setEnergyConversion(new EnergyConversion(EnergyType.RGB_110,EnergyType.RGB_011,.42));
        for(EnergyType type:EnergyType.ALL)original.setEnergy(type,.1*type.getCode());
        var bytes=new ByteArrayOutputStream();try(var out=new ObjectOutputStream(bytes)){out.writeObject(original);}
        try(var in=new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            Cell copy=(Cell)in.readObject();assertEquals(original.getEnergyConversion(),copy.getEnergyConversion());
            for(EnergyType type:EnergyType.ALL)assertEquals(original.getEnergy(type),copy.getEnergy(type));
        }
    }
}
