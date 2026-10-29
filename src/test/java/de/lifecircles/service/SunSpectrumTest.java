package de.lifecircles.service;

import de.lifecircles.model.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class SunSpectrumTest {
    static final double W=400, ZONE=W/4, HALF=SunSpectrum.TRANSITION_FRACTION/2*ZONE;
    final SimulationConfig config=SimulationConfig.getInstance();
    double rate,spacing,rayEnergy,zoneWidth,background,transition,softness;boolean zones;
    @BeforeEach void save(){rate=config.getSunRayRate();spacing=config.getSunRaySpacingPx();rayEnergy=config.getEnergyPerRay();zones=config.isSunSpectralZones();zoneWidth=config.getSunZoneWidth();
        background=config.getSunZoneBackground();transition=config.getSunZoneTransition();softness=config.getSunZoneSoftness();}
    @AfterEach void restore(){config.setSunRayRate(rate);config.setSunRaySpacingPx(spacing);config.setEnergyPerRay(rayEnergy);config.setSunSpectralZones(zones);config.setSunZoneWidth(zoneWidth);
        config.setSunZoneBackground(background);config.setSunZoneTransition(transition);config.setSunZoneSoftness(softness);}

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
        // The dominant form takes the lowest draws (on the border the left one), the others follow in zone order.
        assertEquals(EnergyType.PRIMARY,SunSpectrum.formAt(ZONE,W,.49));assertEquals(EnergyType.SECONDARY,SunSpectrum.formAt(ZONE,W,.51));
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
        for(int i=0;i<300;i++) // below full stores (1.0 per form)
            for(SunRay ray:sun.calculateSunEnergy(cells,List.of(),W,300,config,.05))
                if(cells.stream().anyMatch(c->Math.abs(Math.IEEEremainder(c.getPosition().getX()-ray.getEnd().getX(),W))<11 && Math.abs(c.getPosition().getY()-ray.getEnd().getY())<11))absorbed++;
        double total=0;
        for(int zone=0;zone<4;zone++){
            Cell cell=cells.get(zone);EnergyType own=SunSpectrum.ZONES.get(zone);
            assertTrue(cell.getEnergy(own)>0,"zone "+zone);
            for(EnergyType other:EnergyType.ALL)if(other!=own)assertEquals(0,cell.getEnergy(other),"zone "+zone+" form "+other);
            total+=cell.getTotalEnergy();
        }
        assertEquals(absorbed*.001,total,1e-9);
    }
    /** SPEKTRALZONEN.md (29.10.2026): background share, border width and border curve. */
    @Test void backgroundBorderWidthAndCurveAreAdjustable() {
        var mix=new SunSpectrum.Mix(.02,.5,0);
        // A zone center holds every foreign form at the background share; the own form keeps the rest.
        for(int z=0;z<4;z++)assertEquals(z==1?.94:.02,SunSpectrum.share(1.5*ZONE,W,ZONE,z,mix),1e-12);
        // Half a zone wide border: a quarter zone each side; linear from 0 to 1 of what the background leaves.
        assertEquals(.02+.92*.5,SunSpectrum.share(ZONE,W,ZONE,1,mix),1e-12);
        assertEquals(.02+.92*.75,SunSpectrum.share(ZONE+ZONE/8,W,ZONE,1,mix),1e-12);
        assertEquals(.94,SunSpectrum.share(ZONE+ZONE/4,W,ZONE,1,mix),1e-12);
        // The S-curve lets the neighbor reach in less deeply but meets it at 50:50 on the border.
        var soft=new SunSpectrum.Mix(0,.5,1);
        assertEquals(.5,SunSpectrum.share(ZONE,W,ZONE,1,soft),1e-12);
        assertEquals(.103515625,SunSpectrum.share(ZONE-ZONE/8,W,ZONE,1,soft),1e-12);  // a quarter into the border
        assertTrue(SunSpectrum.share(ZONE-ZONE/8,W,ZONE,1,soft)<SunSpectrum.share(ZONE-ZONE/8,W,ZONE,1,new SunSpectrum.Mix(0,.5,0)));
        // A border as wide as a zone: pure only exactly at the centers, so the light changes everywhere.
        var full=new SunSpectrum.Mix(0,1,0);
        assertEquals(1,SunSpectrum.share(1.5*ZONE,W,ZONE,1,full),1e-12);
        assertEquals(.9,SunSpectrum.share(1.4*ZONE,W,ZONE,1,full),1e-12);
        // Width 0 is a hard edge; background 1/4 is uniform light.
        assertEquals(1,SunSpectrum.share(ZONE+1e-9,W,ZONE,1,new SunSpectrum.Mix(0,0,0)),1e-12);
        for(int z=0;z<4;z++)assertEquals(.25,SunSpectrum.share(37,W,ZONE,z,new SunSpectrum.Mix(.25,.15,0)),1e-12);
        Random random=new Random(3);
        for(var m:List.of(mix,soft,full))for(double x=-W;x<2*W;x+=5.3){
            double sum=0;for(int z=0;z<4;z++)sum+=SunSpectrum.share(x,W,ZONE,z,m);
            assertEquals(1,sum,1e-12);
            SunSpectrum.formAt(x,W,ZONE,random.nextDouble(),m);
        }
        // Drawn forms follow the shares, also the background in a zone center.
        int[] counts=new int[8];int n=100_000;
        for(int i=0;i<n;i++)counts[SunSpectrum.formAt(2.5*ZONE,W,ZONE,random.nextDouble(),mix).getCode()]++;
        for(int z=0;z<4;z++)assertEquals(SunSpectrum.share(2.5*ZONE,W,ZONE,z,mix),counts[SunSpectrum.ZONES.get(z).getCode()]/(double)n,.003);
        assertEquals(EnergyType.RGB_010,SunSpectrum.dominantAt(2.5*ZONE,W,ZONE,mix));
        for(double invalid:new double[]{-.01,.26,Double.NaN})assertThrows(IllegalArgumentException.class,()->config.setSunZoneBackground(invalid));
        for(double invalid:new double[]{-.01,1.01})assertThrows(IllegalArgumentException.class,()->config.setSunZoneTransition(invalid));
        for(double invalid:new double[]{-.01,1.01})assertThrows(IllegalArgumentException.class,()->config.setSunZoneSoftness(invalid));
        assertEquals(SunSpectrum.Mix.DEFAULT,SunSpectrum.Mix.of(config),"defaults keep the former narrow linear borders");
    }
    @Test void absorbedRaysCarryTheBackgroundFormsToo() {
        config.setSunRayRate(40);config.setSunRaySpacingPx(10);config.setEnergyPerRay(.0002);config.setSunSpectralZones(true);config.setSunZoneBackground(.05);
        Cell cell=new Cell(new Vector2D(2.5*ZONE,250),10,new PhysicsRegressionTest.ConstantBrain());cell.setEnergy(0);cell.setEnergy(EnergyType.SECONDARY,0);
        var sun=new EnergySunCalcService(new Random(11));
        for(int i=0;i<600;i++)sun.calculateSunEnergy(List.of(cell),List.of(),W,300,config,.05);
        double total=cell.getTotalEnergy();
        assertTrue(total<.9*cell.getMaxEnergy(),"no store full: "+total);
        for(int z=0;z<4;z++)assertEquals(z==2?.85:.05,cell.getEnergy(SunSpectrum.ZONES.get(z))/total,.02,"zone form "+z);
    }
    @Test void zonesOffKeepsTheSingleConfiguredForm() {
        config.setSunRayRate(40);config.setSunRaySpacingPx(10);config.setEnergyPerRay(.001);config.setSunSpectralZones(false);
        Cell cell=new Cell(new Vector2D(2.5*ZONE,250),10,new PhysicsRegressionTest.ConstantBrain());cell.setEnergy(0);cell.setEnergy(EnergyType.SECONDARY,0);
        var sun=new EnergySunCalcService(new Random(11));
        for(int i=0;i<200;i++)for(SunRay ray:sun.calculateSunEnergy(List.of(cell),List.of(),W,300,config,.05))assertEquals(config.getSunEnergyType(),ray.getEnergyType());
        assertEquals(cell.getTotalEnergy(),cell.getEnergy(config.getSunEnergyType()));assertTrue(cell.getTotalEnergy()>0);
    }
}
