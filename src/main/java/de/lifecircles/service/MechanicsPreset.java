package de.lifecircles.service;

/** Measured mechanical starting points; not a guarantee for arbitrary organisms. */
public enum MechanicsPreset {
    LEGACY(0, 0.8),
    BALANCED(0.25, 3.0);

    private final double softening;
    private final double rotationDamping;

    MechanicsPreset(double softening, double rotationDamping) {
        this.softening = softening;
        this.rotationDamping = rotationDamping;
    }

    public void apply(SimulationConfig config) {
        config.setCellActorMaxAttractiveForceStrength(90);
        config.setCellRepulsionStrength(125);
        config.setViscosity(0.8);
        config.setRotationalFriction(rotationDamping);
        config.setActorFieldSofteningFraction(softening);
    }
}
