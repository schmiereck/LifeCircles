package de.lifecircles.model;

import de.lifecircles.model.neural.*;
import de.lifecircles.service.SimulationConfig;
import java.util.ArrayList;
import java.util.List;
import java.io.*;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a cell in the simulation.
 * Contains position, rotation, size, type, and sensor/actor points.
 * Behavior is controlled by a neural network brain.
 */
public class Cell implements SensableCell, Serializable {
    private static final long serialVersionUID = 1L;
    private static final AtomicLong NEXT_SENSING_ID = new AtomicLong();
    private transient long sensingId = NEXT_SENSING_ID.getAndIncrement();

    /** Stable runtime identity used only to resolve exact sensing ties. */
    public long getSensingId() { return sensingId; }
    /** Membership stamp of one pass over a cell list (CellAdhesionService.updateBonds); not saved. */
    private transient long listMark;
    public long getListMark() { return listMark; }
    public void setListMark(long mark) { listMark = mark; }
    /** Position in the cell list of the current interaction pass (SensorActorForceCellCalcService); not saved. */
    private transient int listIndex = -1;
    public int getListIndex() { return listIndex; }
    public void setListIndex(int index) { listIndex = index; }

    private Vector2D position;
    private Vector2D velocity;
    private transient Vector2D velocityForce;
    private double rotation; // in radians
    private double angularVelocity;
    private transient double angularVelocityForce;
    private double radiusSize;
    private double targetRadiusSize; // Aktueller Groessenwunsch des Gehirns, auch waehrend des Wachstums
    private double growthAge; // Alter seit der Zellteilung für das Wachstum
    private boolean isGrowing; // Flag ob die Zelle sich im Wachstumsprozess befindet
    private CellType type;
    private final List<SensorActor> sensorActors;
    private CellBrainInterface brain;
    // Keep this exact field name/type and serialVersionUID for existing .cells files.
    private double energy;
    // Missing in old saves: Java serialization initializes it to zero, without a subsidy.
    private double secondaryEnergy;
    // Lazily allocated for codes 010..111. Missing fields in old files mean empty stores.
    private double[] additionalEnergies;
    private boolean remains;
    // Null in old saves means no conversion. Immutable genotype, never a shared mutable controller.
    private EnergyConversion energyConversion;
    private EnergyConversionProgram energyConversionProgram=EnergyConversionProgram.NONE;
    // Stored as throttle: the default 0 of fields absent in older saves means full activity.
    private double enzymeThrottle;
    private int energyUsageVersion=1; // Absent in previous saves: migrate once at simulation import/first step.
    private transient boolean brainPaused;
    private transient boolean brainDecisionAuthorized;
    private double age; // in seconds
    private int generation; // generation counter
    private transient boolean sunRayHit = false; // Flag to indicate if cell was hit by sun ray
    private int cellState; // Zustand der Zelle, beeinflusst zusätzliche Hidden-Layer
    private double mutationRateFactor; // Faktor für die Mutationsrate
    private double mutationStrengthFactor; // Faktor für die Mutationsstärke
    /** Inherited clock periods in seconds (TAKTGEBER.md); absent in older saves. */
    private double[] clockPeriods = DEFAULT_CLOCK_PERIODS.clone();
    /** Octaves for new and migrated cells; mutation moves each period by a factor. */
    public static final double[] DEFAULT_CLOCK_PERIODS = {.5, 1, 2, 4, 8, 16};
    public static final double MIN_CLOCK_PERIOD = .1, MAX_CLOCK_PERIOD = 120;
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
        if (cellBrain instanceof CellBrain neuralBrain) {
            neuralBrain.ensureEnergyInputs(sensorActors.size());
            neuralBrain.ensureEnzymeOutput(sensorActors.size());
        }
        // Raw body construction: factories assign founder budgets; births overwrite all stores from the parent.
        this.energy = SimulationConfig.CELL_MAX_ENERGY;
        this.age = 0.0;
        this.generation = 0; // initialize generation counter
        this.mutationRateFactor = 1.0; // Standardwert
        this.mutationStrengthFactor = 1.0; // Standardwert
        this.setCellState(0);
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
     * Notifies the cell that it has been hit by a SunRay in the current step.
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

    /** Sets the desired adult radius. Repeated brain commands do not restart a timer. */
    public void setRadiusSize(double radiusSize) {
        this.targetRadiusSize = Math.max(SimulationConfig.getInstance().getCellMinRadiusSize(),
                Math.min(SimulationConfig.getInstance().getCellMaxRadiusSize(), radiusSize));
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

    /** Advances adult size by one configured simulation step (no wall-clock time). */
    public void applyDelayedSizeChange() {
        applyDelayedSizeChange(SimulationConfig.getInstance().getCalcTimeStep());
    }

    /**
     * Exponential approach from the current radius to the latest desired radius.
     * sizeChangeDelay is the time constant in milliseconds: after that amount of
     * simulation time, about 63% of a constant requested change is complete.
     * The exact exponential makes subdividing a step equivalent for a constant
     * target. Newborn growth is handled separately by CellCalcService.
     */
    public void applyDelayedSizeChange(double deltaTime) {
        if (deltaTime == 0) return;
        double duration = SimulationConfig.getInstance().getSizeChangeDelay() / 1000.0;
        double progress = duration <= 0 ? 1 : -Math.expm1(-deltaTime / duration);
        this.radiusSize += (this.targetRadiusSize - this.radiusSize) * progress;
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

    /** Velocity change per unit force and 60 Hz step; equal for all cells (one linear mass). */
    public static final double FORCE_TO_VELOCITY = 0.08D;

    /**
     * Applies a force to the cell at a specific point.
     * This will affect linear.
     *
     * @param force            Force vector
     */
    public void applyForce(Vector2D force) {
        // Linear acceleration
        //this.velocity = this.velocity.add(force.multiply(1.0D / this.radiusSize)); // Larger cells are affected less
        this.velocityForce = this.velocityForce.add(force.multiply(FORCE_TO_VELOCITY)); // Larger cells are affected less
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
        this.velocityForce = this.velocityForce.add(force.multiply(FORCE_TO_VELOCITY)); // Larger cells are affected less
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
        updateForce(SimulationConfig.getInstance().getCalcTimeStep());
    }

    public void updateForce(double deltaTime) {
        // Force strengths were calibrated for 60 Hz; retain that scale while
        // integrating proportionally to simulation time at other step sizes.
        double stepScale = deltaTime * SimulationConfig.initialCalcFps;
        this.velocity = this.velocity.add(this.velocityForce.multiply(stepScale));
        this.angularVelocity += this.angularVelocityForce * stepScale;

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
        setEnergy(EnergyType.PRIMARY, energy);
    }

    @Override
    public double getEnergy() {
        return this.energy;
    }

    @Override
    public double getMaxEnergy() {
        return SimulationConfig.CELL_MAX_ENERGY;
    }

    @Override
    public double getEnergy(EnergyType energyType) {
        return switch (energyType) {
            case PRIMARY -> energy;
            case SECONDARY -> secondaryEnergy;
            default -> additionalEnergies == null ? 0 : additionalEnergies[energyType.getCode() - 2];
        };
    }

    @Override
    public double getMaxEnergy(EnergyType energyType) {
        java.util.Objects.requireNonNull(energyType);
        return SimulationConfig.CELL_MAX_ENERGY;
    }

    @Override
    public void setEnergy(EnergyType energyType, double value) {
        if (Double.isNaN(value)) throw new IllegalArgumentException("Energy must not be NaN");
        double bounded = Math.max(0, Math.min(getMaxEnergy(energyType), value));
        switch (energyType) {
            case PRIMARY -> energy = bounded;
            case SECONDARY -> secondaryEnergy = bounded;
            default -> {
                if (additionalEnergies == null && bounded == 0) return;
                if (additionalEnergies == null) additionalEnergies = new double[EnergyType.ALL.size() - 2];
                additionalEnergies[energyType.getCode() - 2] = bounded;
            }
        }
    }

    /** Accounting only: legacy sensing, metabolism and survival still use primary energy. */
    public double getTotalEnergy() {
        double total = energy + secondaryEnergy;
        if (additionalEnergies != null) for (double value : additionalEnergies) total += value;
        return total;
    }

    /** Neural throttle of the expressed enzyme in [0,1]; births and old saves start at full activity. */
    public double getEnzymeActivity() { return 1 - enzymeThrottle; }
    public void setEnzymeActivity(double activity) {
        if (!Double.isFinite(activity) || activity < 0 || activity > 1) throw new IllegalArgumentException("Enzyme activity must be in [0,1]");
        enzymeThrottle = 1 - activity;
    }

    public EnergyConversion getEnergyConversion() {
        return energyConversionProgram.route(getSpecializationIndex());
    }
    /** Uniform-rule convenience for old callers/test genomes. Use the program for differentiated cells. */
    public void setEnergyConversion(EnergyConversion conversion) {
        this.energyConversion=java.util.Objects.requireNonNull(conversion);
        this.energyConversionProgram=EnergyConversionProgram.uniform(conversion);
    }
    public int getSpecializationIndex() {
        // Same three bits as the existing state sensors and neural state experts.
        return cellState & (EnergyConversionProgram.SPECIALIZATION_COUNT-1);
    }
    public EnergyConversionProgram getEnergyConversionProgram() { return energyConversionProgram; }
    public boolean isBrainPaused() { return brainPaused; }
    public void authorizeBrainDecision(boolean permitted) { brainDecisionAuthorized=permitted; }
    public boolean consumeBrainDecisionAuthorization() {
        boolean permitted=brainDecisionAuthorized;brainDecisionAuthorized=false;return permitted;
    }
    public void setBrainPaused(boolean paused) { setBrainPaused(paused,false); }
    /** holdForces keeps the actor fields (tonus), so a funding pause does not dissolve a body. */
    public void setBrainPaused(boolean paused,boolean holdForces) {
        brainPaused=paused;
        if(paused)brainDecisionAuthorized=false;
        if(paused)for(SensorActor actor:sensorActors) {
            if(!holdForces)actor.setForceStrength(0);
            actor.setEnergyDelivery(0);actor.setReproductionDesire(0);
            // Keep the previously chosen absorption ports, so external supply can wake the cell.
        }
    }
    public void migrateEnergyUsage() {
        if(energyUsageVersion>=1)return;
        if(!remains) {
            double shifted=Math.min(energy*de.lifecircles.service.EnergyAllocationService.BRAIN_SHARE,
                    getMaxEnergy(EnergyType.SECONDARY)-secondaryEnergy);
            energy-=shifted;secondaryEnergy+=shifted;
        }
        energyUsageVersion=1;
    }
    public void setEnergyConversionProgram(EnergyConversionProgram program) {
        energyConversionProgram=java.util.Objects.requireNonNull(program);
        energyConversion=null;
    }
    /** Occupied forms without checking six absent stores in primary-only populations. */
    public int getEnergyPresenceMask() {
        int mask = (energy > 0 ? 1 : 0) | (secondaryEnergy > 0 ? 2 : 0);
        if (additionalEnergies != null) for (int i = 0; i < additionalEnergies.length; i++)
            if (additionalEnergies[i] > 0) mask |= 1 << (i + 2);
        return mask;
    }

    @Override public boolean isRemains() { return remains; }

    /** Preserve exact reserves, body and momentum; discard the brain and active commands. */
    public void becomeRemains() { becomeRemains(false); }
    /** holdForces keeps the last actor fields; they then fade with the remains half-life. */
    public void becomeRemains(boolean holdForces) {
        if (remains) return;
        remains = true;
        brain = RemainsBrain.INSTANCE;
        targetRadiusSize = radiusSize;
        isGrowing = false;
        resetSunRayHit();
        for (SensorActor actor : sensorActors) {
            if (!holdForces) actor.setForceStrength(0);
            actor.setEnergyAbsorption(0);
            actor.setEnergyDelivery(0);
            actor.setReproductionDesire(0);
            actor.setReproductionState(0);
        }
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
        if (this.brain instanceof de.lifecircles.model.neural.CellBrain cellBrain) cellBrain.applyCellState(cellState);
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
     * Returns the sensor pointing furthest upward (negative world Y).
     * Uses current orientation, independently of cached or wrapped positions.
     * Numerically equal directions select the lowest index.
     * @return Index of the topmost sensor (0-11)
     */
    public int getTopSensorIndex() {
        double minY = Double.POSITIVE_INFINITY;
        int topSensorIndex = 0;
        double cosRotation = Math.cos(rotation);
        double sinRotation = Math.sin(rotation);

        for (int i = 0; i < this.sensorActors.size(); i++) {
            SensorActor sensor = this.sensorActors.get(i);
            double sensorY = sensor.getRotatedUnitY(cosRotation, sinRotation);
            // Roundoff at a geometric tie must not randomly favor the later sensor.
            if (sensorY < minY - 1e-14) {
                minY = sensorY;
                topSensorIndex = i;
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

    public double getClockPeriod(int clock) { return clockPeriods[clock]; }

    public double[] getClockPeriods() { return clockPeriods.clone(); }

    public void setClockPeriods(double[] periods) {
        if (periods.length != DEFAULT_CLOCK_PERIODS.length) throw new IllegalArgumentException("Expected " + DEFAULT_CLOCK_PERIODS.length + " clock periods");
        double[] clamped = new double[periods.length];
        for (int i = 0; i < periods.length; i++) {
            if (!Double.isFinite(periods[i])) throw new IllegalArgumentException("Clock period must be finite");
            clamped[i] = Math.max(MIN_CLOCK_PERIOD, Math.min(MAX_CLOCK_PERIOD, periods[i]));
        }
        clockPeriods = clamped;
    }

    /** Each clock mutates with the given rate by a factor exp(±strength), so short and long periods move alike. */
    public void mutateClockPeriods(double mutationRate, double mutationStrength, Random random) {
        double[] periods = clockPeriods.clone();
        for (int i = 0; i < periods.length; i++)
            if (random.nextDouble() < mutationRate) periods[i] *= Math.exp((random.nextDouble() * 2.0D - 1.0D) * mutationStrength);
        setClockPeriods(periods);
    }

    public void mutateMutationFactors(double mutationRate, double mutationStrength) {
        mutateMutationFactors(mutationRate, mutationStrength, new Random());
    }

    public void mutateMutationFactors(double mutationRate, double mutationStrength, Random random) {

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
        // Older cells had one state-independent enzyme. Preserve its behavior in all states.
        if(energyConversionProgram==null) energyConversionProgram=energyConversion==null?EnergyConversionProgram.NONE:
                EnergyConversionProgram.uniform(energyConversion);
        if (clockPeriods == null) clockPeriods = DEFAULT_CLOCK_PERIODS.clone();
        else if (clockPeriods.length != DEFAULT_CLOCK_PERIODS.length) throw new InvalidObjectException("Invalid number of clocks");
        if (additionalEnergies != null && additionalEnergies.length != EnergyType.ALL.size() - 2)
            throw new InvalidObjectException("Invalid number of energy stores");
        // A loaded object is a new instance, including repeated loads of one saved cell.
        this.sensingId = NEXT_SENSING_ID.getAndIncrement();
        this.velocityForce = new Vector2D(0, 0);
        this.angularVelocityForce = 0.0D;
        // Setze parentCell in allen SensorActor-Instanzen
        for (SensorActor sensorActor : this.sensorActors) {
            sensorActor.setParentCell(this);
        }
        if (remains) { remains = false; becomeRemains(true); }
        else if (brain instanceof CellBrain neuralBrain) {
            neuralBrain.ensureEnergyInputs(sensorActors.size());
            neuralBrain.ensureEnzymeOutput(sensorActors.size());
        }
    }
}
