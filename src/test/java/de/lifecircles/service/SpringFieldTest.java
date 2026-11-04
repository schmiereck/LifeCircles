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
        assertFalse(config.isSpringPreload());
        assertEquals(0, config.getSpringDamping());
        assertEquals(0, config.getSpringBending());
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

    /** Two spring cells oscillating against each other: returns {relative speed after 3 s, |momentum drift|}. */
    private static double[] pairOscillation(double damping) {
        MechanicsLab.configure(new MechanicsLab.Parameters("balanced", 90, 125, .8, 3, .25));
        SimulationConfig config = SimulationConfig.getInstance();
        config.setSpringAdhesion(true); config.setSpringAdhesionThreshold(.75); config.setSpringDamping(damping);
        var world = new de.lifecircles.model.Environment(4000, 4000);
        var a = SpringFieldLab.add(world, 2000, 2000, 12);
        var b = SpringFieldLab.add(world, 2024, 2000, 12);
        SpringFieldLab.attract(a, 1); SpringFieldLab.attract(b, 1);
        a.setVelocity(new de.lifecircles.model.Vector2D(-6, 1)); b.setVelocity(new de.lifecircles.model.Vector2D(6, -1));
        var grid = new de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy(4000, 4000, 80);
        var cells = world.getCellList();
        for (int step = 0; step < 180; step++) {
            for (var c : cells) c.applyForce(SimulationConfig.GRAVITY_VECTOR.multiply(-1));
            grid.build(cells); world.update(1.0 / 60, grid);
        }
        var momentum = a.getVelocity().add(b.getVelocity());
        return new double[]{a.getVelocity().subtract(b.getVelocity()).length(), momentum.length()};
    }

    @Test
    void springDampingIsSymmetricAndDissipative() {
        double[] free = pairOscillation(0), damped = pairOscillation(.05);
        assertEquals(0, damped[1], 1e-6, "damping must keep actio = reactio");
        assertTrue(damped[0] < free[0], "free=" + free[0] + " damped=" + damped[0]);
    }

    /** Pair with cell a twisted by 10°: returns {|a offset from the 30° actor grid| after 2 s, |b offset|, |momentum|}. */
    private static double[] twistedPair(double bending) {
        MechanicsLab.configure(new MechanicsLab.Parameters("balanced", 90, 125, .8, 3, .25));
        SimulationConfig config = SimulationConfig.getInstance();
        config.setSpringAdhesion(true); config.setSpringAdhesionThreshold(.75); config.setSpringBending(bending);
        var world = new de.lifecircles.model.Environment(4000, 4000);
        var a = SpringFieldLab.add(world, 2000, 2000, 12);
        var b = SpringFieldLab.add(world, 2024, 2000, 12);
        SpringFieldLab.attract(a, 1); SpringFieldLab.attract(b, 1);
        a.setRotation(Math.toRadians(10));
        var grid = new de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy(4000, 4000, 80);
        var cells = world.getCellList();
        for (int step = 0; step < 120; step++) {
            for (var c : cells) c.applyForce(SimulationConfig.GRAVITY_VECTOR.multiply(-1));
            grid.build(cells); world.update(1.0 / 60, grid);
            if (Boolean.getBoolean("life.twistTrace") && step % 10 == 0)
                System.out.printf(java.util.Locale.ROOT, "TWIST k=%.0f step=%d a=%.4f b=%.4f bond=%.4f%n", bending, step,
                        Math.toDegrees(a.getRotation()), Math.toDegrees(b.getRotation()),
                        Math.toDegrees(Math.atan2(b.getPosition().getY() - a.getPosition().getY(), b.getPosition().getX() - a.getPosition().getX())));
        }
        double off = Math.abs(Math.IEEEremainder(a.getRotation(), Math.PI / 6));
        double offB = Math.abs(Math.IEEEremainder(b.getRotation(), Math.PI / 6));
        return new double[]{off, offB, a.getVelocity().add(b.getVelocity()).length()};
    }

    @Test
    void bendingSpringTurnsATwistedCellBack() {
        double free = twistedPair(0)[0];
        double[] bent = twistedPair(1000);
        assertTrue(bent[0] < free / 2, "free=" + Math.toDegrees(free) + "° bent=" + Math.toDegrees(bent[0]) + "°");
        assertEquals(0, bent[2], 1e-6, "angular spring must keep actio = reactio");
    }

    @Test
    void preloadKeepsTheBoneIntact() {
        SimulationConfig.getInstance().setSpringPreload(true);
        var spring = SpringFieldLab.run(SPRING, new SpringFieldLab.Probe("bar", 9, 3, "tension", 600));
        assertEquals(1, spring.finalComponent(), 1e-9, spring.csv());
        assertTrue(spring.residualShape() < .2, spring.csv());
    }

    @Test
    void springSolverIsOffByDefault() {
        SimulationConfig config = SimulationConfig.getInstance();
        assertFalse(config.isSpringSolver());
        assertEquals(Double.POSITIVE_INFINITY, config.getSpringSolverStiffness());
        assertEquals(8, config.getSpringSolverIterations());
    }

    @Test
    void solverHingeClosesTheGapAndKeepsMomentum() {
        MechanicsLab.configure(new MechanicsLab.Parameters("balanced", 90, 125, .8, 3, .25));
        var world = new de.lifecircles.model.Environment(4000, 4000);
        var a = SpringFieldLab.add(world, 2000, 2000, 12);
        var b = SpringFieldLab.add(world, 2030, 2004, 12);
        a.setRotation(.3);
        a.setVelocity(new de.lifecircles.model.Vector2D(2, -1)); b.setVelocity(new de.lifecircles.model.Vector2D(-1, 3));
        double r = a.getRadiusSize();
        var link = new SpringLinkSolver.Link(a, r, 0, b, -r, 0, 1e9, 1e9);
        var before = a.getVelocity().add(b.getVelocity());
        SpringLinkSolver.solve(java.util.List.of(link), 4000, 4000, 1.0 / 60, Double.POSITIVE_INFINITY, 8);
        double ca = Math.cos(a.getRotation()), sa = Math.sin(a.getRotation()), cb = Math.cos(b.getRotation()), sb = Math.sin(b.getRotation());
        double gap = Math.hypot(b.getPosition().getX() - r * cb - a.getPosition().getX() - r * ca,
                b.getPosition().getY() - r * sb - a.getPosition().getY() - r * sa);
        assertEquals(0, gap, 1e-6, "rigid hinge must close");
        assertEquals(0, a.getVelocity().add(b.getVelocity()).subtract(before).length(), 1e-9, "actio = reactio");
        // Capped pull: the hinge slips and only moves by the tear force.
        var c = SpringFieldLab.add(world, 2100, 2000, 12);
        var d = SpringFieldLab.add(world, 2130, 2000, 12);
        SpringLinkSolver.solve(java.util.List.of(new SpringLinkSolver.Link(c, r, 0, d, -r, 0, 1e9, 1)), 4000, 4000,
                1.0 / 60, Double.POSITIVE_INFINITY, 8);
        assertEquals(2 * SpringLinkSolver.W_LINEAR / 3600, 30 - (d.getPosition().getX() - c.getPosition().getX()), 1e-9);
    }

    @Test
    void solverBoneHoldsTension() {
        SimulationConfig.getInstance().setSpringSolver(true);
        var spring = SpringFieldLab.run(SPRING, new SpringFieldLab.Probe("bar", 9, 3, "tension", 600));
        assertEquals(1, spring.finalComponent(), 1e-9, spring.csv());
    }
}
