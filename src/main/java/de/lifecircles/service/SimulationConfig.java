package de.lifecircles.service;

import de.lifecircles.model.Vector2D;
import de.lifecircles.model.EnergyType;
import de.lifecircles.service.trainStrategy.TrainMode;

/**
 * Configuration parameters for the simulation.
 */
public class SimulationConfig {

    //-------------------------------------------------------------------------
    // Environment:

    public static final double GRAVITY = 9.81D * 3.0D;
    public static final Vector2D GRAVITY_VECTOR = new Vector2D(0, GRAVITY);

    // Environment-Cell:

    /**
     * Nonnegative rotational damping rate in inverse simulation seconds.
     * omega(t) = omega(0) * exp(-rate * t); zero disables damping.
     */
    private double rotationalFriction = 0.8D;
    /**
     * Viscosity coefficient for cell movement damping.
     * A higher value means less movement.
     */
    private double viscosity = 0.8D;

    private double cellRepulsionStrength = 125.0;
    // Fraction of the emitting actor's field radius with a linear force core.
    // Zero reproduces the former direction discontinuity at coincident actors.
    private double actorFieldSofteningFraction = 0.25;

    // --- Neue Konstanten für Distanzfaktoren bei Energieübertragung ---
    /**
     * Am fernsten Punkt (max. Sensorreichweite) ist die Delivery-Effizienz 80%.
     */
    public static final double CELL_ENERGY_DELIVERY_FACTOR_FAR = 0.8D;
    /**
     * Am fernsten Punkt (max. Sensorreichweite) ist die Absorptions-Effizienz 70%.
     */
    public static final double CELL_ENERGY_ABSORPTION_FACTOR_FAR = 0.7D;

    // Environment-Blocker:

    // Blocker repulsion strength for cell-blocker interactions
    private double blockerRepulsionStrength = 400.0;

    // Environment-Mode:

    public double scaleSimulation = 2.0D;
    private double width = 1024 * 2;// * SCALE_SIMULATION;
    private double height = 1200;// * SCALE_SIMULATION;
    public static final double FPS = 60.0D;
    public static final double initialCalcFps = 60.0D; // 60 Hz simulation
    private final double calcTimeStep = 1.0D / initialCalcFps; // 60 Hz simulation
    public static final double initialRunFps = 60.0D; // 60 Hz simulation
    private double runTimeStep = 1.0D / initialRunFps; // 60 Hz simulation

    //-------------------------------------------------------------------------
    // Training:

    // Training mode configuration
    private TrainMode trainMode = TrainMode.NONE;

    // Used by explicit initialization/reset, never a population floor during simulation.
    private int initialCellCount = 30;

    //-------------------------------------------------------------------------
    // Sun:

    private volatile double sunRayRate = 10.0;
    private volatile EnergyType sunEnergyType = EnergyType.PRIMARY;

    /** Season length in simulation seconds; 0 switches seasons off (constant summer). */
    private volatile double seasonDuration = 0;
    /** Sun intensity factor at mid-winter, relative to mid-summer, in [0,1]. */
    private volatile double winterIntensity = .25;
    public double getSeasonDuration() { return seasonDuration; }
    public void setSeasonDuration(double seconds) {
        if (!Double.isFinite(seconds) || seconds < 0) throw new IllegalArgumentException("Season duration must be finite and >= 0");
        seasonDuration = seconds;
    }
    public double getWinterIntensity() { return winterIntensity; }
    public void setWinterIntensity(double factor) {
        if (!Double.isFinite(factor) || factor < 0 || factor > 1) throw new IllegalArgumentException("Winter intensity must be in [0,1]");
        winterIntensity = factor;
    }

    //-------------------------------------------------------------------------
    // Durable adhesion (ZELLVERBINDUNGEN.md), only in policies that opt in:

    private volatile boolean cellAdhesion = true;
    /** Training worlds (all modes except NONE) use adhesion only with this opt-in. */
    private volatile boolean trainingAdhesion = false;
    public boolean isTrainingAdhesion() { return trainingAdhesion; }
    public void setTrainingAdhesion(boolean training) { trainingAdhesion = training; }
    /** Time constant with which the effective actor force follows the brain command. */
    private volatile double actorForceTonusSeconds = .5;
    /** Half-life of the forces held by remains; infinity keeps them ("wood"). */
    private volatile double remainsForceHalfLife = 300;
    /** Exponent applied to actor-colour and to body-type similarity. */
    private volatile double adhesionSharpness = 4;
    /** Share of a foreign partner's attraction that acts as repulsion instead. */
    private volatile double adhesionRejection = .5;
    /** Paused brains and remains keep their fields (otherwise both drop them to 0 as before). */
    private volatile boolean adhesionHoldForces = true;
    /** Colour/body-type affinity; off means colour-blind fields as before. */
    private volatile boolean adhesionRecognition = true;
    /** Stage 3: touching attractive actors lock into bonds (experimental, off by default). */
    private volatile boolean adhesionBonds = false;
    /** Stage 4: a daughter stays bonded to the mother if the dividing actor attracts (off by default). */
    private volatile boolean adhesionBirthBonds = false;
    /** A locked pair's field is this many times its frozen attraction (a plain field is a weak spring). */
    private volatile double bondFieldFactor = 3;
    public double getBondFieldFactor() { return bondFieldFactor; }
    public void setBondFieldFactor(double factor) {
        if (!Double.isFinite(factor) || factor <= 0) throw new IllegalArgumentException("Bond factor must be > 0");
        bondFieldFactor = factor;
    }
    // Ground hold (BODENHALT.md), experimental, off by default and only with cell adhesion:
    /** Stage A: blocker surfaces attract an actor's own attraction. */
    private volatile boolean groundAdhesion = false;
    /** Stage B: actors touching a blocker while attracting lock to a fixed anchor point (needs stage A). */
    private volatile boolean groundAnchors = false;
    /** Anchors only on GROUND blockers, no climbing on walls or platforms. */
    private volatile boolean groundAnchorsGroundOnly = false;
    /** Field strength of the blockers themselves (<= 0): a sticky floor for every actor. */
    private volatile double groundStickiness = 0;
    public boolean isGroundAdhesion() { return groundAdhesion; }
    public void setGroundAdhesion(boolean ground) { groundAdhesion = ground; }
    public boolean isGroundAnchors() { return groundAnchors; }
    public void setGroundAnchors(boolean anchors) { groundAnchors = anchors; }
    public boolean isGroundAnchorsGroundOnly() { return groundAnchorsGroundOnly; }
    public void setGroundAnchorsGroundOnly(boolean groundOnly) { groundAnchorsGroundOnly = groundOnly; }
    public double getGroundStickiness() { return groundStickiness; }
    public void setGroundStickiness(double strength) {
        if (!Double.isFinite(strength) || strength > 0) throw new IllegalArgumentException("Ground stickiness must be finite and <= 0");
        groundStickiness = strength;
    }
    public boolean isAdhesionBonds() { return adhesionBonds; }
    public void setAdhesionBonds(boolean bonds) { adhesionBonds = bonds; }
    public boolean isAdhesionBirthBonds() { return adhesionBirthBonds; }
    public void setAdhesionBirthBonds(boolean bonds) { adhesionBirthBonds = bonds; }
    public boolean isAdhesionHoldForces() { return adhesionHoldForces; }
    public void setAdhesionHoldForces(boolean hold) { adhesionHoldForces = hold; }
    public boolean isAdhesionRecognition() { return adhesionRecognition; }
    public void setAdhesionRecognition(boolean recognition) { adhesionRecognition = recognition; }
    public boolean isCellAdhesion() { return cellAdhesion; }
    public void setCellAdhesion(boolean adhesion) { cellAdhesion = adhesion; }
    public double getActorForceTonusSeconds() { return actorForceTonusSeconds; }
    public void setActorForceTonusSeconds(double seconds) {
        if (!Double.isFinite(seconds) || seconds < 0) throw new IllegalArgumentException("Tonus time constant must be finite and >= 0");
        actorForceTonusSeconds = seconds;
    }
    public double getRemainsForceHalfLife() { return remainsForceHalfLife; }
    public void setRemainsForceHalfLife(double seconds) {
        if (Double.isNaN(seconds) || seconds <= 0) throw new IllegalArgumentException("Half-life must be > 0 (infinity allowed)");
        remainsForceHalfLife = seconds;
    }
    public double getAdhesionSharpness() { return adhesionSharpness; }
    public void setAdhesionSharpness(double exponent) {
        if (!Double.isFinite(exponent) || exponent < 0) throw new IllegalArgumentException("Sharpness must be finite and >= 0");
        adhesionSharpness = exponent;
    }
    public double getAdhesionRejection() { return adhesionRejection; }
    public void setAdhesionRejection(double share) {
        if (!Double.isFinite(share) || share < 0 || share > 1) throw new IllegalArgumentException("Rejection must be in [0,1]");
        adhesionRejection = share;
    }

    /** Location-dependent forms (see SunSpectrum) instead of the single sun energy type. */
    private volatile boolean sunSpectralZones = false;
    public boolean isSunSpectralZones() { return sunSpectralZones; }
    public void setSunSpectralZones(boolean zones) { sunSpectralZones = zones; }
    /** Width of one spectral zone in px; the four zones repeat. 0 = one cycle per world (a quarter of the width). */
    private volatile double sunZoneWidth = 0;
    public double getSunZoneWidth() { return sunZoneWidth; }
    public void setSunZoneWidth(double width) {
        if (!Double.isFinite(width) || width < 0) throw new IllegalArgumentException("Zone width must be finite and >= 0");
        sunZoneWidth = width;
    }

    public EnergyType getSunEnergyType() { return sunEnergyType; }
    public void setSunEnergyType(EnergyType energyType) {
        sunEnergyType = java.util.Objects.requireNonNull(energyType);
    }
    private volatile BrainEnergyCosts brainEnergyCosts = BrainEnergyCosts.DEFAULT;

    public BrainEnergyCosts getBrainEnergyCosts() { return brainEnergyCosts; }
    /** One live multiplier for both stored DNA and the original per-decision work bill. */
    public void setBrainEnergyMultiplier(double multiplier) {
        brainEnergyCosts = new BrainEnergyCosts(multiplier, multiplier);
    }
    private double energyPerRay = 0.01D; //0.005; //0.015; // 0.025;
    // Spacing between sun rays in pixels; average one ray per this spacing
    private double sunRaySpacingPx = 60.0;

    // Day/Night cycle duration in seconds
    public static final double SUN_DAY_NIGHT_CYCLE_DURATION = 60.0D * 2.0D;

    /**
     * Intensity of sun rays during the night (0.0 to 1.0).
     */
    public static final double SUN_NIGHT_INTENSITY = 0.4D;

    // Sonnenwinkel in Grad (konfigurierbar)
    public static final double SUN_ANGLE_MORNING_DEG = 360.0D + 35.0D; // von links
    public static final double SUN_ANGLE_EVENING_DEG = 360.0D - 35.0D; // von rechts (180-35)

    //-------------------------------------------------------------------------
    // Cell:
    public static final int CELL_SENSOR_ACTOR_COUNT = 12;
    public static final double CELL_MAX_ENERGY = 1.0D;
    public static final int CELL_TEMP_THINK_HACK_COUNTER_MAX = 10;

    public static final double ENERGY_DECAY_RATE = 0.003D;
    public static final double ENERGY_COST_PER_SYNAPSE           = 0.0000000003D;
    public static final double ENERGY_COST_PER_PROCESSED_SYNAPSE = 0.00000002D;

    public static final double CELL_ANGULAR_VELOCITY_DIFF = 20.0D;

    /**
     * Anzahl zusätzliche Hidden-Layer die abhängig vom Cell-State aktiv sein können.
     */
    public static final int CELL_STATE_ACTIVE_LAYER_COUNT = 3;

    // Konstante für die Zellwachstumszeit in Sekunden
    public static final double CELL_GROWTH_DURATION = 1.0D;

    private double cellMinRadiusSize = 6.0D;
    private double cellMinGrowRadiusSize = 0.5D; //2.0D;
    private double cellMaxRadiusSize = 20.0D;

    public static double cellActorMaxFieldRadiusFactor = 0.9D;
    private double cellActorMaxFieldRadius =
            SensorActorForceCellCalcService.calcSensorRadius(this.cellMaxRadiusSize, CELL_SENSOR_ACTOR_COUNT);

    // Age in Seconds after which a cell is considered dead.
    private volatile double cellDeathAge = 10000;

    //private double cellActorMaxForceStrength = 16.0D * 8.0D * 4.0D * 1.5D;
    /**
     * Maximum attractive force strength for cell actors (< 0).
     */
    private double cellActorMaxAttractiveForceStrength = 90.0D;
    /**
     * Maximum attractive force strength for cell actors (> 0).
     */
    private double cellActorMaxRepulsiveForceStrength = 150.0D;

    private double reproductionEnergyThreshold = 0.4D;
    private double reproductionAgeThreshold = 8.0D; // seconds
    private double reproductionDesireThreshold = 0.5D;

    private double mutationRate = 0.1D;
    private double mutationStrength = 0.2D;
    // Natural division only; zero enables clonal development. Training keeps the global rate.
    private double divisionMutationRateMultiplier = 1;
    private boolean normalizeBrainInputs = false;

    private double cellStateOutputThreshold = 0.5D; // Threshold für die Berechnung des Zell-Zustands

    /**
     * Default synapse connectivity for the neural network (0 - 1.0).
     */
    // Founders: 24 shared hidden neurons plus three independently gated experts of eight.
    public static final int DEFAULT_BRAIN_TRUNK_NEURONS = 24;
    public static final int DEFAULT_BRAIN_STATE_NEURONS = 8;
    public static final double DEFAULT_BRAIN_CONNECTIVITY = 0.1;
    public static final double DEFAULT_BRAIN_STATE_CONNECTIVITY = 0.025;

    // Historical factor-based settings for explicit experimental constructors/studies only.
    public static double hiddenLayerSynapseConnectivityDefault = 0.025D;
    public static double stateHiddenLayerSynapseConnectivityDefault = 0.005D;
    public static double hiddenCountFactorDefault = 1.4D;

    // Zeitkonstante der Groessenglaettung in Simulations-Millisekunden; <= 0: sofort im naechsten Zeitschritt
    private long sizeChangeDelay = 100;

    //-------------------------------------------------------------------------
    // Singleton instance
    private static final SimulationConfig INSTANCE = new SimulationConfig();

    private SimulationConfig() {}

    public static SimulationConfig getInstance() {
        return INSTANCE;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public double getCalcTimeStep() {
        return this.calcTimeStep;
    }

    public double getRunTimeStep() {
        return this.runTimeStep;
    }

    public void setRunTimeStep(double runTimeStep) {
        this.runTimeStep = runTimeStep;
    }

    public double getCellMinRadiusSize() {
        return this.cellMinRadiusSize;
    }

    public void setCellMinRadiusSize(double cellMinRadiusSize) {
        this.cellMinRadiusSize = cellMinRadiusSize;
    }

    public void setCellMinGrowRadiusSize(double cellMinGrowRadiusSize) {
        this.cellMinGrowRadiusSize = cellMinGrowRadiusSize;
    }

    public double getCellMinGrowRadiusSize() {
        return this.cellMinGrowRadiusSize;
    }

    public double getCellMaxRadiusSize() {
        return this.cellMaxRadiusSize;
    }

    public void setCellMaxRadiusSize(double cellMaxRadiusSize) {
        this.cellMaxRadiusSize = cellMaxRadiusSize;
    }

    public double getCellDeathAge() {
        return this.cellDeathAge;
    }

    public void setCellDeathAge(double cellDeathAge) {
        if (!Double.isFinite(cellDeathAge) || cellDeathAge <= 0)
            throw new IllegalArgumentException("Cell lifespan must be finite and positive");
        this.cellDeathAge = cellDeathAge;
    }

    public int getInitialCellCount() {
        return this.initialCellCount;
    }

    public void setInitialCellCount(int initialCellCount) {
        this.initialCellCount = initialCellCount;
    }

    public double getSunRayRate() {
        return this.sunRayRate;
    }

    public void setSunRayRate(double sunRayRate) {
        this.sunRayRate = sunRayRate;
    }

    public double getEnergyPerRay() {
        return this.energyPerRay;
    }

    public void setEnergyPerRay(double energyPerRay) {
        this.energyPerRay = energyPerRay;
    }

    public double getSunRaySpacingPx() {
        return this.sunRaySpacingPx;
    }

    public void setSunRaySpacingPx(double sunRaySpacingPx) {
        this.sunRaySpacingPx = sunRaySpacingPx;
    }

    public double getBlockerRepulsionStrength() {
        return this.blockerRepulsionStrength;
    }

    public void setBlockerRepulsionStrength(double blockerRepulsionStrength) {
        this.blockerRepulsionStrength = blockerRepulsionStrength;
    }

    /**
     * Gets the rotational damping rate in inverse simulation seconds.
     */
    public double getRotationalFriction() {
        return this.rotationalFriction;
    }

    /**
     * Sets the finite, nonnegative rotational damping rate (1/s).
     * @throws IllegalArgumentException if the rate is negative or non-finite
     */
    public void setRotationalFriction(double rotationalFriction) {
        if (!Double.isFinite(rotationalFriction) || rotationalFriction < 0) {
            throw new IllegalArgumentException("Rotational damping must be finite and nonnegative");
        }
        this.rotationalFriction = rotationalFriction;
    }

    public double getScaleSimulation() {
        return this.scaleSimulation;
    }

    public void setScaleSimulation(final double scaleSimulation) {
        this.scaleSimulation = scaleSimulation;
    }

    public TrainMode getTrainMode() {
        return this.trainMode;
    }

    public void setTrainMode(TrainMode trainMode) {
        this.trainMode = trainMode;
    }

    public double getViscosity() {
        return this.viscosity;
    }

    public void setViscosity(final double viscosity) {
        this.viscosity = viscosity;
    }

    public double getCellRepulsionStrength() {
        return cellRepulsionStrength;
    }

    /** Maximum attractive actor-field magnitude, independently tunable from contact stiffness. */
    public void setCellActorMaxAttractiveForceStrength(double strength) {
        if (!Double.isFinite(strength) || strength <= 0) {
            throw new IllegalArgumentException("Maximum attraction must be finite and positive");
        }
        cellActorMaxAttractiveForceStrength = strength;
    }

    /** Contact stiffness, independently tunable from attractive actor fields. */
    public void setCellRepulsionStrength(double strength) {
        if (!Double.isFinite(strength) || strength < 0) {
            throw new IllegalArgumentException("Cell repulsion strength must be finite and nonnegative");
        }
        cellRepulsionStrength = strength;
    }

    public double getActorFieldSofteningFraction() { return actorFieldSofteningFraction; }

    public void setActorFieldSofteningFraction(double fraction) {
        if (!Double.isFinite(fraction) || fraction < 0 || fraction > 1) {
            throw new IllegalArgumentException("Actor field softening must be in [0, 1]");
        }
        actorFieldSofteningFraction = fraction;
    }

    public double getReproductionEnergyThreshold() {
        return this.reproductionEnergyThreshold;
    }

    public void setReproductionEnergyThreshold(double reproductionEnergyThreshold) {
        this.reproductionEnergyThreshold = reproductionEnergyThreshold;
    }

    public double getReproductionAgeThreshold() {
        return this.reproductionAgeThreshold;
    }

    public void setReproductionAgeThreshold(double reproductionAgeThreshold) {
        this.reproductionAgeThreshold = reproductionAgeThreshold;
    }

    public double getMutationRate() { return this.mutationRate; }
    public void setMutationRate(double rate) { this.mutationRate = rate; }

    public double getMutationStrength() { return this.mutationStrength; }
    public void setMutationStrength(double strength) { this.mutationStrength = strength; }

    public double getDivisionMutationRateMultiplier() { return divisionMutationRateMultiplier; }
    public void setDivisionMutationRateMultiplier(double multiplier) {
        if (!Double.isFinite(multiplier) || multiplier < 0 || multiplier > 1)
            throw new IllegalArgumentException("Division mutation multiplier must be in [0,1]");
        divisionMutationRateMultiplier = multiplier;
    }
    public boolean isNormalizeBrainInputs() { return normalizeBrainInputs; }
    public void setNormalizeBrainInputs(boolean normalize) { normalizeBrainInputs = normalize; }

    public double getReproductionDesireThreshold() { return this.reproductionDesireThreshold; }
    public void setReproductionDesireThreshold(double threshold) { this.reproductionDesireThreshold = threshold; }

    public double getCellActorMaxAttractiveForceStrength() {
        return this.cellActorMaxAttractiveForceStrength;
    }

    public double getCellActorMaxRepulsiveForceStrength() {
        return this.cellActorMaxRepulsiveForceStrength;
    }

    public double getCellActorMaxFieldRadius() {
        return this.cellActorMaxFieldRadius;
    }

    public double getCellStateOutputThreshold() {
        return this.cellStateOutputThreshold;
    }

    public void setCellStateOutputThreshold(double cellStateOutputThreshold) {
        this.cellStateOutputThreshold = cellStateOutputThreshold;
    }

    // Getter und Setter für die Verzögerung
    public long getSizeChangeDelay() {
        return sizeChangeDelay;
    }

    public void setSizeChangeDelay(long sizeChangeDelay) {
        this.sizeChangeDelay = sizeChangeDelay;
    }
}

