package de.lifecircles.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.nio.file.*;
import java.util.*;

/** Opt-in reproducible parameter study; excluded from routine regression runs. */
@EnabledIfSystemProperty(named = "mechanics.sweep", matches = "true")
class MechanicsSweepTest {
    @Test void sweep() throws Exception {
        Path directory = Path.of(System.getProperty("mechanics.output", "target/mechanics"));
        Files.createDirectories(directory);
        StringBuilder results = new StringBuilder(MechanicsLab.Result.header());
        StringBuilder frames = new StringBuilder("profile,scenario,dt,time,cell,x,y,radius\n");
        List<MechanicsLab.Parameters> profiles = new ArrayList<>();
        if (System.getProperty("mechanics.validation") != null) {
            profiles.add(new MechanicsLab.Parameters("legacy", 90, 125, 0.8, 0.8, 0));
            profiles.add(new MechanicsLab.Parameters("soft-only", 90, 125, 0.8, 0.8, 0.25));
            profiles.add(new MechanicsLab.Parameters("balanced", 90, 125, 0.8, 3, 0.25));
        } else if (System.getProperty("mechanics.refine") != null) {
            for (double smoothing : new double[]{0, 0.1, 0.25, 0.5}) for (double damping : new double[]{0.8, 3})
                profiles.add(new MechanicsLab.Parameters("soft" + smoothing + "-d" + damping, 90, 125, 0.8, damping, smoothing));
        } else {
            for (double attraction : new double[]{30, 90, 180}) for (double contact : new double[]{80, 125, 250})
                for (double viscosity : new double[]{0.8, 2}) profiles.add(new MechanicsLab.Parameters(
                        "a" + (int) attraction + "-k" + (int) contact + "-v" + viscosity, attraction, contact, viscosity, 0.8,
                        Double.parseDouble(System.getProperty("mechanics.softening", "0"))));
        }
        double dt = Double.parseDouble(System.getProperty("mechanics.dt", "0.016666666666666666"));
        String[] scenarios = System.getProperty("mechanics.scenarios", "snowman,snowman-drop,chain,strip,actuator,sizes").split(",");
        try (MechanicsLab.Settings ignored = new MechanicsLab.Settings()) {
            for (var profile : profiles) {
                for (String scenario : scenarios) results.append(MechanicsLab.run(profile, scenario, dt, frames).csv());
                Files.writeString(directory.resolve("results.csv"), results);
                Files.writeString(directory.resolve("trajectories.csv"), frames);
                System.out.println("Measured " + profile.name() + " at dt=" + dt);
            }
        }
    }
}
