package de.lifecircles.service;

import de.lifecircles.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MultiEnergySunTest {
    private final SimulationConfig config=SimulationConfig.getInstance();
    private double rate,spacing,rayEnergy;
    private EnergyType previousType;
    @BeforeEach void setup() {
        rate=config.getSunRayRate();spacing=config.getSunRaySpacingPx();rayEnergy=config.getEnergyPerRay();previousType=config.getSunEnergyType();
        config.setSunRayRate(10);config.setSunRaySpacingPx(10);config.setEnergyPerRay(.01);
    }
    @AfterEach void restore() {
        config.setSunRayRate(rate);config.setSunRaySpacingPx(spacing);config.setEnergyPerRay(rayEnergy);config.setSunEnergyType(previousType);
    }
    private Cell cell() {
        // The middle ray starts at the sky's center (x=150) and drifts right in the morning sun (35 degrees).
        Cell c=new Cell(new Vector2D(150+200*Math.tan(Math.toRadians(35)),200),10,new PhysicsRegressionTest.ConstantBrain());
        c.setEnergy(.2);c.setEnergy(EnergyType.SECONDARY,.3);return c;
    }
    private EnergySunCalcService sun() {return new EnergySunCalcService(new Random(1){@Override public double nextDouble(){return .5;}});}
    @ParameterizedTest @EnumSource(EnergyType.class)
    void raysFillOnlyTheChosenStoreWithoutDuplicatingTheirEnergy(EnergyType type) {
        Cell c=cell();config.setSunEnergyType(type);
        var rays=sun().calculateSunEnergy(List.of(c),List.of(),300,300,config,.01);
        assertFalse(rays.isEmpty());assertTrue(c.isSunRayHit());
        assertEquals(.2+(type==EnergyType.PRIMARY?rays.size()*.01:0),c.getEnergy(),1e-14);
        assertEquals(.3+(type==EnergyType.SECONDARY?rays.size()*.01:0),c.getEnergy(EnergyType.SECONDARY),1e-14);
        assertEquals(.5+rays.size()*.01,c.getTotalEnergy(),1e-14);
    }
    @Test void fullSelectedStoreDoesNotSpillIntoTheOtherForm() {
        Cell c=cell();c.setEnergy(1);c.setEnergy(EnergyType.SECONDARY,0);config.setSunEnergyType(EnergyType.PRIMARY);
        sun().calculateSunEnergy(List.of(c),List.of(),300,300,config,.01);
        assertEquals(1,c.getEnergy());assertEquals(0,c.getEnergy(EnergyType.SECONDARY));
    }
    @Test void liveSourceChangeTakesEffectInTheNextBatch() {
        Cell c=cell();var sun=sun();config.setSunEnergyType(EnergyType.PRIMARY);
        sun.calculateSunEnergy(List.of(c),List.of(),300,300,config,.01);
        double primary=c.getEnergy();assertTrue(primary>.2);assertEquals(.3,c.getEnergy(EnergyType.SECONDARY));
        config.setSunEnergyType(EnergyType.SECONDARY);sun.calculateSunEnergy(List.of(c),List.of(),300,300,config,.01);
        assertEquals(primary,c.getEnergy());assertTrue(c.getEnergy(EnergyType.SECONDARY)>.3);
        assertThrows(NullPointerException.class,()->config.setSunEnergyType(null));
    }
    @Test void sourceIsCapturedBeforeEmittingTheBatchEvenIfConfigChangesDuringRayGeneration() {
        Cell c=cell();config.setSunEnergyType(EnergyType.PRIMARY);
        var sun=new EnergySunCalcService(new Random(1){@Override public double nextDouble(){config.setSunEnergyType(EnergyType.SECONDARY);return .5;}});
        sun.calculateSunEnergy(List.of(c),List.of(),300,300,config,.01);
        assertTrue(c.getEnergy()>.2);assertEquals(.3,c.getEnergy(EnergyType.SECONDARY));
        assertEquals(EnergyType.SECONDARY,config.getSunEnergyType());
    }
}
