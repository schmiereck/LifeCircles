package de.lifecircles.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.nio.file.*;
import java.util.*;

/** Explicit study: mvn test -Dtest=SpringFieldSweepTest -Dspring.sweep=true (writes target/spring-field/sweep.csv). */
class SpringFieldSweepTest {
    @Test @EnabledIfSystemProperty(named = "spring.sweep", matches = "true")
    void sweep() throws Exception {
        List<SpringFieldLab.Field> fields = List.of(
                SpringFieldLab.Field.classic(1), SpringFieldLab.Field.spring(.75, 2, 1), SpringFieldLab.Field.spring(.75, 4, 1).uncapped(),
                SpringFieldLab.Field.classic(.6), SpringFieldLab.Field.spring(.5, 4, .6).uncapped(), SpringFieldLab.Field.spring(.75, 4, .6).uncapped());
        List<SpringFieldLab.Probe> probes = new ArrayList<>();
        for (String g : new String[]{"bar"}) for (int layers : new int[]{1, 2, 3}) {
            probes.add(new SpringFieldLab.Probe(g, 9, layers, "none", 0));
            for (double f : new double[]{150, 300, 600, 1200}) probes.add(new SpringFieldLab.Probe(g, 9, layers, "tension", f));
            for (double f : new double[]{6, 30, 150}) probes.add(new SpringFieldLab.Probe(g, 9, layers, "bend", f));
            for (double f : new double[]{150, 600}) probes.add(new SpringFieldLab.Probe(g, 9, layers, "compression", f));
        }
        for (int layers : new int[]{1, 2}) {
            probes.add(new SpringFieldLab.Probe("ring", 24, layers, "none", 0));
            for (double f : new double[]{30, 150, 600}) probes.add(new SpringFieldLab.Probe("ring", 24, layers, "compression", f));
            for (double f : new double[]{150, 600, 1200}) probes.add(new SpringFieldLab.Probe("ring", 24, layers, "tension", f));
        }
        StringBuilder csv = new StringBuilder(SpringFieldLab.Result.header());
        try (var settings = new SpringFieldLab.Settings()) {
            for (var field : fields) for (var probe : probes) {
                var r = SpringFieldLab.run(field, probe);
                csv.append(r.csv());
                System.out.print(r.csv());
            }
        }
        Path out = Path.of(System.getProperty("spring.output", "target/spring-field"));
        Files.createDirectories(out);
        Files.writeString(out.resolve("sweep.csv"), csv);
    }

    /** Tear factor limit by cell size at 60 Hz: mvn test -Dtest=SpringFieldSweepTest -Dspring.sizelimit=true. */
    @Test @EnabledIfSystemProperty(named = "spring.sizelimit", matches = "true")
    void sizeLimit() throws Exception {
        StringBuilder csv = new StringBuilder(SpringFieldLab.Result.header());
        try (var settings = new SpringFieldLab.Settings()) {
            for (double tear : new double[]{1, 1.5, 2, 2.5}) for (double radius : new double[]{16, 20})
                for (var probe : List.of(
                        new SpringFieldLab.Probe("bar", 9, 2, "none", 0, radius, 1.0 / 60), new SpringFieldLab.Probe("bar", 9, 3, "none", 0, radius, 1.0 / 60),
                        new SpringFieldLab.Probe("bar", 9, 3, "tension", 600, radius, 1.0 / 60), new SpringFieldLab.Probe("bar", 9, 3, "bend", 150, radius, 1.0 / 60)))
                    csv.append(SpringFieldLab.run(SpringFieldLab.Field.spring(.75, tear, 1).uncapped(), probe).csv());
        }
        Path out = Path.of(System.getProperty("spring.output", "target/spring-field"));
        Files.createDirectories(out);
        Files.writeString(out.resolve("sizelimit.csv"), csv);
    }

    /** Cell size and time step for the chosen tear factor: mvn test -Dtest=SpringFieldSweepTest -Dspring.sizesweep=true. */
    @Test @EnabledIfSystemProperty(named = "spring.sizesweep", matches = "true")
    void sizeSweep() throws Exception {
        StringBuilder csv = new StringBuilder(SpringFieldLab.Result.header());
        try (var settings = new SpringFieldLab.Settings()) {
            for (var field : List.of(SpringFieldLab.Field.classic(1), SpringFieldLab.Field.spring(.75, 3, 1)))
                for (double radius : new double[]{8, 12, 20}) for (double dt : new double[]{1.0 / 60, 1.0 / 120})
                    for (int layers : new int[]{2, 3}) for (var probe : List.of(
                            new SpringFieldLab.Probe("bar", 9, layers, "none", 0, radius, dt), new SpringFieldLab.Probe("bar", 9, layers, "tension", 300, radius, dt),
                            new SpringFieldLab.Probe("bar", 9, layers, "bend", 150, radius, dt),
                            new SpringFieldLab.Probe("ring", 24, layers - 1, "none", 0, radius, dt),
                            new SpringFieldLab.Probe("ring", 24, layers - 1, "tension", 600, radius, dt))) {
                        var r = SpringFieldLab.run(field, probe);
                        csv.append(r.csv());
                    }
        }
        Path out = Path.of(System.getProperty("spring.output", "target/spring-field"));
        Files.createDirectories(out);
        Files.writeString(out.resolve("size.csv"), csv);
    }

    /** Stability limit of the tear factor at full attraction: mvn test -Dtest=SpringFieldSweepTest -Dspring.tearsweep=true. */
    @Test @EnabledIfSystemProperty(named = "spring.tearsweep", matches = "true")
    void tearSweep() throws Exception {
        StringBuilder csv = new StringBuilder(SpringFieldLab.Result.header());
        try (var settings = new SpringFieldLab.Settings()) {
            for (double tear : new double[]{1, 1.5, 2, 2.5, 3, 3.5}) for (int layers : new int[]{2, 3}) for (var probe : List.of(
                    new SpringFieldLab.Probe("bar", 9, layers, "none", 0), new SpringFieldLab.Probe("bar", 9, layers, "tension", 300),
                    new SpringFieldLab.Probe("bar", 9, layers, "tension", 600), new SpringFieldLab.Probe("bar", 9, layers, "bend", 150),
                    new SpringFieldLab.Probe("ring", 24, layers - 1, "tension", 600), new SpringFieldLab.Probe("ring", 24, layers - 1, "compression", 150))) {
                var r = SpringFieldLab.run(SpringFieldLab.Field.spring(.75, tear, 1).uncapped(), probe);
                csv.append(r.csv());
            }
        }
        Path out = Path.of(System.getProperty("spring.output", "target/spring-field"));
        Files.createDirectories(out);
        Files.writeString(out.resolve("tear.csv"), csv);
    }
}
