package de.lifecircles.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Hard-wired spring-field probes (FEDERFELD.md): bone = 9×3 bar, armour = two-layer ring, r = 12 unless stated. */
class SpringFieldTest {
    private SpringFieldLab.Settings settings;
    private static final SpringFieldLab.Field SPRING = SpringFieldLab.Field.spring(.75, 3, 1);
    private static final SpringFieldLab.Field CLASSIC = SpringFieldLab.Field.classic(1);

    @BeforeEach void setUp() { settings = new SpringFieldLab.Settings(); }
    @AfterEach void tearDown() { settings.close(); }

    @Test
    void springIsOffByDefault() {
        SimulationConfig config = SimulationConfig.getInstance();
        assertFalse(config.isSpringAdhesion());
        assertEquals(.75, config.getSpringAdhesionThreshold());
        assertEquals(3, config.getSpringTearFactor());
        assertEquals(36, config.getSpringStiffnessLimit());
    }

    @Test
    void boneHoldsTensionThatTearsTheClassicField() {
        var probe = new SpringFieldLab.Probe("bar", 9, 3, "tension", 600);
        var classic = SpringFieldLab.run(CLASSIC, probe);
        var spring = SpringFieldLab.run(SPRING, probe);
        assertTrue(classic.finalComponent() < .9, classic.csv());
        assertEquals(1, spring.finalComponent(), 1e-9, spring.csv());
        assertTrue(spring.residualShape() < .2, spring.csv());
    }

    @Test
    void boneSpringsBackAfterBending() {
        var probe = new SpringFieldLab.Probe("bar", 9, 3, "bend", 150);
        var classic = SpringFieldLab.run(CLASSIC, probe);
        var spring = SpringFieldLab.run(SPRING, probe);
        assertTrue(classic.residualShape() > 1, classic.csv());
        assertEquals(1, spring.finalComponent(), 1e-9, spring.csv());
        assertTrue(spring.residualShape() < .3, spring.csv());
    }

    @Test
    void twoLayerArmourHoldsTension() {
        var probe = new SpringFieldLab.Probe("ring", 24, 2, "tension", 600);
        var classic = SpringFieldLab.run(CLASSIC, probe);
        var spring = SpringFieldLab.run(SPRING, probe);
        assertTrue(classic.finalComponent() < .5, classic.csv());
        assertEquals(1, spring.finalComponent(), 1e-9, spring.csv());
        assertTrue(spring.residualShape() < 1, spring.csv());
    }

    @Test
    void attractionBelowTheThresholdStaysClassic() {
        var probe = new SpringFieldLab.Probe("bar", 9, 3, "tension", 300);
        var classic = SpringFieldLab.run(SpringFieldLab.Field.classic(.6), probe);
        var below = SpringFieldLab.run(SpringFieldLab.Field.spring(.75, 3, .6), probe);
        var above = SpringFieldLab.run(SpringFieldLab.Field.spring(.5, 3, .6), probe);
        assertEquals(classic.peakShape(), below.peakShape(), 0, below.csv());
        assertEquals(classic.residualShape(), below.residualShape(), 0, below.csv());
        assertNotEquals(classic.peakShape(), above.peakShape(), above.csv());
    }

    @Test
    void stiffnessCapKeepsLargeCellsStableAt60Hz() {
        var probe = new SpringFieldLab.Probe("bar", 9, 3, "none", 0, 20, 1.0 / 60);
        var capped = SpringFieldLab.run(SPRING, probe);
        var raw = SpringFieldLab.run(SPRING.uncapped(), probe);
        assertEquals(1, capped.finalComponent(), 1e-9, capped.csv());
        assertTrue(capped.maxOverlap() < .15, capped.csv());
        assertTrue(raw.finalComponent() < .9 || raw.maxOverlap() > .3, raw.csv());
    }
}
