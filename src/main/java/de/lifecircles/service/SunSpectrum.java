package de.lifecircles.service;

import de.lifecircles.model.EnergyType;
import java.util.List;

/**
 * Location-dependent sunlight: equally wide zones repeating along the periodic x axis.
 * By default one cycle of the four zones spans the world; a fixed zone width repeats the cycle.
 * Narrow borders mix both neighbors linearly; every ray still carries exactly one form.
 */
public final class SunSpectrum {
    public static final List<EnergyType> ZONES =
            List.of(EnergyType.PRIMARY, EnergyType.SECONDARY, EnergyType.RGB_010, EnergyType.RGB_100);
    /** Width of one mixed border, as a fraction of one zone width, centered on the border. */
    public static final double TRANSITION_FRACTION = .15;
    private SunSpectrum() {}

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
        double u = position(x, width, zoneWidth);
        int border = (int) Math.round(u);
        double offset = u - border, half = TRANSITION_FRACTION / 2;
        int left = Math.floorMod(border - 1, ZONES.size()), right = Math.floorMod(border, ZONES.size());
        if (Math.abs(offset) >= half) return zone == Math.floorMod((int) Math.floor(u), ZONES.size()) ? 1 : 0;
        double rightShare = (offset + half) / (2 * half);
        return (zone == right ? rightShare : 0) + (zone == left ? 1 - rightShare : 0);
    }

    /** One form for a ray hitting x; draw is uniform in [0,1) and only matters in mixed borders. One cycle per world. */
    public static EnergyType formAt(double x, double width, double draw) {
        return formAt(x, width, width / ZONES.size(), draw);
    }

    /** One form for a ray hitting x for repeating zones of the given width. */
    public static EnergyType formAt(double x, double width, double zoneWidth, double draw) {
        double u = position(x, width, zoneWidth);
        int border = (int) Math.round(u);
        double offset = u - border, half = TRANSITION_FRACTION / 2;
        if (Math.abs(offset) >= half) return ZONES.get(Math.floorMod((int) Math.floor(u), ZONES.size()));
        boolean right = draw < (offset + half) / (2 * half);
        return ZONES.get(Math.floorMod(right ? border : border - 1, ZONES.size()));
    }

    /** Index of the zone containing x, ignoring the mixed borders. */
    public static int zoneIndex(double x, double width, double zoneWidth) {
        return Math.min(ZONES.size() - 1, (int) position(x, width, zoneWidth));
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
