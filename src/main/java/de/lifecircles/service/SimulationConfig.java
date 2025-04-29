package de.lifecircles.service;

/**
 * Configuration parameters for the simulation.
 */
public class SimulationConfig {
    public static final double SCALE_SIMULATION = 2.0D;
    private double width = 1024 * 2;// * SCALE_SIMULATION;
    private double height = 1200;// * SCALE_SIMULATION;
    private int targetUpdatesPerSecond = 60;
    private double timeStep = 1.0D / targetUpdatesPerSecond; // 60 Hz simulation
    
    private double cellMaxRadius = 30.0D;
    private double cellInteractionRadius = 30.0D;

    private int initialCellCount = 30;

    // Training mode configuration
    private TrainMode trainMode = TrainMode.NONE;

    // Sun ray energy settings
    private double sunRayRate = 4.0;
    private double energyPerRay = 0.1;
    // Spacing between sun rays in pixels; average one ray per this spacing
    private double sunRaySpacingPx = 60.0;
    // Energy threshold below which a cell dies
    private double energyDeathThreshold = 0.0;
    // Blocker repulsion strength for cell-blocker interactions
    private double blockerRepulsionStrength = 200.0;
    // Friction coefficient for cell rotation (damping)
    private double rotationalFriction = 1.99D;

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
        return timeStep;
    }

    public void setTimeStep(double timeStep) {
        this.timeStep = timeStep;
    }

    public int getTargetUpdatesPerSecond() {
        return targetUpdatesPerSecond;
    }

    public void setTargetUpdatesPerSecond(int targetUpdatesPerSecond) {
        this.targetUpdatesPerSecond = targetUpdatesPerSecond;
    }

    public double getCellInteractionRadius() {
        return cellInteractionRadius;
    }

    public void setCellInteractionRadius(double cellInteractionRadius) {
        this.cellInteractionRadius = cellInteractionRadius;
    }

    public double getCellMaxRadius() {
        return cellMaxRadius;
    }

    public void setCellMaxRadius(double cellMaxRadius) {
        this.cellMaxRadius = cellMaxRadius;
    }

    public int getInitialCellCount() {
        return initialCellCount;
    }

    public void setInitialCellCount(int initialCellCount) {
        this.initialCellCount = initialCellCount;
    }

    public double getSunRayRate() {
        return sunRayRate;
    }

    public void setSunRayRate(double sunRayRate) {
        this.sunRayRate = sunRayRate;
    }

    public double getEnergyPerRay() {
        return energyPerRay;
    }

    public void setEnergyPerRay(double energyPerRay) {
        this.energyPerRay = energyPerRay;
    }

    public double getSunRaySpacingPx() {
        return sunRaySpacingPx;
    }

    public void setSunRaySpacingPx(double sunRaySpacingPx) {
        this.sunRaySpacingPx = sunRaySpacingPx;
    }

    public double getEnergyDeathThreshold() {
        return energyDeathThreshold;
    }

    public void setEnergyDeathThreshold(double energyDeathThreshold) {
        this.energyDeathThreshold = energyDeathThreshold;
    }

    public double getBlockerRepulsionStrength() {
        return blockerRepulsionStrength;
    }

    public void setBlockerRepulsionStrength(double blockerRepulsionStrength) {
        this.blockerRepulsionStrength = blockerRepulsionStrength;
    }

    /**
     * Gets the friction coefficient used for cell rotation damping.
     */
    public double getRotationalFriction() {
        return rotationalFriction;
    }

    /**
     * Sets the friction coefficient used for cell rotation damping.
     */
    public void setRotationalFriction(double rotationalFriction) {
        this.rotationalFriction = rotationalFriction;
    }

    public double getScaleSimulation() {
        return SCALE_SIMULATION;
    }

    public TrainMode getTrainMode() {
        return trainMode;
    }

    public void setTrainMode(TrainMode trainMode) {
        this.trainMode = trainMode;
    }
}
