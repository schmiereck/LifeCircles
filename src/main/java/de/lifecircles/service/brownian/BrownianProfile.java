package de.lifecircles.service.brownian;

/**
 * Height profile of the Brownian strength (BROWNSCHE_BEWEGUNG.md, Höhenprofil): the "temperature" of the background
 * may depend on the height above the ground. Two straight pieces meet at a knee: between the ground and the knee
 * height lies the configured share of the whole change, the rest between the knee and the top of the world. A high
 * share with a low knee gives a steep gradient where the cells live on the ground.
 */
public enum BrownianProfile {
    /** The same strength everywhere. */
    UNIFORM,
    /** Warm (full strength) at the ground, cold at the top: air heated by the ground. */
    WARM_BOTTOM,
    /** Warm at the top, cold at the ground: water warmed from the surface. */
    WARM_TOP;

    /**
     * Share of the configured strength at {@code height} px above the ground surface.
     *
     * @param top        height of the world's top above the ground surface (px)
     * @param cold       share of the strength at the cold end (0..1)
     * @param knee       knee height above the ground (px)
     * @param kneeShare  share of the change between the ground and the knee (0..1)
     */
    public double factor(double height, double top, double cold, double knee, double kneeShare) {
        if (this == UNIFORM) return 1;
        double h = Math.max(0, Math.min(top, height));
        double k = Math.max(0, Math.min(top, knee));
        // Progress from the ground (0) to the top (1), bent at the knee.
        double progress;
        if (h <= k) progress = k > 0 ? kneeShare * h / k : kneeShare;
        else progress = top > k ? kneeShare + (1 - kneeShare) * (h - k) / (top - k) : 1;
        double coldness = this == WARM_BOTTOM ? progress : 1 - progress;
        return 1 - coldness * (1 - cold);
    }
}
