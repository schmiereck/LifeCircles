package de.lifecircles.model;

import de.lifecircles.model.neural.CellBrain;
import de.lifecircles.model.neural.CellBrainService;
import de.lifecircles.model.neural.NeuralNetwork;
import de.lifecircles.service.EnergyCellCalcService;
import de.lifecircles.service.SimulationConfig;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a cell in the simulation.
 * Contains position, rotation, size, type, and sensor/actor points.
 * Behavior is controlled by a neural network brain.
 */
public class Cell {
    private Vector2D position;
    private Vector2D velocity;
    private double rotation; // in radians
    private double angularVelocity;
    private double radiusSize;
    private CellType type;
    private final List<SensorActor> sensorActors;
    private CellBrain brain;
    private double energy;
    private double age; // in seconds
    private double reproductionDesire; // neural net output for reproduction
    private int generation; // generation counter
    private boolean sunRayHit = false; // Flag to indicate if cell was hit by sun ray

    private int tempThinkHackCounter = SimulationConfig.CELL_TEMP_THINK_HACK_COUNTER_MAX;

    /**
     * Returns the index of the sensor that is currently at the top position.
     * @return Index of the topmost sensor (0-11)
     */
    public int getTopSensorIndex() {
        double maxY = Double.NEGATIVE_INFINITY;
        int topSensorIndex = 0; // Default to first sensor if positions aren't calculated yet
        
        for (int i = 0; i < sensorActors.size(); i++) {
            SensorActor sensor = sensorActors.get(i);
            Vector2D cachedPosition = sensor.getCachedPosition();
            
            if (cachedPosition != null) {
                double sensorY = cachedPosition.getY();
                if (sensorY > maxY) {
                    maxY = sensorY;
                    topSensorIndex = i;
                }
            }
        }

        return topSensorIndex;
    }

    public Cell(Vector2D position, final double radiusSize) {
        this.position = position;
        this.velocity = new Vector2D(0, 0);
        this.rotation = 0;
        this.angularVelocity = 0;
        this.radiusSize = radiusSize;
        this.type = new CellType(0, 0, 0);
        this.sensorActors = new ArrayList<>();
        initializeSensorActors();
        this.brain = new CellBrain(this);
        this.energy = SimulationConfig.CELL_MAX_ENERGY;
        this.age = 0.0;
        this.reproductionDesire = 0.0;
        this.generation = 0; // initialize generation counter
    }

    public Cell(Vector2D position, final double radiusSize, final NeuralNetwork neuralNetwork) {
        this.position = position;
        this.velocity = new Vector2D(0, 0);
        this.rotation = 0;
        this.angularVelocity = 0;
        this.radiusSize = radiusSize;
        this.type = new CellType(0, 0, 0);
        this.sensorActors = new ArrayList<>();
        initializeSensorActors();
        this.brain = new CellBrain(this, neuralNetwork);
        this.energy = SimulationConfig.CELL_MAX_ENERGY;
        this.age = 0.0;
        this.reproductionDesire = 0.0;
        this.generation = 0; // initialize generation counter
    }

    private void initializeSensorActors() {
        double angleStep = 2 * Math.PI / SimulationConfig.CELL_SENSOR_ACTOR_COUNT;
        for (int i = 0; i < SimulationConfig.CELL_SENSOR_ACTOR_COUNT; i++) {
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

    public double getRadiusSize() {
        return this.radiusSize;
    }

    public void setRadiusSize(double radiusSize) {
        this.radiusSize = Math.max(SimulationConfig.getInstance().getCellMinRadiusSize(),
                Math.min(SimulationConfig.getInstance().getCellMaxRadiusSize(), radiusSize));
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
     * Updates the cell's position, rotation, and behavior based on its current state.
     * @param deltaTime Time step in seconds
     */
    public void updateWithNeighbors(final double deltaTime) {
        // Update neural network
        final boolean useSynapseEnergyCost;
        if (this.tempThinkHackCounter >= SimulationConfig.CELL_TEMP_THINK_HACK_COUNTER_MAX) {
            CellBrainService.think(this);
            useSynapseEnergyCost = true;
            this.tempThinkHackCounter = 0;
        } else {
            useSynapseEnergyCost = false;
            this.tempThinkHackCounter++;
        }

        // Update physics
        this.position = this.position.add(this.velocity.multiply(deltaTime));
        this.rotation += this.angularVelocity * deltaTime;
        // Apply rotational friction
        this.angularVelocity *= (1.0D - SimulationConfig.getInstance().getRotationalFriction()) * deltaTime;
        
        // Normalize rotation to [0, 2π)
        rotation = rotation % (2 * Math.PI);
        if (rotation < 0) {
            rotation += 2 * Math.PI;
        }

        // Update energy and age
        EnergyCellCalcService.decayEnergy(this, deltaTime, useSynapseEnergyCost);
        // Mark cell death if energy below threshold
        if (energy < 0.0D) {
            this.energy = 0.0D;
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
        velocity = velocity.add(force.multiply(1.0 / radiusSize)); // Larger cells are affected less

        // Calculate torque and angular acceleration
        //Vector2D radiusVector = applicationPoint.subtract(position);
        final double xRadius = applicationPoint.getX() - position.getX();
        final double yRadius = applicationPoint.getY() - position.getY();
        //double torque = radiusVector.getX() * force.getY() - radiusVector.getY() * force.getX();
        double torque = xRadius * force.getY() - yRadius * force.getX();
        angularVelocity += torque / (radiusSize * radiusSize); // Moment of inertia approximated as size²
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
        this.energy = Math.max(0.0, Math.min(SimulationConfig.CELL_MAX_ENERGY, energy));
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
