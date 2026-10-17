package de.lifecircles.model.neural;

/** Append-only output channels. Original global/actor indices never move. */
public final class OutputLayout {
    private OutputLayout() {}
    public static int legacyOutputCount(int actors) {
        return GlobalOutputFeature.values().length + actors * ActorOutputFeature.values().length;
    }
    public static int outputCount(int actors) {
        return legacyOutputCount(actors) + 1;
    }
    public static int enzymeActivity(int actors) {
        return legacyOutputCount(actors);
    }
    /**
     * A migrated, disconnected sigmoid output is exactly 0.5 and keeps the previous full activity.
     * Lower outputs throttle linearly to zero; mutated activations are clamped to [0,1].
     */
    public static double activityFromOutput(double output) {
        if (Double.isNaN(output)) return 1;
        return Math.clamp(2 * output, 0, 1);
    }
}
