package de.lifecircles.service;

import de.lifecircles.model.EnergyType;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** GUI environment settings travel beside a cells file and come back exactly. */
class SimulationSettingsFileTest {
    final SimulationConfig config = SimulationConfig.getInstance();
    @TempDir Path dir;
    Path stash;

    @BeforeEach void save() throws Exception { stash = dir.resolve("stash.cells"); SimulationSettingsFile.save(stash.toString(), config, 0); }
    @AfterEach void restore() throws Exception { assertEquals(List.of(), SimulationSettingsFile.load(stash.toString(), config).problems()); }

    @Test void settingsRoundTripBesideTheCellsFile() throws Exception {
        String cells = dir.resolve("run.cells").toString();
        config.setSunRayRate(37.5); config.setSunEnergyType(EnergyType.ALL.get(3)); config.setSunSpectralZones(true);
        config.setSunZoneWidth(1600); config.setSeasonDuration(900); config.setWinterIntensity(.3);
        config.setCellAdhesion(false); config.setTrainingAdhesion(true); config.setAdhesionBonds(true); config.setAdhesionBirthBonds(true);
        config.setRemainsForceHalfLife(Double.POSITIVE_INFINITY); config.setAdhesionRejection(.7);
        config.setGroundAdhesion(true); config.setGroundAnchors(true); config.setGroundAnchorsGroundOnly(true); config.setGroundStickiness(-12.25);
        config.setGravity(44.145); config.setViscosity(1.75); config.setRotationalFriction(0.3);
        config.setBlockerFriction(true); config.setBlockerFrictionCoefficient(1.25); config.setBlockerRollingResistance(.375);
        SimulationSettingsFile.save(cells, config, 12_345_678_901L);
        assertTrue(Files.exists(Path.of(cells + ".settings")));

        config.setSunRayRate(10); config.setSunEnergyType(EnergyType.PRIMARY); config.setSunSpectralZones(false);
        config.setSunZoneWidth(0); config.setSeasonDuration(0); config.setWinterIntensity(.25);
        config.setCellAdhesion(true); config.setTrainingAdhesion(false); config.setAdhesionBonds(false); config.setAdhesionBirthBonds(false);
        config.setRemainsForceHalfLife(300); config.setAdhesionRejection(.5);
        config.setGroundAdhesion(false); config.setGroundAnchors(false); config.setGroundAnchorsGroundOnly(false); config.setGroundStickiness(0);
        config.setGravity(SimulationConfig.GRAVITY); config.setViscosity(.8); config.setRotationalFriction(.8);
        config.setBlockerFriction(false); config.setBlockerFrictionCoefficient(.5); config.setBlockerRollingResistance(.5);

        SimulationSettingsFile.Loaded loaded = SimulationSettingsFile.load(cells, config);
        assertEquals(List.of(), loaded.problems());
        assertEquals(12_345_678_901L, loaded.steps());
        assertEquals(37.5, config.getSunRayRate()); assertEquals(EnergyType.ALL.get(3), config.getSunEnergyType());
        assertTrue(config.isSunSpectralZones()); assertEquals(1600, config.getSunZoneWidth());
        assertEquals(900, config.getSeasonDuration()); assertEquals(.3, config.getWinterIntensity());
        assertFalse(config.isCellAdhesion()); assertTrue(config.isTrainingAdhesion());
        assertTrue(config.isAdhesionBonds()); assertTrue(config.isAdhesionBirthBonds());
        assertEquals(Double.POSITIVE_INFINITY, config.getRemainsForceHalfLife()); assertEquals(.7, config.getAdhesionRejection());
        assertTrue(config.isGroundAdhesion()); assertTrue(config.isGroundAnchors()); assertTrue(config.isGroundAnchorsGroundOnly());
        assertEquals(-12.25, config.getGroundStickiness());
        assertEquals(44.145, config.getGravity()); assertEquals(1.75, config.getViscosity()); assertEquals(0.3, config.getRotationalFriction());
        assertTrue(config.isBlockerFriction()); assertEquals(1.25, config.getBlockerFrictionCoefficient());
        assertEquals(.375, config.getBlockerRollingResistance());
    }

    @Test void reproductionSettingsRoundTrip() throws Exception {
        String cells = dir.resolve("reproduction.cells").toString();
        config.setReproductionEnergyThreshold(.65); config.setMutationRate(.125); config.setMutationStrength(.375);
        config.setReproductionDesireThreshold(.75); config.setReproductionAgeThreshold(14.5); config.setReproductionCooldown(7.25);
        config.setDivisionMutationRateMultiplier(.25); config.setNormalizeBrainInputs(true);
        SimulationSettingsFile.save(cells, config, 1);
        config.setReproductionEnergyThreshold(.4); config.setMutationRate(.1); config.setMutationStrength(.2);
        config.setReproductionDesireThreshold(.5); config.setReproductionAgeThreshold(8); config.setReproductionCooldown(0);
        config.setDivisionMutationRateMultiplier(1); config.setNormalizeBrainInputs(false);
        assertEquals(List.of(), SimulationSettingsFile.load(cells, config).problems());
        assertEquals(.65, config.getReproductionEnergyThreshold()); assertEquals(.125, config.getMutationRate());
        assertEquals(.375, config.getMutationStrength()); assertEquals(.75, config.getReproductionDesireThreshold());
        assertEquals(14.5, config.getReproductionAgeThreshold()); assertEquals(7.25, config.getReproductionCooldown());
        assertEquals(.25, config.getDivisionMutationRateMultiplier()); assertTrue(config.isNormalizeBrainInputs());
    }

    @Test void oldCellsFilesWithoutSettingsKeepTheCurrentConfig() throws Exception {
        config.setSunZoneWidth(800);
        SimulationSettingsFile.Loaded loaded = SimulationSettingsFile.load(dir.resolve("old.cells").toString(), config);
        assertEquals(List.of(), loaded.problems());
        assertEquals(-1, loaded.steps());
        assertEquals(800, config.getSunZoneWidth());
    }

    @Test void badValuesAndAnotherWorldAreReportedNotApplied() throws Exception {
        String cells = dir.resolve("edited.cells").toString();
        config.setAdhesionRejection(.4);
        Files.writeString(Path.of(cells + ".settings"), """
                version=1
                info.trainMode=WORLD2
                info.width=25600.0
                adhesionRejection=2
                sunZoneWidth=1200
                cellAdhesion=yes
                steps=-5
                """);
        SimulationSettingsFile.Loaded loaded = SimulationSettingsFile.load(cells, config);
        List<String> problems = loaded.problems();
        assertEquals(-1, loaded.steps());
        assertEquals(.4, config.getAdhesionRejection());
        assertEquals(1200, config.getSunZoneWidth());
        assertEquals(4, problems.size(), problems.toString());
        assertTrue(problems.get(2).contains("WORLD2"), problems.toString());
    }

    @Test void everyStoredKeyIsWritten() throws Exception {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(Path.of(stash + ".settings"))) { properties.load(reader); }
        for (String key : SimulationSettingsFile.keys()) assertNotNull(properties.getProperty(key), key);
        assertEquals("0", properties.getProperty("steps"));
    }
}
