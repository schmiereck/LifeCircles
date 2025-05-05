package de.lifecircles.model;

import de.lifecircles.model.neural.CellBrain;
import de.lifecircles.model.neural.CellBrainService;
import de.lifecircles.service.EnergyCellCalcService;
import de.lifecircles.service.SimulationConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a cell in the simulation.
 * Contains position, rotation, size, type, and sensor/actor points.
 * Behavior is controlled by a neural network brain.
 */
public class Cell {
    private static final int SENSOR_ACTOR_COUNT = 12;
    private static final double MIN_SIZE = 10.0;
    private static final double MAX_SIZE = 50.0;
    public static final double MAX_ENERGY = 1.0;
    private static final int TEMP_THINK_HACK_COUNTER_MAX = 20;

    private Vector2D position;
    private Vector2D velocity;
    private double rotation; // in radians
    private double angularVelocity;
    private double size;
    private CellType type;
    private final List<SensorActor> sensorActors;
    private CellBrain brain;
    private double energy;
    private double age; // in seconds
    private double reproductionDesire; // neural net output for reproduction
    private int generation; // generation counter
    private boolean sunRayHit = false; // Flag to indicate if cell was hit by sun ray

    private int tempThinkHackCounter = 0;

    /**
     * Returns the index of the sensor that is currently at the top position.
     * @return Index of the topmost sensor (0-11)
     */
    public int getTopSensorIndex() {
        double maxY = Double.NEGATIVE_INFINITY;
        int topSensorIndex = -1;
        
        for (int i = 0; i < sensorActors.size(); i++) {
            SensorActor sensor = sensorActors.get(i);
            double sensorY = sensor.getCachedPosition().getY();
            if (sensorY > maxY) {
                maxY = sensorY;
                topSensorIndex = i;
            }
        }
        return topSensorIndex;
    }

    public Cell(Vector2D position, final double size) {
        this.position = position;
        this.velocity = new Vector2D(0, 0);
        this.rotation = 0;
        this.angularVelocity = 0;
        this.size = size;
        this.type = new CellType(0, 0, 0);
        this.sensorActors = new ArrayList<>();
        initializeSensorActors();
        this.brain = new CellBrain(this);
        this.energy = MAX_ENERGY;
        this.age = 0.0;
        this.reproductionDesire = 0.0;
        this.generation = 0; // initialize generation counter
    }

    private void initializeSensorActors() {
        double angleStep = 2 * Math.PI / SENSOR_ACTOR_COUNT;
        for (int i = 0; i < SENSOR_ACTOR_COUNT; i++) {
            sensorActors.add(new SensorActor(this, i * angleStep));
        }
    }

    public Vector2D getPosition() {
        return position;
    }

    public void setPosition(Vector2D position) {
        this.position = position;
    }

    public Vector2D getVelocity() {
        return velocity;
    }

    public void setVelocity(Vector2D velocity) {
        this.velocity = velocity;
    }

    public double getRotation() {
        return rotation;
    }

    public void setRotation(double rotation) {
        this.rotation = rotation;
    }

    /**
     * Notifies the cell that it has been hit by a SunRay.
     * This method can be used to trigger cell behavior changes when hit by sunlight.
     */
    public void notifySunRayHit() {
        sunRayHit = true;
    }

    public void resetSunRayHit() {
        sunRayHit = false;
    }

    public boolean isSunRayHit() {
        return sunRayHit;
    }

    public double getAngularVelocity() {
        return angularVelocity;
    }

    public void setAngularVelocity(double angularVelocity) {
        this.angularVelocity = angularVelocity;
    }

    public double getSize() {
        return size;
    }

    public void setSize(double size) {
        this.size = Math.max(MIN_SIZE, Math.min(MAX_SIZE, size));
    }

    public CellType getType() {
        return type;
    }

    public void setType(CellType type) {
        this.type = type;
    }

    public List<SensorActor> getSensorActors() {
        return this.sensorActors;
    }

    /**
     * Returns the maximum allowed cell size (diameter).
     * Used for spatial grid cell sizing.
     */
    public static double getMaxSize() {
        return MAX_SIZE;
    }

    /**
     * Updates the cell's position, rotation, and behavior based on its current state.
     * @param deltaTime Time step in seconds
     * @param neighbors List of neighboring cells
     */
    public void updateWithNeighbors(final double deltaTime, final List<Cell> neighbors) {
        // Update neural network
        final boolean useSynapseEnergyCost;
        if (this.tempThinkHackCounter > TEMP_THINK_HACK_COUNTER_MAX) {
            CellBrainService.think(this, neighbors);
            useSynapseEnergyCost = true;
            this.tempThinkHackCounter = 0;
        } else {
            useSynapseEnergyCost = false;
            this.tempThinkHackCounter++;
        }

        // Update physics
        position = position.add(velocity.multiply(deltaTime));
        rotation += angularVelocity * deltaTime;
        // Apply rotational friction
        angularVelocity *= (1.0D - SimulationConfig.getInstance().getRotationalFriction() * deltaTime);
        
        // Normalize rotation to [0, 2π)
        rotation = rotation % (2 * Math.PI);
        if (rotation < 0) {
            rotation += 2 * Math.PI;
        }

        // Update energy and age
        EnergyCellCalcService.decayEnergy(this, deltaTime, useSynapseEnergyCost);
        // Mark cell death if energy below threshold
        if (energy <= SimulationConfig.getInstance().getEnergyDeathThreshold()) {
            this.energy = 0.0;
        }
        this.age += deltaTime;
    }

    /**
     * Applies a force to the cell at a specific point.
     * This will affect both linear and angular velocity.
     * @param force Force vector
     * @param applicationPoint Point where the force is applied
     */
    public void applyForce(Vector2D force, Vector2D applicationPoint, double deltaTime) {
        // Linear acceleration
        velocity = velocity.add(force.multiply(1.0 / size)); // Larger cells are affected less

        // Calculate torque and angular acceleration
        //Vector2D radiusVector = applicationPoint.subtract(position);
        final double xRadius = applicationPoint.getX() - position.getX();
        final double yRadius = applicationPoint.getY() - position.getY();
        //double torque = radiusVector.getX() * force.getY() - radiusVector.getY() * force.getX();
        double torque = xRadius * force.getY() - yRadius * force.getX();
        angularVelocity += torque / (size * size); // Moment of inertia approximated as size²
    }

    public double getEnergy() {
        return energy;
    }

    public double getReproductionDesire() {
        return reproductionDesire;
    }

    public void setReproductionDesire(double reproductionDesire) {
        this.reproductionDesire = Math.max(0.0, Math.min(1.0, reproductionDesire));
    }

    public double getAge() {
        return this.age;
    }

    public void setEnergy(double energy) {
        this.energy = Math.max(0.0, Math.min(MAX_ENERGY, energy));
    }

    public void setBrain(CellBrain brain) {
        this.brain = brain;
    }

    public CellBrain getBrain() {
        return this.brain;
    }

    /**
     * Returns the generation counter of this cell.
     */
    public int getGeneration() {
        return generation;
    }

    /**
     * Sets the generation counter of this cell.
     */
    public void setGeneration(int generation) {
        this.generation = generation;
    }
}
