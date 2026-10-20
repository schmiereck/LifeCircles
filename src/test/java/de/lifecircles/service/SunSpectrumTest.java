package de.lifecircles.service;

import de.lifecircles.model.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class SunSpectrumTest {
    static final double W=400, ZONE=W/4, HALF=SunSpectrum.TRANSITION_FRACTION/2*ZONE;
    final SimulationConfig config=SimulationConfig.getInstance();
    double rate,spacing,rayEnergy,zoneWidth;boolean zones;
    @BeforeEach void save(){rate=config.getSunRayRate();spacing=config.getSunRaySpacingPx();rayEnergy=config.getEnergyPerRay();zones=config.isSunSpectralZones();zoneWidth=config.getSunZoneWidth();}
    @AfterEach void restore(){config.setSunRayRate(rate);config.setSunRaySpacingPx(spacing);config.setEnergyPerRay(rayEnergy);config.setSunSpectralZones(zones);config.setSunZoneWidth(zoneWidth);}

    @Test void zoneCentersAndOuterBorderEdgesArePure() {
        for(int zone=0;zone<4;zone++) {
            EnergyType type=SunSpectrum.ZONES.get(zone);
            for(double x:new double[]{zone*ZONE+HALF,(zone+.5)*ZONE,(zone+1)*ZONE-HALF-1e-9})
                for(double draw:new double[]{0,.5,.999}) {
                    assertEquals(type,SunSpectrum.formAt(x,W,draw),"x="+x);
                    assertEquals(1,SunSpectrum.share(x,W,zone),1e-12);
                }
        }
    }
    @Test void bordersMixLinearlyAndWrapPeriodically() {
        assertEquals(.5,SunSpectrum.share(ZONE,W,0),1e-12);assertEquals(.5,SunSpectrum.share(ZONE,W,1),1e-12);
        assertEquals(.75,SunSpectrum.share(ZONE+HALF/2,W,1),1e-12);
        assertEquals(EnergyType.SECONDARY,SunSpectrum.formAt(ZONE,W,.49));assertEquals(EnergyType.PRIMARY,SunSpectrum.formAt(ZONE,W,.51));
        // x=0 is the border between 100 (last zone) and 000 (first zone).
        assertEquals(.5,SunSpectrum.share(0,W,0),1e-12);assertEquals(.5,SunSpectrum.share(0,W,3),1e-12);
        assertEquals(.5,SunSpectrum.share(W,W,3),1e-12);
        for(double x=-W;x<2*W;x+=3.7) {
            double sum=0;for(int z=0;z<4;z++)sum+=SunSpectrum.share(x,W,z);
            assertEquals(1,sum,1e-12);
            assertEquals(SunSpectrum.formAt(x+W,W,.3),SunSpectrum.formAt(x,W,.3));
        }
    }
    @Test void fixedZoneWidthRepeatsTheCycleAlongTheWorld() {
        double world=4*W, zone=ZONE;  // four cycles of 400 px
        assertEquals(ZONE,SunSpectrum.zoneWidth(W,0));assertEquals(250,SunSpectrum.zoneWidth(W,250));
        for(int cycle=0;cycle<4;cycle++)for(int z=0;z<4;z++){
            double center=cycle*W+(z+.5)*zone;
            assertEquals(SunSpectrum.ZONES.get(z),SunSpectrum.formAt(center,world,zone,.3),"cycle "+cycle+" zone "+z);
            assertEquals(1,SunSpectrum.share(center,world,zone,z),1e-12);assertEquals(z,SunSpectrum.zoneIndex(center,world,zone));
        }
        // Borders between cycles mix 100 and 000 like the world edge.
        assertEquals(.5,SunSpectrum.share(W,world,zone,0),1e-12);assertEquals(.5,SunSpectrum.share(W,world,zone,3),1e-12);
        assertEquals(.5,SunSpectrum.share(0,world,zone,3),1e-12);assertEquals(.5,SunSpectrum.share(world,world,zone,0),1e-12);
        for(double x=-world;x<2*world;x+=7.3){
            double sum=0;for(int z=0;z<4;z++)sum+=SunSpectrum.share(x,world,zone,z);
            assertEquals(1,sum,1e-12);
            assertEquals(SunSpectrum.formAt(x+W,world,zone,.3),SunSpectrum.formAt(x,world,zone,.3));
        }
        // The default overloads keep one cycle per world.
        for(double x=0;x<W;x+=11)assertEquals(SunSpectrum.formAt(x,W,.4),SunSpectrum.formAt(x,W,ZONE,.4));
        assertThrows(IllegalArgumentException.class,()->SunSpectrum.share(1,W,0,0));
        assertThrows(IllegalArgumentException.class,()->config.setSunZoneWidth(-1));
    }
    @Test void sunUsesTheConfiguredZoneWidth() {
        config.setSunRayRate(40);config.setSunRaySpacingPx(10);config.setEnergyPerRay(.001);config.setSunSpectralZones(true);config.setSunZoneWidth(ZONE/2);
        // With half-width zones the third zone of the world-wide cycle (x 200..300) holds 000 and 001.
        Cell cell=new Cell(new Vector2D(2.25*ZONE,250),10,new PhysicsRegressionTest.ConstantBrain());cell.setEnergy(0);cell.setEnergy(EnergyType.SECONDARY,0);
        var sun=new EnergySunCalcService(new Random(11));
        for(int i=0;i<200;i++)sun.calculateSunEnergy(List.of(cell),List.of(),W,300,config,.05);
        assertTrue(cell.getEnergy(EnergyType.PRIMARY)>0);
        assertEquals(cell.getTotalEnergy(),cell.getEnergy(EnergyType.PRIMARY),1e-12);
    }
    @Test void drawnFormsFollowTheSharesInsideAMixedBorder() {
        Random random=new Random(5);double x=2*ZONE+HALF/3;int[] counts=new int[8];int n=40_000;
        for(int i=0;i<n;i++)counts[SunSpectrum.formAt(x,W,random.nextDouble()).getCode()]++;
        assertEquals(SunSpectrum.share(x,W,2),counts[EnergyType.RGB_010.getCode()]/(double)n,.01);
        assertEquals(SunSpectrum.share(x,W,1),counts[EnergyType.SECONDARY.getCode()]/(double)n,.01);
    }
    @Test void absorbedRaysFillOnlyTheLocalZoneFormWithUnchangedRayEnergy() {
        config.setSunRayRate(40);config.setSunRaySpacingPx(10);config.setEnergyPerRay(.001);config.setSunSpectralZones(true);
        List<Cell> cells=new ArrayList<>();
        for(int zone=0;zone<4;zone++){
            Cell cell=new Cell(new Vector2D((zone+.5)*ZONE,250),10,new PhysicsRegressionTest.ConstantBrain());
            cell.setEnergy(0);cell.setEnergy(EnergyType.SECONDARY,0);cells.add(cell);
        }
        var sun=new EnergySunCalcService(new Random(11));int absorbed=0;
        for(int i=0;i<400;i++)
            for(SunRay ray:sun.calculateSunEnergy(cells,List.of(),W,300,config,.05))
                if(cells.stream().anyMatch(c->Math.abs(c.getPosition().getX()-ray.getEnd().getX())<11 && Math.abs(c.getPosition().getY()-ray.getEnd().getY())<11))absorbed++;
        double total=0;
        for(int zone=0;zone<4;zone++){
            Cell cell=cells.get(zone);EnergyType own=SunSpectrum.ZONES.get(zone);
            assertTrue(cell.getEnergy(own)>0,"zone "+zone);
            for(EnergyType other:EnergyType.ALL)if(other!=own)assertEquals(0,cell.getEnergy(other),"zone "+zone+" form "+other);
            total+=cell.getTotalEnergy();
        }
        assertEquals(absorbed*.001,total,1e-9);
    }
    @Test void zonesOffKeepsTheSingleConfiguredForm() {
        config.setSunRayRate(40);config.setSunRaySpacingPx(10);config.setEnergyPerRay(.001);config.setSunSpectralZones(false);
        Cell cell=new Cell(new Vector2D(2.5*ZONE,250),10,new PhysicsRegressionTest.ConstantBrain());cell.setEnergy(0);cell.setEnergy(EnergyType.SECONDARY,0);
        var sun=new EnergySunCalcService(new Random(11));
        for(int i=0;i<200;i++)for(SunRay ray:sun.calculateSunEnergy(List.of(cell),List.of(),W,300,config,.05))assertEquals(config.getSunEnergyType(),ray.getEnergyType());
        assertEquals(cell.getTotalEnergy(),cell.getEnergy(config.getSunEnergyType()));assertTrue(cell.getTotalEnergy()>0);
    }
}
