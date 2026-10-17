package de.lifecircles.service;

import de.lifecircles.model.Cell;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages simulation statistics and population tracking.
 */
public class StatisticsManagerService {
    private static final StatisticsManagerService INSTANCE = new StatisticsManagerService();
    /** History is sampled in simulation time, so day/night and seasons keep their shape at any speed. */
    public static final double SAMPLE_INTERVAL_SECONDS = 5;
    /** 300 samples of 5 s: 25 simulated minutes, i.e. 12.5 day/night cycles. */
    public static final int HISTORY_SIZE = 300;

    // Current population counts
    private final IntegerProperty totalPopulation = new SimpleIntegerProperty(0);
    private int remainsPopulation;

    // Population history for graphs
    private final List<Integer> totalHistory = new ArrayList<>();
    private final List<Double> sunPowerHistory = new ArrayList<>();
    private double sunPower;
    private double sampleTime, sunEnergyInSample;

    // Performance metrics
    private long lastUpdateTime = System.nanoTime();
    private double averageUpdateTime = 0.0;
    private int updateCount = 0;

    private StatisticsManagerService() {}

    public static StatisticsManagerService getInstance() {
        return INSTANCE;
    }

    /** A new world starts a new history, including while paused before the first step. */
    public void reset(List<Cell> cells) {
        synchronized (totalHistory) { totalHistory.clear(); sunPowerHistory.clear(); }
        sunPower = 0; sampleTime = 0; sunEnergyInSample = 0;
        remainsPopulation=(int)cells.stream().filter(Cell::isRemains).count();
        totalPopulation.set(cells.size()-remainsPopulation);
        averageUpdateTime=0; updateCount=0; lastUpdateTime=System.nanoTime();
    }

    /**
     * Updates statistics based on current cell population.
     */
    public void update(List<Cell> cells) {
        update(cells, 0, 0);
    }

    /** sunPower: offered sunlight energy per simulation second during this step of length deltaTime. */
    public void update(List<Cell> cells, double sunPower, double deltaTime) {
        this.sunPower = sunPower;
        sampleTime += deltaTime;
        sunEnergyInSample += sunPower * deltaTime;
        if (sampleTime >= SAMPLE_INTERVAL_SECONDS - 1e-9) { // summed step lengths may round just below
            int living = (int) cells.stream().filter(cell -> !cell.isRemains()).count();
            // The GUI thread reads both lists; keep each sample pair together.
            synchronized (totalHistory) {
                this.updateHistory(this.totalHistory, living);
                // Mean over the interval, so short rate changes and dusk are not aliased.
                this.updateHistory(this.sunPowerHistory, sunEnergyInSample / sampleTime);
            }
            sampleTime = 0; sunEnergyInSample = 0;
        }
        // Update performance metrics
        long currentTime = System.nanoTime();
        double updateTime = (currentTime - this.lastUpdateTime) / 1_000_000.0; // Convert to ms
        if ((currentTime - this.lastUpdateTime) > 1_000_000_000L) {
            this.lastUpdateTime = currentTime;

            this.updateCount++;
            this.averageUpdateTime = this.averageUpdateTime * 0.95 + updateTime * 0.05;

            // Update current counts
            remainsPopulation = (int) cells.stream().filter(Cell::isRemains).count();
            this.totalPopulation.set(cells.size() - remainsPopulation);

        }
    }

    private <T> void updateHistory(List<T> history, T value) {
        history.add(value);
        if (history.size() > HISTORY_SIZE) {
            history.remove(0);
        }
    }

    // Getters for current population
    public int getTotalPopulation() { return totalPopulation.get(); }
    public int getRemainsPopulation() { return remainsPopulation; }
    public IntegerProperty totalPopulationProperty() { return totalPopulation; }

    // Getters for history
    public List<Integer> getTotalHistory() { synchronized (totalHistory) { return new ArrayList<>(totalHistory); } }
    /** Sampled together with getTotalHistory, same indices. */
    public List<Double> getSunPowerHistory() { synchronized (totalHistory) { return new ArrayList<>(sunPowerHistory); } }
    public double getSunPower() { return sunPower; }

    // Performance metrics
    public double getAverageUpdateTime() { return averageUpdateTime; }
    public int getUpdateCount() { return updateCount; }
}
