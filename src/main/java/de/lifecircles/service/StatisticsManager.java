package de.lifecircles.service;

import de.lifecircles.model.Cell;
import de.lifecircles.model.CellType;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Manages simulation statistics and population tracking.
 */
public class StatisticsManager {
    private static final StatisticsManager INSTANCE = new StatisticsManager();
    private static final int HISTORY_SIZE = 100;

    // Current population counts
    private final IntegerProperty totalPopulation = new SimpleIntegerProperty(0);

    // Population history for graphs
    private final List<Integer> totalHistory = new ArrayList<>();

    // Performance metrics
    private long lastUpdateTime = System.nanoTime();
    private double averageUpdateTime = 0.0;
    private int updateCount = 0;

    private StatisticsManager() {}

    public static StatisticsManager getInstance() {
        return INSTANCE;
    }

    /**
     * Updates statistics based on current cell population.
     */
    public void update(List<Cell> cells) {
        // Update performance metrics
        long currentTime = System.nanoTime();
        double updateTime = (currentTime - this.lastUpdateTime) / 1_000_000.0; // Convert to ms
        if ((currentTime - this.lastUpdateTime) > 1_000_000_000L) {
            this.lastUpdateTime = currentTime;

            this.updateCount++;
            this.averageUpdateTime = this.averageUpdateTime * 0.95 + updateTime * 0.05;

            // Update current counts
            this.totalPopulation.set(cells.size());

            // Update history
            this.updateHistory(this.totalHistory, cells.size());
        }
    }

    private void updateHistory(List<Integer> history, int value) {
        history.add(value);
        if (history.size() > HISTORY_SIZE) {
            history.remove(0);
        }
    }

    // Getters for current population
    public int getTotalPopulation() { return totalPopulation.get(); }
    public IntegerProperty totalPopulationProperty() { return totalPopulation; }

    // Getters for history
    public List<Integer> getTotalHistory() { return new ArrayList<>(totalHistory); }

    // Performance metrics
    public double getAverageUpdateTime() { return averageUpdateTime; }
    public int getUpdateCount() { return updateCount; }
}
