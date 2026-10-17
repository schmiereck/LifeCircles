package de.lifecircles.service;

import de.lifecircles.model.EnergyType;
import java.util.List;

/**
 * Location-dependent sunlight: four equally wide zones along the periodic x axis.
 * Narrow borders mix both neighbors linearly; every ray still carries exactly one form.
 */
public final class SunSpectrum {
    public static final List<EnergyType> ZONES =
            List.of(EnergyType.PRIMARY, EnergyType.SECONDARY, EnergyType.RGB_010, EnergyType.RGB_100);
    /** Width of one mixed border, as a fraction of one zone width, centered on the border. */
    public static final double TRANSITION_FRACTION = .15;
    private SunSpectrum() {}

    /** Share of the zone with the given index at x; shares of all zones sum to one. */
    public static double share(double x, double width, int zone) {
        double u = position(x, width);
        int border = (int) Math.round(u);
        double offset = u - border, half = TRANSITION_FRACTION / 2;
        int left = Math.floorMod(border - 1, ZONES.size()), right = Math.floorMod(border, ZONES.size());
        if (Math.abs(offset) >= half) return zone == Math.floorMod((int) Math.floor(u), ZONES.size()) ? 1 : 0;
        double rightShare = (offset + half) / (2 * half);
        return (zone == right ? rightShare : 0) + (zone == left ? 1 - rightShare : 0);
    }

    /** One form for a ray hitting x; draw is uniform in [0,1) and only matters in mixed borders. */
    public static EnergyType formAt(double x, double width, double draw) {
        double u = position(x, width);
        int border = (int) Math.round(u);
        double offset = u - border, half = TRANSITION_FRACTION / 2;
        if (Math.abs(offset) >= half) return ZONES.get(Math.floorMod((int) Math.floor(u), ZONES.size()));
        boolean right = draw < (offset + half) / (2 * half);
        return ZONES.get(Math.floorMod(right ? border : border - 1, ZONES.size()));
    }

    /** Continuous zone coordinate in [0, zone count), wrapped like the periodic world. */
    private static double position(double x, double width) {
        if (!Double.isFinite(x) || !(width > 0)) throw new IllegalArgumentException("Invalid spectrum position");
        double u = (x % width + width) % width / width * ZONES.size();
        return u >= ZONES.size() ? 0 : u;
    }
}
