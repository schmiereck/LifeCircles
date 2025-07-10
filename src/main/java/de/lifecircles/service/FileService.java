package de.lifecircles.service;

import de.lifecircles.model.Cell;
import de.lifecircles.model.Environment;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Service für das Laden und Speichern von Zellen in Dateien.
 */
public class FileService {
    private static FileService instance;

    private FileService() {
        // Privater Konstruktor für Singleton-Pattern
    }

    /**
     * Gibt die Singleton-Instanz des FileService zurück
     */
    public static FileService getInstance() {
        if (instance == null) {
            instance = new FileService();
        }
        return instance;
    }

    /**
     * Speichert alle Zellen in eine Datei.
     */
    public void saveCellsToFile(String filePath, List<Cell> cells) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(cells);
        }
    }

    /**
     * Lädt Zellen aus einer Datei.
     */
    @SuppressWarnings("unchecked")
    public List<Cell> loadCellsFromFile(String filePath) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            return (List<Cell>) ois.readObject();
        }
    }

    /**
     * Speichert die 100 besten Zellen, verteilt über die Breite (x-Position) des Environments, jeweils mit der höchsten Energie.
     */
    public void saveBestCellsToFile(String filePath, List<Cell> cells, double environmentWidth) throws IOException {
        if (cells.isEmpty()) return;

        int numBest = 100;
        double minX = 0;
        double maxX = environmentWidth;
        double binSize = (maxX - minX) / numBest;
        List<Cell> bestCells = new ArrayList<>();

        for (int i = 0; i < numBest; i++) {
            double binStart = minX + i * binSize;
            double binEnd = binStart + binSize;
            Cell best = null;
            double bestEnergy = Double.NEGATIVE_INFINITY;

            for (Cell c : cells) {
                double x = c.getPosition().getX();
                if (x >= binStart && x < binEnd) {
                    if (c.getEnergy() > bestEnergy) {
                        best = c;
                        bestEnergy = c.getEnergy();
                    }
                }
            }

            if (best != null) {
                bestCells.add(best);
            }
        }

        // Falls weniger als 100 gefunden wurden, mit weiteren besten auffüllen
        if (bestCells.size() < numBest) {
            List<Cell> sorted = new ArrayList<>(cells);
            sorted.sort(Comparator.comparingDouble(Cell::getEnergy).reversed());
            for (Cell c : sorted) {
                if (!bestCells.contains(c)) {
                    bestCells.add(c);
                    if (bestCells.size() >= numBest) break;
                }
            }
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(bestCells);
        }
    }
}
