package de.lifecircles.service;

/** Multipliers of the existing storage and per-decision energy prices. */
public record BrainEnergyCosts(double storage, double work) {
    public static final BrainEnergyCosts DEFAULT = new BrainEnergyCosts(1, 1);
    public BrainEnergyCosts {
        if (!Double.isFinite(storage) || storage < 0 || !Double.isFinite(work) || work < 0)
            throw new IllegalArgumentException("Brain energy multipliers must be finite and nonnegative");
    }
    public double cost(int stored, long executed, double deltaTime) {
        return storage * stored * SimulationConfig.ENERGY_COST_PER_SYNAPSE * deltaTime
                + work * executed * SimulationConfig.ENERGY_COST_PER_PROCESSED_SYNAPSE;
    }
}
