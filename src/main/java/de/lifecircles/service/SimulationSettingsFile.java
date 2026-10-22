package de.lifecircles.service;

import de.lifecircles.model.EnergyType;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Environment settings from the GUI panel, stored beside a cells file as {@code <file>.cells.settings}
 * (readable properties). The cells file format stays unchanged, so old files load as before; missing keys keep
 * the current value. Scenario and world size are recorded for information only: loading cells keeps the world.
 */
public final class SimulationSettingsFile {
    public static final String SUFFIX = ".settings";
    static final int VERSION = 1;

    private record Setting(String key, Function<SimulationConfig, String> get, Consumer<Applied> set) {}
    private record Applied(SimulationConfig config, String value) {
        double number() { return Double.parseDouble(value); }
        boolean flag() {
            if (!value.equals("true") && !value.equals("false")) throw new IllegalArgumentException("not a boolean: " + value);
            return Boolean.parseBoolean(value);
        }
    }

    private static final List<Setting> SETTINGS = List.of(
            new Setting("sunRayRate", c -> str(c.getSunRayRate()), a -> a.config.setSunRayRate(nonNegative(a.number()))),
            new Setting("sunEnergyType", c -> c.getSunEnergyType().name(), a -> a.config.setSunEnergyType(EnergyType.valueOf(a.value))),
            new Setting("sunSpectralZones", c -> str(c.isSunSpectralZones()), a -> a.config.setSunSpectralZones(a.flag())),
            new Setting("sunZoneWidth", c -> str(c.getSunZoneWidth()), a -> a.config.setSunZoneWidth(a.number())),
            new Setting("seasonDuration", c -> str(c.getSeasonDuration()), a -> a.config.setSeasonDuration(a.number())),
            new Setting("winterIntensity", c -> str(c.getWinterIntensity()), a -> a.config.setWinterIntensity(a.number())),
            new Setting("cellAdhesion", c -> str(c.isCellAdhesion()), a -> a.config.setCellAdhesion(a.flag())),
            new Setting("trainingAdhesion", c -> str(c.isTrainingAdhesion()), a -> a.config.setTrainingAdhesion(a.flag())),
            new Setting("adhesionBonds", c -> str(c.isAdhesionBonds()), a -> a.config.setAdhesionBonds(a.flag())),
            new Setting("adhesionBirthBonds", c -> str(c.isAdhesionBirthBonds()), a -> a.config.setAdhesionBirthBonds(a.flag())),
            new Setting("remainsForceHalfLife", c -> str(c.getRemainsForceHalfLife()), a -> a.config.setRemainsForceHalfLife(a.number())),
            new Setting("adhesionRejection", c -> str(c.getAdhesionRejection()), a -> a.config.setAdhesionRejection(a.number())),
            new Setting("groundAdhesion", c -> str(c.isGroundAdhesion()), a -> a.config.setGroundAdhesion(a.flag())),
            new Setting("groundAnchors", c -> str(c.isGroundAnchors()), a -> a.config.setGroundAnchors(a.flag())),
            new Setting("groundAnchorsGroundOnly", c -> str(c.isGroundAnchorsGroundOnly()), a -> a.config.setGroundAnchorsGroundOnly(a.flag())),
            new Setting("groundStickiness", c -> str(c.getGroundStickiness()), a -> a.config.setGroundStickiness(a.number())));

    private SimulationSettingsFile() {}

    public static Path pathFor(String cellsFile) { return Path.of(cellsFile + SUFFIX); }

    public static void save(String cellsFile, SimulationConfig config) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("version", String.valueOf(VERSION));
        properties.setProperty("info.trainMode", config.getTrainMode().name());
        properties.setProperty("info.width", str(config.getWidth()));
        properties.setProperty("info.height", str(config.getHeight()));
        for (Setting setting : SETTINGS) properties.setProperty(setting.key, setting.get.apply(config));
        try (Writer writer = Files.newBufferedWriter(pathFor(cellsFile), StandardCharsets.UTF_8)) {
            properties.store(writer, "LifeCircles environment settings for " + Path.of(cellsFile).getFileName());
        }
    }

    /**
     * Applies the stored settings to the config. Returns the problems (unreadable values, other world); an empty
     * list also when no settings file exists, which then leaves the config unchanged.
     */
    public static List<String> load(String cellsFile, SimulationConfig config) throws IOException {
        List<String> problems = new ArrayList<>();
        Path path = pathFor(cellsFile);
        if (!Files.exists(path)) return problems;
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        for (Setting setting : SETTINGS) {
            String value = properties.getProperty(setting.key);
            if (value == null) continue;
            try {
                setting.set.accept(new Applied(config, value.trim()));
            } catch (RuntimeException e) {
                problems.add(setting.key + "=" + value + " ignored: " + e.getMessage());
            }
        }
        String mode = properties.getProperty("info.trainMode"), width = properties.getProperty("info.width");
        if (mode != null && !mode.equals(config.getTrainMode().name()) || width != null && !width.equals(str(config.getWidth()))) {
            problems.add("cells were saved in " + mode + " (width " + width + "), current world is "
                    + config.getTrainMode() + " (width " + str(config.getWidth()) + ")");
        }
        return problems;
    }

    private static String str(double value) { return String.valueOf(value); }
    private static String str(boolean value) { return String.valueOf(value); }
    private static double nonNegative(double value) {
        if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("must be finite and >= 0");
        return value;
    }

    /** For tests: the stored keys in file order. */
    static List<String> keys() { return SETTINGS.stream().map(Setting::key).toList(); }
}
