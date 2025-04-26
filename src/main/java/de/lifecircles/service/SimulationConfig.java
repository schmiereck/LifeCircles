package de.lifecircles.service;

/**
 * Configuration parameters for the simulation.
 */
public class SimulationConfig {
    public static final double SCALE_SIMULATION = 3.0D;
    private double width = 1024 * SCALE_SIMULATION;
    private double height = 768 * SCALE_SIMULATION;
    private int targetUpdatesPerSecond = 60;
    private double timeStep = 1.0D / targetUpdatesPerSecond; // 60 Hz simulation
    private double cellInteractionRadius = 100.0D;
    private int initialCellCount = 30;

    // Sun ray energy settings
    private double sunRayRate = 5.0;
    private double energyPerRay = 0.1;
    // Spacing between sun rays in pixels; average one ray per this spacing
    private double sunRaySpacingPx = 120.0;
    // Energy threshold below which a cell dies
    private double energyDeathThreshold = 0.0;
    // Blocker repulsion strength for cell-blocker interactions
    private double blockerRepulsionStrength = 200.0;

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

    public double getScaleSimulation() {
        return SCALE_SIMULATION;
    }
}
