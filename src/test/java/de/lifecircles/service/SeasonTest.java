package de.lifecircles.service;

import de.lifecircles.model.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class SeasonTest {
    final SimulationConfig config=SimulationConfig.getInstance();
    double duration,winter,rate;
    @BeforeEach void save(){duration=config.getSeasonDuration();winter=config.getWinterIntensity();rate=config.getSunRayRate();}
    @AfterEach void restore(){config.setSeasonDuration(duration);config.setWinterIntensity(winter);config.setSunRayRate(rate);}

    @Test void yearRunsSmoothlyFromSummerToWinterFactor() {
        assertEquals(1,EnergySunCalcService.seasonFactor(0,.25),1e-15);
        assertEquals(.25,EnergySunCalcService.seasonFactor(.5,.25),1e-15);
        assertEquals(.625,EnergySunCalcService.seasonFactor(.25,.25),1e-15);
        assertEquals(1,EnergySunCalcService.seasonFactor(.5,1),1e-15);
        assertEquals(0,EnergySunCalcService.seasonFactor(.5,0),1e-15);
    }
    double intensityAfter(double seconds,double dt) {
        var sun=new EnergySunCalcService(new Random(3));
        for(double t=0;t<seconds-1e-9;t+=dt)sun.calculateSunEnergy(List.of(),List.of(),400,300,config,dt);
        return sun.getCurrentIntensity();
    }
    @Test void midWinterScalesTheDayNightIntensityAndOffIsUnchanged() {
        config.setSeasonDuration(0);double plain=intensityAfter(30,.5);
        assertTrue(plain>.4 && plain<=1);
        config.setSeasonDuration(60);config.setWinterIntensity(.25);
        assertEquals(plain*.25,intensityAfter(30,.5),1e-12);
        config.setSeasonDuration(120);
        assertEquals(plain*.625,intensityAfter(30,.5),1e-12);
    }
    @Test void seasonsReduceDeliveredSunlightOnACell() {
        config.setSunRayRate(40);
        double[] energy=new double[2];
        for(int run=0;run<2;run++) {
            config.setSeasonDuration(run==0?0:20);config.setWinterIntensity(0);
            var sun=new EnergySunCalcService(new Random(9));
            Cell cell=new Cell(new Vector2D(200,250),40,new PhysicsRegressionTest.ConstantBrain());cell.setEnergy(0);cell.setEnergy(EnergyType.SECONDARY,0);
            for(int i=0;i<200;i++){cell.setEnergy(0);sun.calculateSunEnergy(List.of(cell),List.of(),400,300,config,.05);energy[run]+=cell.getEnergy();}
        }
        assertTrue(energy[1]<energy[0]*.7,Arrays.toString(energy));
    }
    @Test void liveLengthChangeKeepsTheCurrentPhase() {
        config.setSeasonDuration(100);var sun=new EnergySunCalcService(new Random(1));
        for(int i=0;i<25;i++)sun.calculateSunEnergy(List.of(),List.of(),400,300,config,1);
        assertEquals(.25,sun.getSeasonPhase(),1e-12);
        config.setSeasonDuration(1000);sun.calculateSunEnergy(List.of(),List.of(),400,300,config,10);
        assertEquals(.26,sun.getSeasonPhase(),1e-12);
    }
    @Test void invalidSettingsAreRejected() {
        assertThrows(IllegalArgumentException.class,()->config.setSeasonDuration(-1));
        assertThrows(IllegalArgumentException.class,()->config.setSeasonDuration(Double.NaN));
        assertThrows(IllegalArgumentException.class,()->config.setWinterIntensity(1.1));
        assertThrows(IllegalArgumentException.class,()->config.setWinterIntensity(-.1));
    }
    @Test void historyIsSampledInSimulationTimeWithMeanSunPower() {
        var stats=StatisticsManagerService.getInstance();
        List<Cell> cells=new ArrayList<>(List.of(new Cell(new Vector2D(1,1),5,new PhysicsRegressionTest.ConstantBrain())));
        Cell remains=new Cell(new Vector2D(9,9),5,new PhysicsRegressionTest.ConstantBrain());remains.becomeRemains();cells.add(remains);
        stats.reset(cells);
        for(int i=0;i<49;i++)stats.update(cells,i<25?2:0,.1);
        assertTrue(stats.getTotalHistory().isEmpty());
        stats.update(cells,0,.1);
        assertEquals(List.of(1),stats.getTotalHistory());assertEquals(1,stats.getSunPowerHistory().get(0),1e-12);
        for(int i=0;i<StatisticsManagerService.HISTORY_SIZE*50+10;i++)stats.update(cells,3,.1);
        assertEquals(StatisticsManagerService.HISTORY_SIZE,stats.getTotalHistory().size());
        assertEquals(StatisticsManagerService.HISTORY_SIZE,stats.getSunPowerHistory().size());
        assertEquals(3,stats.getSunPowerHistory().getLast(),1e-12);
        stats.reset(List.of());assertTrue(stats.getSunPowerHistory().isEmpty());
    }
}
