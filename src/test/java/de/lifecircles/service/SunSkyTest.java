package de.lifecircles.service;

import de.lifecircles.model.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** SPEKTRALZONEN.md (Sonne): the whole sky shines and slanted rays wrap around the periodic x axis. */
class SunSkyTest {
    static final double W = 400, H = 300;
    final SimulationConfig config = SimulationConfig.getInstance();
    double rate, spacing, rayEnergy;
    boolean zones;

    @BeforeEach void save() {
        rate = config.getSunRayRate(); spacing = config.getSunRaySpacingPx(); rayEnergy = config.getEnergyPerRay(); zones = config.isSunSpectralZones();
        config.setSunRayRate(40); config.setSunRaySpacingPx(10); config.setEnergyPerRay(1e-5); config.setSunSpectralZones(false);
    }
    @AfterEach void restore() {
        config.setSunRayRate(rate); config.setSunRaySpacingPx(spacing); config.setEnergyPerRay(rayEnergy); config.setSunSpectralZones(zones);
    }

    static Cell cell(double x, double y) {
        Cell cell = new Cell(new Vector2D(x, y), 10, new PhysicsRegressionTest.ConstantBrain());
        cell.setEnergy(0);
        return cell;
    }

    /** Energy each cell absorbs over the given simulated time, one row of cells at a time (no shading). */
    static double[] light(double y, double seconds, double[] xs) {
        List<Cell> cells = new ArrayList<>();
        for (double x : xs) cells.add(cell(x, y));
        var sun = new EnergySunCalcService(new Random(7));
        for (double t = 0; t < seconds; t += .01) sun.calculateSunEnergy(cells, List.of(), W, H, SimulationConfig.getInstance(), .01);
        return cells.stream().mapToDouble(Cell::getEnergy).toArray();
    }

    /**
     * Morning sun (35 degrees): rays starting along the old ground-aimed range left the top of the far side dark
     * (top right, x > 90 + 0.7·y in a 300 px wide world). Now every place in a row gets the same light, also the
     * cells across the seam at x = 0.
     */
    @Test void slantedLightReachesEveryPlaceEvenly() {
        double[] xs = {0, 50, 100, 150, 200, 250, 300, 350}; // evenly spaced across the seam, none shading another
        for (double y : new double[]{15, 150, 285}) {
            double[] energy = light(y, 10, xs);
            double mean = Arrays.stream(energy).average().orElseThrow();
            System.out.printf(Locale.ROOT, "y=%3.0f: %s (mean %.4f)%n", y, Arrays.toString(Arrays.stream(energy).map(e -> Math.round(e / mean * 100) / 100.0).toArray()), mean);
            for (int i = 0; i < xs.length; i++) assertEquals(mean, energy[i], mean * .15, "x=" + xs[i] + " y=" + y);
        }
    }

    @Test void raysStartAlongTheWholeSkyAndEndUnwrapped() {
        var sun = new EnergySunCalcService(new Random(3));
        List<SunRay> rays = new ArrayList<>();
        for (int i = 0; i < 50; i++) rays.addAll(sun.calculateSunEnergy(List.of(), List.of(), W, H, config, .01));
        assertFalse(rays.isEmpty());
        double tan = Math.tan(Math.toRadians(35));
        for (SunRay ray : rays) {
            assertEquals(0, ray.getStart().getY());
            assertTrue(ray.getStart().getX() >= 0 && ray.getStart().getX() < W);
            assertEquals(H, ray.getEnd().getY(), 1e-9, "no side walls: every ray reaches the bottom");
            // Morning: drifting right by about 0.7 · height, past the world edge for most rays.
            assertEquals(H * tan, ray.getEnd().getX() - ray.getStart().getX(), 5, "the sun turns a little");
        }
        assertTrue(rays.stream().anyMatch(ray -> ray.getEnd().getX() > W), "rays cross the seam");
    }

    @Test void aBlockerShadesAcrossTheSeam() {
        // A wide roof right of the seam's left side shades cells below it on the far side of the world.
        var roof = new Blocker(new Vector2D(W - 20, 100), 40, 10, javafx.scene.paint.Color.GRAY, Blocker.BlockerType.PLATFORM);
        double tan = Math.tan(Math.toRadians(35));
        // Below the roof: rays through it drift right across x = W and arrive at x ≈ (W - 20 + 100·tan) - W.
        Cell shaded = cell(W - 20 + 150 * tan - W, 250), open = cell(W / 2, 250);
        var sun = new EnergySunCalcService(new Random(5));
        for (int i = 0; i < 100; i++) sun.calculateSunEnergy(List.of(shaded, open), List.of(roof), W, H, config, .01);
        System.out.printf(Locale.ROOT, "behind the roof across the seam %.4f, open %.4f%n", shaded.getEnergy(), open.getEnergy());
        assertTrue(open.getEnergy() > 0);
        assertTrue(shaded.getEnergy() < open.getEnergy() * .1, "the roof's shadow wraps around");
    }
}
