package de.lifecircles.service;

import de.lifecircles.model.Vector2D;
import de.lifecircles.service.trainStrategy.TrainMode;

/**
 * Configuration parameters for the simulation.
 */
public class SimulationConfig {
    // Environment:
    public static final double GRAVITY = 9.81;
    public static final Vector2D GRAVITY_VECTOR = new Vector2D(0, GRAVITY);
    public static final double REPOPULATION_THRESHOLD_PERCENT = 0.25;

    // Sun:

    private double sunRayRate = 5.0;
    private double energyPerRay = 0.02;
    // Spacing between sun rays in pixels; average one ray per this spacing
    private double sunRaySpacingPx = 60.0;

    // Day/Night cycle duration in seconds
    public static final double DAY_NIGHT_CYCLE_DURATION = 60.0D * 2;

    // View:
    public double scaleSimulation = 2.0D;
    private double width = 1024 * 2;// * SCALE_SIMULATION;
    private double height = 1200;// * SCALE_SIMULATION;
    private double timeStep = 1.0D / 60.0D; // 60 Hz simulation

    // Cell:
    public static final int CELL_SENSOR_ACTOR_COUNT = 12;
    public static final double CELL_MAX_ENERGY = 1.0;
    public static final int CELL_TEMP_THINK_HACK_COUNTER_MAX = 10;

    // Konstante für die Zellwachstumszeit in Sekunden
    public static final double CELL_GROWTH_DURATION = 2.0;

    private double cellMinRadiusSize = 10.0D;
    private double cellMaxRadiusSize = 50.0D;

    private double cellActorMaxFieldRadius =
            ActorSensorCellCalcService.calcSensorRadius(this.cellMaxRadiusSize, CELL_SENSOR_ACTOR_COUNT);

    private int initialCellCount = 30;

    // Training mode configuration
    private TrainMode trainMode = TrainMode.NONE;

    // Age in Seconds after which a cell is considered dead.
    private int cellDeathAge = 100;
    // Blocker repulsion strength for cell-blocker interactions
    private double blockerRepulsionStrength = 200.0;
    /** 
     * Friction coefficient for cell rotation (damping)
     * A higher value means less rotation (0.0D to 1.0D).
     */
    private double rotationalFriction = 0.9D;
    /** 
     * Viscosity coefficient for cell movement damping.
     * A higher value means less movement.
     */
    private double viscosity = 5.75D;

    private static final double CELL_REPULSION_STRENGTH = 175.0;

    private double cellActorMinForceStrength = 0.0D;
    private double cellActorMaxForceStrength = 16.0D * 4.0D * 2.0D;

    private double reproductionEnergyThreshold = 0.2D;
    private double reproductionAgeThreshold = 4.0D; // seconds
    private double reproductionDesireThreshold = 0.5;

    private double mutationRate = 0.1;
    private double mutationStrength = 0.2;

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

    public double getTimeStep() {
        return this.timeStep;
    }

    public void setTimeStep(double timeStep) {
        this.timeStep = timeStep;
    }

    public double getCellMinRadiusSize() {
        return this.cellMinRadiusSize;
    }

    public void setCellMinRadiusSize(double cellMinRadiusSize) {
        this.cellMinRadiusSize = cellMinRadiusSize;
    }

    public double getCellMaxRadiusSize() {
        return this.cellMaxRadiusSize;
    }

    public void setCellMaxRadiusSize(double cellMaxRadiusSize) {
        this.cellMaxRadiusSize = cellMaxRadiusSize;
    }

    public int getCellDeathAge() {
        return this.cellDeathAge;
    }

    public void setCellDeathAge(int cellDeathAge) {
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

    public double getCellActorMinForceStrength() {
        return this.cellActorMinForceStrength;
    }

    public double getCellActorMaxForceStrength() {
        return this.cellActorMaxForceStrength;
    }

    public double getCellActorMaxFieldRadius() {
        return this.cellActorMaxFieldRadius;
    }
}
