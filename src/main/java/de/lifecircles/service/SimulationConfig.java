package de.lifecircles.service;

import de.lifecircles.model.Vector2D;
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
     * Friction coefficient for cell rotation (damping)
     * A higher value means less rotation (0.0D to 1.0D).
     */
    private double rotationalFriction = 0.8D;
    /**
     * Viscosity coefficient for cell movement damping.
     * A higher value means less movement.
     */
    private double viscosity = 0.8D;

    private static final double CELL_REPULSION_STRENGTH = 125.0;

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

    private int initialCellCount = 30;
    public static final double REPOPULATION_THRESHOLD_PERCENT = 0.25;

    //-------------------------------------------------------------------------
    // Sun:

    private double sunRayRate = 10.0;
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
    private double cellDeathAge = 10000;

    //private double cellActorMaxForceStrength = 16.0D * 8.0D * 4.0D * 1.5D;
    /**
     * Maximum attractive force strength for cell actors (< 0).
     */
    private double cellActorMaxAttractiveForceStrength = 70.0D;
    /**
     * Maximum attractive force strength for cell actors (> 0).
     */
    private double cellActorMaxRepulsiveForceStrength = 150.0D;

    private double reproductionEnergyThreshold = 0.4D;
    private double reproductionAgeThreshold = 8.0D; // seconds
    private double reproductionDesireThreshold = 0.5D;

    private double mutationRate = 0.1D;
    private double mutationStrength = 0.2D;

    private double cellStateOutputThreshold = 0.5D; // Threshold für die Berechnung des Zell-Zustands

    /**
     * Default synapse connectivity for the neural network (0 - 1.0).
     */
    public static double brainSynapseConnectivityDefault = 0.05D;
    public static double stateHiddenLayerSynapseConnectivityDefault = 0.025D;
    public static double hiddenCountFactorDefault = 1.4D;

    // Verzögerung für Größenänderung in Millisekunden
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
     * Gets the friction coefficient used for cell rotation damping.
     */
    public double getRotationalFriction() {
        return this.rotationalFriction;
    }

    /**
     * Sets the friction coefficient used for cell rotation damping.
     */
    public void setRotationalFriction(double rotationalFriction) {
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
        return CELL_REPULSION_STRENGTH;
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

