package de.lifecircles.model.neural;

import de.lifecircles.model.EnergyType;

/** Append-only energy channels and clocks. Original global/actor indices never move. */
public final class EnergyInputLayout {
    /** Inherited clocks, each read as sine and cosine of the cell's age (Cell#getClockPeriod). */
    public static final int CLOCKS = 6;
    private EnergyInputLayout() {}
    public static int legacyInputCount(int actors) {
        return GlobalInputFeature.values().length + actors * SensorInputFeature.values().length;
    }
    public static int inputCount(int actors) {
        return remainsInputCount(actors) + 2 * CLOCKS;
    }
    /** The layout before the clocks: energy forms and neighbouring remains. */
    public static int remainsInputCount(int actors) {
        return energyOnlyInputCount(actors) + actors;
    }
    public static int clockSine(int actors, int clock) {
        if (clock < 0 || clock >= CLOCKS) throw new IndexOutOfBoundsException(clock);
        return remainsInputCount(actors) + 2 * clock;
    }
    public static int clockCosine(int actors, int clock) { return clockSine(actors, clock) + 1; }
    public static int energyOnlyInputCount(int actors) {
        return legacyInputCount(actors) + (EnergyType.ALL.size() - 1) * (actors + 1);
    }
    public static int neighborRemains(int actors, int actor) {
        if (actor < 0 || actor >= actors) throw new IndexOutOfBoundsException(actor);
        return energyOnlyInputCount(actors) + actor;
    }
    public static int ownEnergy(int actors, EnergyType type) {
        return type == EnergyType.PRIMARY ? GlobalInputFeature.MY_CELL_ENERGY.ordinal()
                : legacyInputCount(actors) + type.getCode() - 1;
    }
    public static int neighborEnergy(int actors, int actor, EnergyType type) {
        if (actor < 0 || actor >= actors) throw new IndexOutOfBoundsException(actor);
        return type == EnergyType.PRIMARY
                ? GlobalInputFeature.values().length + actor * SensorInputFeature.values().length + SensorInputFeature.SENSED_CELL_ENERGY.ordinal()
                : legacyInputCount(actors) + (EnergyType.ALL.size() - 1) * (actor + 1) + type.getCode() - 1;
    }
}
