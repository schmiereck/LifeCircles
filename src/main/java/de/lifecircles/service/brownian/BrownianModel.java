package de.lifecircles.service.brownian;

/** Selectable implementations; OFF leaves the cells alone. */
public enum BrownianModel {
    OFF(null),
    /** Single particle hits (Poisson), the default when switched on. */
    COLLISIONS(new CollisionBrownianMotion()),
    /** Gaussian push per step with the same mean square effect. */
    LANGEVIN(new LangevinBrownianMotion());

    private final BrownianMotion motion;
    BrownianModel(BrownianMotion motion) { this.motion = motion; }
    public BrownianMotion motion() { return motion; }
}
