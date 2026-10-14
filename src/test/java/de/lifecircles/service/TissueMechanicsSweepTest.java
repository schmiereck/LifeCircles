package de.lifecircles.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.nio.file.*;

@EnabledIfSystemProperty(named = "tissue.sweep", matches = "true")
class TissueMechanicsSweepTest {
    @Test void measure() throws Exception {
        Path output = Path.of(System.getProperty("tissue.output", "target/tissue-mechanics"));
        Files.createDirectories(output);
        StringBuilder results = new StringBuilder(TissueMechanicsLab.Result.header());
        double dt = Double.parseDouble(System.getProperty("tissue.dt", "0.016666666666666666"));
        try (MechanicsLab.Settings ignored = new MechanicsLab.Settings()) {
            for (int columns : new int[]{5, 9}) for (int layers : new int[]{1, 2, 3}) {
                for (String loading : new String[]{"bend", "tension", "compression"})
                    for (double force : new double[]{0, 1, 6, 30, 150}) {
                        results.append(TissueMechanicsLab.run(new TissueMechanicsLab.Probe("bar", columns, layers, loading, force, dt)).csv());
                    }
                for (double force : new double[]{0, 1, 6, 30})
                    results.append(TissueMechanicsLab.run(new TissueMechanicsLab.Probe("bar", columns, layers, "active", force, dt)).csv());
                Files.writeString(output.resolve("results.csv"), results);
                System.out.println("Measured bar " + columns + "x" + layers + " dt=" + dt);
            }
            for (double force : new double[]{0, 1, 6, 30, 150})
                results.append(TissueMechanicsLab.run(new TissueMechanicsLab.Probe("neck", 9, 3, "bend", force, dt)).csv());
            Files.writeString(output.resolve("results.csv"), results);
        }
    }
}
