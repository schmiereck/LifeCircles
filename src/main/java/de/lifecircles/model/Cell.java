package de.lifecircles.model;

import de.lifecircles.model.neural.*;
import de.lifecircles.service.SimulationConfig;
import java.util.ArrayList;
import java.util.List;
import java.io.*;
import java.util.Random;

/**
 * Represents a cell in the simulation.
 * Contains position, rotation, size, type, and sensor/actor points.
 * Behavior is controlled by a neural network brain.
 */
public class Cell implements SensableCell, Serializable {
    private static final long serialVersionUID = 1L;

    private Vector2D position;
    private Vector2D velocity;
    private transient Vector2D velocityForce;
    private double rotation; // in radians
    private double angularVelocity;
    private transient double angularVelocityForce;
    private double radiusSize;
    private double targetRadiusSize; // Zielgröße nach dem Wachstum
    private double growthAge; // Alter seit der Zellteilung für das Wachstum
    private boolean isGrowing; // Flag ob die Zelle sich im Wachstumsprozess befindet
    private CellType type;
    private final List<SensorActor> sensorActors;
    private CellBrainInterface brain;
    private double energy;
    private double age; // in seconds
    private int generation; // generation counter
    private transient boolean sunRayHit = false; // Flag to indicate if cell was hit by sun ray
    private int cellState; // Zustand der Zelle, beeinflusst zusätzliche Hidden-Layer
    private double mutationRateFactor; // Faktor für die Mutationsrate
    private double mutationStrengthFactor; // Faktor für die Mutationsstärke
    private transient long sizeChangeTimestamp = -1; // Zeitstempel für verzögerte Größenänderung
    private transient final Random random = new Random();
    //private int tempThinkHackCounter = SimulationConfig.CELL_TEMP_THINK_HACK_COUNTER_MAX;

    public Cell(final Vector2D position, final double radiusSize, final CellBrainInterface cellBrain) {
        this.position = position;
        this.velocity = new Vector2D(0, 0);
        this.velocityForce = new Vector2D(0, 0);
        this.rotation = 0.0D;
        this.angularVelocity = 0.0D;
        this.angularVelocityForce = 0.0D;
        this.radiusSize = radiusSize;
        this.targetRadiusSize = radiusSize;
        this.isGrowing = false;
        this.growthAge = 0;
        this.type = new CellType(this.random.nextDouble(), this.random.nextDouble(), this.random.nextDouble());
        this.sensorActors = this.createSensorActors(this);
        this.brain = cellBrain;
        this.energy = SimulationConfig.CELL_MAX_ENERGY;
        this.age = 0.0;
        this.generation = 0; // initialize generation counter
        this.mutationRateFactor = 1.0; // Standardwert
        this.mutationStrengthFactor = 1.0; // Standardwert
    }

    private static ArrayList<SensorActor> createSensorActors(final Cell cell) {
        final ArrayList<SensorActor> sensorActors = new ArrayList<>();
        double angleStep = 2 * Math.PI / SimulationConfig.CELL_SENSOR_ACTOR_COUNT;
        for (int i = 0; i < SimulationConfig.CELL_SENSOR_ACTOR_COUNT; i++) {
            sensorActors.add(new SensorActor(cell, i * angleStep));
        }
        return sensorActors;
    }

    public Vector2D getPosition() {
        return this.position;
    }

    public void setPosition(Vector2D position) {
        this.position = position;
    }

    public Vector2D getVelocity() {
        return this.velocity;
    }

    public void setVelocity(Vector2D velocity) {
        this.velocity = velocity;
    }

    public double getRotation() {
        return this.rotation;
    }

    public void setRotation(double rotation) {
        this.rotation = rotation;
    }

    /**
     * Notifies the cell that it has been hit by a SunRay.
     * This method can be used to trigger cell behavior changes when hit by sunlight.
     */
    public void notifySunRayHit() {
        this.sunRayHit = true;
    }

    public void resetSunRayHit() {
        this.sunRayHit = false;
    }

    public boolean isSunRayHit() {
        return this.sunRayHit;
    }

    public double getAngularVelocity() {
        return this.angularVelocity;
    }

    public void setAngularVelocity(double angularVelocity) {
        this.angularVelocity = angularVelocity;
    }

    public double getRadiusSize() {
        return this.radiusSize;
    }

    public void setRadiusSize(double radiusSize) {
        this.targetRadiusSize = Math.max(SimulationConfig.getInstance().getCellMinRadiusSize(),
                Math.min(SimulationConfig.getInstance().getCellMaxRadiusSize(), radiusSize));
        
        // Setze den Zeitstempel für die Verzögerung
        this.sizeChangeTimestamp = System.currentTimeMillis();
    }

    public void setRealRadiusSize(double radiusSize) {
        this.radiusSize = Math.max(SimulationConfig.getInstance().getCellMinRadiusSize(),
                Math.min(SimulationConfig.getInstance().getCellMaxRadiusSize(), radiusSize));
    }

    public void setRealGrowRadiusSize(double radiusSize) {
        this.radiusSize = Math.max(SimulationConfig.getInstance().getCellMinGrowRadiusSize(),
                Math.min(SimulationConfig.getInstance().getCellMaxRadiusSize(), radiusSize));
    }

    /**
     * Setzt die Zelle in den Wachstumsmodus und startet mit der Minimalgröße
     */
    public void startGrowthProcess() {
        this.radiusSize = SimulationConfig.getInstance().getCellMinGrowRadiusSize();
        this.isGrowing = true;
        this.growthAge = 0;
    }

    public double getTargetRadiusSize() {
        return this.targetRadiusSize;
    }

    public boolean isGrowing() {
        return this.isGrowing;
    }

    // Neue Methode, um die verzögerte Größenänderung zu prüfen und anzuwenden
    public void applyDelayedSizeChange() {
        if (this.sizeChangeTimestamp > 0) {
            long elapsedTime = System.currentTimeMillis() - this.sizeChangeTimestamp;
            if (elapsedTime >= SimulationConfig.getInstance().getSizeChangeDelay()) {
                this.radiusSize = this.targetRadiusSize;
                this.sizeChangeTimestamp = -1; // Zurücksetzen des Zeitstempels
            } else {
                // Berechne den Fortschritt der Verzögerung
                double progress = (double) elapsedTime / SimulationConfig.getInstance().getSizeChangeDelay();
                // Interpolieren zwischen der aktuellen Größe und der Zielgröße
                this.radiusSize = this.radiusSize + (this.targetRadiusSize - this.radiusSize) * progress;
            }
        }
    }

    @Override
    public CellType getType() {
        return this.type;
    }

    public void setType(CellType type) {
        this.type = type;
    }

    public List<SensorActor> getSensorActors() {
        return this.sensorActors;
    }

    /**
     * Applies a force to the cell at a specific point.
     * This will affect linear.
     *
     * @param force            Force vector
     */
    public void applyForce(Vector2D force) {
        // Linear acceleration
        //this.velocity = this.velocity.add(force.multiply(1.0D / this.radiusSize)); // Larger cells are affected less
        this.velocityForce = this.velocityForce.add(force.multiply(0.08D)); // Larger cells are affected less
        //this.velocity = this.velocity.add(force); // Larger cells are affected less
    }

    /**
     * Applies a force to the cell at a specific point.
     * This will affect both linear and angular velocity.
     *
     * @param force            Force vector
     * @param applicationPoint Point where the force is applied
     */
    public void applyForce(Vector2D force, Vector2D applicationPoint) {
        // Linear acceleration
        //this.velocity = this.velocity.add(force.multiply(1.0D / this.radiusSize)); // Larger cells are affected less
        this.velocityForce = this.velocityForce.add(force.multiply(0.08D)); // Larger cells are affected less
        //this.velocity = this.velocity.add(force); // Larger cells are affected less

        // Calculate torque and angular acceleration
        //Vector2D radiusVector = applicationPoint.subtract(position);
        final double xRadius = applicationPoint.getX() - this.position.getX();
        final double yRadius = applicationPoint.getY() - this.position.getY();
        //double torque = radiusVector.getX() * force.getY() - radiusVector.getY() * force.getX();
        double torque = (xRadius * force.getY() - yRadius * force.getX()) / SimulationConfig.CELL_ANGULAR_VELOCITY_DIFF; // Scale down torque for stability
        this.angularVelocityForce += torque / (this.radiusSize * this.radiusSize); // Moment of inertia approximated as size²
    }

    /**
     * velocityForce to velocity.
     * angularVelocityForce to angularVelocity
     */
    public void updateForce() {
        this.velocity = this.velocity.add(this.velocityForce);
        this.angularVelocity += this.angularVelocityForce;

        this.velocityForce = new Vector2D(0, 0);
        this.angularVelocityForce = 0.0D;
    }

    public double getMaxReproductionDesire() {
        return sensorActors.stream()
                .mapToDouble(SensorActor::getReproductionDesire)
                .max().orElse(0.0D);
    }

    @Override
    public double getAge() {
        return this.age;
    }

    @Override
    public void setEnergy(double energy) {
        this.energy = Math.max(0.0D, Math.min(SimulationConfig.CELL_MAX_ENERGY, energy));
    }

    @Override
    public double getEnergy() {
        return this.energy;
    }

    @Override
    public double getMaxEnergy() {
        return SimulationConfig.CELL_MAX_ENERGY;
    }

    public CellBrainInterface getBrain() {
        return this.brain;
    }

    /**
     * Returns the generation counter of this cell.
     */
    public int getGeneration() {
        return this.generation;
    }

    /**
     * Sets the generation counter of this cell.
     */
    public void setGeneration(int generation) {
        this.generation = generation;
    }

    //public int getTempThinkHackCounter() {
    //    return this.tempThinkHackCounter;
    //}
//
    //public void setTempThinkHackCounter(int tempThinkHackCounter) {
    //    this.tempThinkHackCounter = tempThinkHackCounter;
    //}

    public void setSunRayHit(boolean sunRayHit) {
        this.sunRayHit = sunRayHit;
    }

    public void setAge(double age) {
        this.age = age;
    }

    public void incAge(double age) {
        this.age += age;
    }

    public void setGrowing(boolean growing) {
        this.isGrowing = growing;
    }

    public double getGrowthAge() {
        return this.growthAge;
    }

    public void setGrowthAge(double growthAge) {
        this.growthAge = growthAge;
    }

    public void incGrowthAge(double growthAge) {
        this.growthAge += growthAge;
    }

    public void setTargetRadiusSize(double targetRadiusSize) {
        this.targetRadiusSize = targetRadiusSize;
    }

    //public void incTempThinkHackCounter() {
    //    this.tempThinkHackCounter++;
    //}

    public void setIsGrowing(boolean isGrowing) {
        this.isGrowing = isGrowing;
    }

    public int getCellState() {
        return this.cellState;
    }

    public void setCellState(int cellState) {
        this.cellState = cellState;
    }

    /**
     * Returns the cell state as a normalized value for external sensors.
     * Each bit of the cell state is treated as a separate feature.
     */
    @Override
    public double[] getNormalizedCellState() {
        double[] normalizedState = new double[SimulationConfig.CELL_STATE_ACTIVE_LAYER_COUNT];
        for (int i = 0; i < normalizedState.length; i++) {
            normalizedState[i] = (this.cellState & (1 << i)) != 0 ? 1.0 : 0.0;
        }
        return normalizedState;
    }

    /**
     * Returns the index of the sensor that is currently at the top position.
     * @return Index of the topmost sensor (0-11)
     */
    public int getTopSensorIndex() {
        double maxY = Double.NEGATIVE_INFINITY;
        int topSensorIndex = 0; // Default to first sensor if positions aren't calculated yet

        for (int i = 0; i < this.sensorActors.size(); i++) {
            SensorActor sensor = this.sensorActors.get(i);
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

    /**
     * Speichert die Zelle in eine Datei.
     */
    public void saveToFile(String filePath) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(this);
        }
    }

    /**
     * Lädt eine Zelle aus einer Datei.
     */
    public static Cell loadFromFile(String filePath) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            return (Cell) ois.readObject();
        }
    }

    public double getMutationRateFactor() {
        return this.mutationRateFactor;
    }

    public void setMutationRateFactor(double mutationRateFactor) {
        this.mutationRateFactor = Math.max(0.1D, Math.min(2.0D, mutationRateFactor)); // Begrenzung auf sinnvolle Werte
    }

    public double getMutationStrengthFactor() {
        return this.mutationStrengthFactor;
    }

    public void setMutationStrengthFactor(double mutationStrengthFactor) {
        this.mutationStrengthFactor = Math.max(0.1D, Math.min(2.0D, mutationStrengthFactor)); // Begrenzung auf sinnvolle Werte
    }

    public void mutateMutationFactors(double mutationRate, double mutationStrength) {
        Random random = new Random();

        // Mutate mutationRateFactor
        if (random.nextDouble() < mutationRate) {
            double mutation = (random.nextDouble() * 2.0D - 1.0D) * mutationStrength;
            this.setMutationRateFactor(this.mutationRateFactor + mutation);
        }

        // Mutate mutationStrengthFactor
        if (random.nextDouble() < mutationRate) {
            double mutation = (random.nextDouble() * 2.0D - 1.0D) * mutationStrength;
            this.setMutationStrengthFactor(this.mutationStrengthFactor + mutation);
        }
    }

    // Methode zur Initialisierung des Gehirns und der SensorActor nach der Deserialisierung
    @Serial
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        this.velocityForce = new Vector2D(0, 0);
        this.angularVelocityForce = 0.0D;
        // Setze parentCell in allen SensorActor-Instanzen
        for (SensorActor sensorActor : this.sensorActors) {
            sensorActor.setParentCell(this);
        }
    }
}
