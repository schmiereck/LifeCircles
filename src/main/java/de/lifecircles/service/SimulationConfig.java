package de.lifecircles.service;

/**
 * Configuration parameters for the simulation.
 */
public class SimulationConfig {
    public double scaleSimulation = 2.0D;
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
    // Age in Seconds after which a cell is considered dead.
    private int cellDeathAge = 100;
    // Blocker repulsion strength for cell-blocker interactions
    private double blockerRepulsionStrength = 200.0;
    /** 
     * Friction coefficient for cell rotation (damping)
     * A higher value means less rotation.
     */
    private double rotationalFriction = 0.75D;
    /** 
     * Viscosity coefficient for cell movement damping.
     * A higher value means less movement.
     */
    private double viscosity = 2.75D;

    public static final double ACTOR_INTERACTION_FORCE = 16.0D * 2.0D * 1.0D;
    private static final double CELL_REPULSION_STRENGTH = 75.0;

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
        return this.cellMaxRadius;
    }

    public void setCellMaxRadius(double cellMaxRadius) {
        this.cellMaxRadius = cellMaxRadius;
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

    public double getActorInteractionForce() {
        return ACTOR_INTERACTION_FORCE;
    }

    public double getCellRepulsionStrength() {
        return CELL_REPULSION_STRENGTH;
    }
}
