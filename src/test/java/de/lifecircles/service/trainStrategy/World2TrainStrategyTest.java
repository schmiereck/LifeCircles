package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.*;
import de.lifecircles.service.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** WORLD2: four NONE segments, each shifted by one zone, with all new physics switched on. */
class World2TrainStrategyTest {
    final SimulationConfig config = SimulationConfig.getInstance();
    double width, height, scale, zoneWidth;
    boolean zones, adhesion, bonds, birthBonds, ground, anchors;

    @BeforeEach void save() {
        width = config.getWidth(); height = config.getHeight(); scale = config.getScaleSimulation(); zoneWidth = config.getSunZoneWidth();
        zones = config.isSunSpectralZones(); adhesion = config.isCellAdhesion(); bonds = config.isAdhesionBonds();
        birthBonds = config.isAdhesionBirthBonds(); ground = config.isGroundAdhesion(); anchors = config.isGroundAnchors();
    }
    @AfterEach void restore() {
        config.setWidth(width); config.setHeight(height); config.setScaleSimulation(scale); config.setSunZoneWidth(zoneWidth);
        config.setSunSpectralZones(zones); config.setCellAdhesion(adhesion); config.setAdhesionBonds(bonds);
        config.setAdhesionBirthBonds(birthBonds); config.setGroundAdhesion(ground); config.setGroundAnchors(anchors);
    }

    @Test void worldIsFourSegmentsWideWithRepeatingZonesAndNewPhysics() {
        var strategy = new World2TrainStrategy(new Random(1));
        Environment world = strategy.initializeEnvironment(); strategy.initialize(world);
        assertEquals(4 * 6400, world.getWidth());
        assertEquals(1600, config.getSunZoneWidth());
        assertTrue(config.isSunSpectralZones() && config.isCellAdhesion() && config.isAdhesionBonds()
                && config.isAdhesionBirthBonds() && config.isGroundAdhesion() && config.isGroundAnchors());
        assertSame(SimulationStepPolicy.OPEN_WORLD, strategy.stepPolicy());
        assertSame(World2TrainStrategy.class, CalculationService.strategyFor(TrainMode.WORLD2).getClass());
        assertEquals(config.getInitialCellCount() * 4, world.getCellList().size());

        List<Blocker> blockers = world.getBlockerList();
        assertEquals(1 + 4 * 55, blockers.size());
        for (Blocker b : blockers) if (b.getBlockerType() != Blocker.BlockerType.GROUND) {
            assertTrue(b.getPosition().getX() - b.getWidth() / 2 >= 0, "left edge " + b.getPosition());
            assertTrue(b.getPosition().getX() + b.getWidth() / 2 <= world.getWidth(), "right edge " + b.getPosition());
        }
        // The wide shade roof lies in a different zone in every segment.
        Set<EnergyType> roofLight = new HashSet<>();
        for (Blocker b : blockers) if (b.getWidth() == 800) {
            double x = b.getPosition().getX();
            roofLight.add(SunSpectrum.formAt(x, world.getWidth(), config.getSunZoneWidth(), .5));
        }
        assertEquals(Set.copyOf(SunSpectrum.ZONES), roofLight);
        // Founders spread over all segments.
        for (int segment = 0; segment < 4; segment++) {
            final int s = segment;
            assertTrue(world.getCellList().stream().anyMatch(c -> (int) (c.getPosition().getX() / 6400) == s), "segment " + s);
        }
    }
}
