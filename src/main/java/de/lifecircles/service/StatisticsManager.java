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

    // All encountered types
    private final Set<CellType> allTypes = new HashSet<>();

    // Clusters of types (each list holds the 10% most similar types for that cluster center)
    private final List<List<CellType>> clusters = new ArrayList<>();

    // History per cluster
    private final List<List<Integer>> clusterHistories = new ArrayList<>();

    private StatisticsManager() {}

    public static StatisticsManager getInstance() {
        return INSTANCE;
    }

    /**
     * Updates statistics based on current cell population.
     */
    public void update(List<Cell> cells) {
        collectTypes(cells);
        clusterTypes();

        // Update performance metrics
        long currentTime = System.nanoTime();
        double updateTime = (currentTime - lastUpdateTime) / 1_000_000.0; // Convert to ms
        lastUpdateTime = currentTime;
        
        updateCount++;
        averageUpdateTime = averageUpdateTime * 0.95 + updateTime * 0.05;

        // Update current counts
        totalPopulation.set(cells.size());

        // Update history
        updateHistory(totalHistory, cells.size());

        // Update cluster histories
        for (int i = 0; i < clusters.size(); i++) {
            int clusterCount = 0;
            for (Cell cell : cells) {
                if (clusters.get(i).contains(cell.getType())) {
                    clusterCount++;
                }
            }
            updateHistory(clusterHistories.get(i), clusterCount);
        }
    }

    private void updateHistory(List<Integer> history, int value) {
        history.add(value);
        if (history.size() > HISTORY_SIZE) {
            history.remove(0);
        }
    }

    /**
     * Register types of current cells.
     */
    private void collectTypes(List<Cell> cells) {
        for (Cell cell : cells) {
            allTypes.add(cell.getType());
        }
    }

    /**
     * Perform clustering of allTypes into clusters of 10% most similar (disjoint).
     */
    private void clusterTypes() {
        clusters.clear();
        // Disjunkte Cluster in Gruppen von 10% der Typen
        List<CellType> remaining = new ArrayList<>(allTypes);
        if (remaining.isEmpty()) return;
        int clusterSize = Math.max(1, (int)(remaining.size() * 0.1));
        List<List<CellType>> newClusters = new ArrayList<>();
        while (!remaining.isEmpty()) {
            CellType center = remaining.get(0);
            List<CellType> cluster = remaining.stream()
                .sorted((a, b) -> Double.compare(center.similarity(b), center.similarity(a)))
                .limit(clusterSize)
                .collect(Collectors.toList());
            newClusters.add(cluster);
            remaining.removeAll(cluster);
        }
        // Übernehme neue Cluster
        clusters.addAll(newClusters);
        // Stimmen Anzahl der History-Listen auf Clusterzahl ab
        while (clusterHistories.size() < clusters.size()) {
            clusterHistories.add(new ArrayList<>());
        }
        while (clusterHistories.size() > clusters.size()) {
            clusterHistories.remove(clusterHistories.size() - 1);
        }
    }

    // Getters for current population
    public int getTotalPopulation() { return totalPopulation.get(); }
    public IntegerProperty totalPopulationProperty() { return totalPopulation; }

    // Getters for history
    public List<Integer> getTotalHistory() { return new ArrayList<>(totalHistory); }

    // Getters for cluster histories
    public List<List<Integer>> getClusterHistories() {
        List<List<Integer>> histories = new ArrayList<>();
        for (int i = 0; i < clusters.size(); i++) {
            if (!clusters.get(i).isEmpty()) {
                histories.add(new ArrayList<>(clusterHistories.get(i)));
            }
        }
        return histories;
    }

    // Performance metrics
    public double getAverageUpdateTime() { return averageUpdateTime; }
    public int getUpdateCount() { return updateCount; }
}
