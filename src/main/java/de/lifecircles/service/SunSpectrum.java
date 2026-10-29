package de.lifecircles.service;

import de.lifecircles.model.EnergyType;
import java.util.List;

/**
 * Location-dependent sunlight: equally wide zones repeating along the periodic x axis.
 * By default one cycle of the four zones spans the world; a fixed zone width repeats the cycle.
 * Borders mix both neighbors over an adjustable width and curve, and a background share of every form falls in
 * every zone (SPEKTRALZONEN.md). Every ray still carries exactly one form.
 */
public final class SunSpectrum {
    public static final List<EnergyType> ZONES =
            List.of(EnergyType.PRIMARY, EnergyType.SECONDARY, EnergyType.RGB_010, EnergyType.RGB_100);
    /** Default width of one mixed border, as a fraction of one zone width, centered on the border. */
    public static final double TRANSITION_FRACTION = .15;
    private SunSpectrum() {}

    /**
     * How zones mix.
     *
     * @param background share of every form in every zone, 0 … 1/4 (1/4 = no zones left)
     * @param transition width of one mixed border as a fraction of the zone width, 0 (hard) … 1 (pure only at the centers)
     * @param softness   curve across the border, 0 = linear … 1 = smooth S-curve (the neighbor reaches in less deeply)
     */
    public record Mix(double background, double transition, double softness) {
        public static final Mix DEFAULT = new Mix(0, TRANSITION_FRACTION, 0);
        public Mix {
            if (!(background >= 0 && background <= 1.0 / ZONES.size())) throw new IllegalArgumentException("Background share must be in [0, 1/4]");
            if (!(transition >= 0 && transition <= 1)) throw new IllegalArgumentException("Transition must be in [0, 1]");
            if (!(softness >= 0 && softness <= 1)) throw new IllegalArgumentException("Softness must be in [0, 1]");
        }
        public static Mix of(SimulationConfig config) {
            return new Mix(config.getSunZoneBackground(), config.getSunZoneTransition(), config.getSunZoneSoftness());
        }
    }

    /** Effective zone width: the configured width in px, or a quarter of the world for 0. */
    public static double zoneWidth(double width, double configured) {
        return configured > 0 ? configured : width / ZONES.size();
    }

    /** Share of the zone with the given index at x; shares of all zones sum to one. One cycle per world. */
    public static double share(double x, double width, int zone) {
        return share(x, width, width / ZONES.size(), zone);
    }

    /** Share of the zone with the given index at x for repeating zones of the given width. */
    public static double share(double x, double width, double zoneWidth, int zone) {
        return share(x, width, zoneWidth, zone, Mix.DEFAULT);
    }

    /** Share of the zone with the given index at x: background plus the border profile. */
    public static double share(double x, double width, double zoneWidth, int zone, Mix mix) {
        return mix.background() + (1 - ZONES.size() * mix.background()) * profile(x, width, zoneWidth, zone, mix);
    }

    /** One form for a ray hitting x; draw is uniform in [0,1). One cycle per world. */
    public static EnergyType formAt(double x, double width, double draw) {
        return formAt(x, width, width / ZONES.size(), draw);
    }

    /** One form for a ray hitting x for repeating zones of the given width. */
    public static EnergyType formAt(double x, double width, double zoneWidth, double draw) {
        return formAt(x, width, zoneWidth, draw, Mix.DEFAULT);
    }

    /**
     * One form for a ray hitting x, drawn with the probabilities of share(). The dominant form takes the lowest
     * draws, so a pure zone stays pure for every draw, even where rounding leaves a neighbor a share of 1e-17.
     */
    public static EnergyType formAt(double x, double width, double zoneWidth, double draw, Mix mix) {
        int dominant = dominant(x, width, zoneWidth, mix);
        double cumulative = share(x, width, zoneWidth, dominant, mix);
        if (draw < cumulative) return ZONES.get(dominant);
        int last = dominant;
        for (int zone = 0; zone < ZONES.size(); zone++) {
            if (zone == dominant) continue;
            last = zone;
            cumulative += share(x, width, zoneWidth, zone, mix);
            if (draw < cumulative) return ZONES.get(zone);
        }
        return ZONES.get(last);
    }

    /** The form with the largest share at x, for drawing unabsorbed rays without consuming randomness. */
    public static EnergyType dominantAt(double x, double width, double zoneWidth, Mix mix) {
        return ZONES.get(dominant(x, width, zoneWidth, mix));
    }

    private static int dominant(double x, double width, double zoneWidth, Mix mix) {
        int best = 0;
        for (int zone = 1; zone < ZONES.size(); zone++)
            if (profile(x, width, zoneWidth, zone, mix) > profile(x, width, zoneWidth, best, mix)) best = zone;
        return best;
    }

    /** Index of the zone containing x, ignoring the mixed borders. */
    public static int zoneIndex(double x, double width, double zoneWidth) {
        return Math.min(ZONES.size() - 1, (int) position(x, width, zoneWidth));
    }

    /** Zone shares without the background: pure inside a zone, mixed across a border of the configured width. */
    private static double profile(double x, double width, double zoneWidth, int zone, Mix mix) {
        double u = position(x, width, zoneWidth);
        int border = (int) Math.round(u);
        double offset = u - border, half = mix.transition() / 2;
        if (Math.abs(offset) >= half) return zone == Math.floorMod((int) Math.floor(u), ZONES.size()) ? 1 : 0;
        double t = (offset + half) / (2 * half);
        double rightShare = (1 - mix.softness()) * t + mix.softness() * t * t * t * (t * (6 * t - 15) + 10);
        int left = Math.floorMod(border - 1, ZONES.size()), right = Math.floorMod(border, ZONES.size());
        return (zone == right ? rightShare : 0) + (zone == left ? 1 - rightShare : 0);
    }

    /**
     * Continuous zone coordinate in [0, zone count), wrapped like the periodic world. The world width should be
     * a multiple of one cycle (zone count × zone width); otherwise the cycle is cut off at x = 0.
     */
    private static double position(double x, double width, double zoneWidth) {
        if (!Double.isFinite(x) || !(width > 0) || !(zoneWidth > 0) || !Double.isFinite(zoneWidth))
            throw new IllegalArgumentException("Invalid spectrum position");
        double u = (x % width + width) % width / zoneWidth % ZONES.size();
        return u >= ZONES.size() ? 0 : u;
    }
}
