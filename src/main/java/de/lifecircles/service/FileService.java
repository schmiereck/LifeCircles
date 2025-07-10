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
        System.out.println("Starte Speichern von " + cells.size() + " Zellen...");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(cells);
            System.out.println("Speichern abgeschlossen: " + filePath);
        }
    }

    /**
     * Speichert alle Zellen in eine Datei mit Fortschrittsanzeige.
     * Diese Methode verwendet eine eigene Implementierung für die Fortschrittsanzeige.
     */
    public void saveCellsToFileWithProgress(String filePath, List<Cell> cells) throws IOException {
        System.out.println("Starte Speichern von " + cells.size() + " Zellen...");

        try (FileOutputStream fos = new FileOutputStream(filePath);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             CustomProgressOutputStream pos = new CustomProgressOutputStream(bos, cells.size());
             ObjectOutputStream oos = new ObjectOutputStream(pos)) {

            oos.writeObject(cells);
            System.out.println("Speichern abgeschlossen: " + filePath);
        }
    }

    /**
     * Lädt Zellen aus einer Datei.
     */
    @SuppressWarnings("unchecked")
    public List<Cell> loadCellsFromFile(String filePath) throws IOException, ClassNotFoundException {
        System.out.println("Starte Laden von Zellen aus " + filePath + "...");
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            List<Cell> cells = (List<Cell>) ois.readObject();
            System.out.println("Laden abgeschlossen: " + cells.size() + " Zellen geladen");
            return cells;
        }
    }

    /**
     * Lädt Zellen aus einer Datei mit Fortschrittsanzeige.
     */
    @SuppressWarnings("unchecked")
    public List<Cell> loadCellsFromFileWithProgress(String filePath) throws IOException, ClassNotFoundException {
        System.out.println("Starte Laden von Zellen aus " + filePath + "...");

        // Um die Dateigröße für den Fortschritt zu ermitteln
        File file = new File(filePath);
        long fileSize = file.length();

        try (FileInputStream fis = new FileInputStream(filePath);
             BufferedInputStream bis = new BufferedInputStream(fis);
             CustomProgressInputStream pis = new CustomProgressInputStream(bis, fileSize);
             ObjectInputStream ois = new ObjectInputStream(pis)) {

            List<Cell> cells = (List<Cell>) ois.readObject();
            System.out.println("Laden abgeschlossen: " + cells.size() + " Zellen geladen");
            return cells;
        }
    }

    /**
     * Lädt Zellen aus einer Datei und fügt sie zum Environment hinzu.
     */
    public void mergeCellsFromFile(String filePath, Environment environment) throws IOException, ClassNotFoundException {
        System.out.println("Starte Laden und Zusammenführen von Zellen aus " + filePath + "...");
        List<Cell> loadedCells = loadCellsFromFileWithProgress(filePath);

        // Anstatt die Zellen zu einer Kopie hinzuzufügen, fügen wir sie direkt zum Environment hinzu
        for (Cell cell : loadedCells) {
            environment.addCell(cell);
        }

        System.out.println("Zusammenführen abgeschlossen: " + loadedCells.size() + " Zellen hinzugefügt");
    }

    /**
     * Speichert die 100 besten Zellen, verteilt über die Breite (x-Position) des Environments, jeweils mit der höchsten Energie.
     */
    public void saveBestCellsToFile(String filePath, List<Cell> cells, double environmentWidth) throws IOException {
        System.out.println("Starte Speichern der besten Zellen...");

        if (cells.isEmpty()) {
            System.out.println("Keine Zellen zum Speichern vorhanden.");
            return;
        }

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

            // Fortschritt anzeigen (bei jedem 10. Schritt)
            if (i % 10 == 0) {
                System.out.println("Fortschritt: " + (i * 100 / numBest) + "%");
            }

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
            System.out.println("Speichern der besten Zellen abgeschlossen: " + bestCells.size() + " Zellen gespeichert");
        }
    }

    /**
     * Lädt und setzt Zellen im Environment.
     */
    public void loadAndSetCells(String filePath, Environment environment) throws IOException, ClassNotFoundException {
        List<Cell> cells = loadCellsFromFileWithProgress(filePath);
        environment.resetCells(cells);
        System.out.println("Environment mit " + cells.size() + " geladenen Zellen aktualisiert");
    }

    /**
     * Ein OutputStream, der den Fortschritt während des Schreibens anzeigt.
     */
    private static class CustomProgressOutputStream extends FilterOutputStream {
        private long bytesWritten = 0;
        private final int totalItems;
        private int lastPercentReported = -1;

        public CustomProgressOutputStream(OutputStream out, int totalItems) {
            super(out);
            this.totalItems = totalItems;
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
            updateProgress(1);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
            updateProgress(len);
        }

        private void updateProgress(int bytesWrittenNow) {
            bytesWritten += bytesWrittenNow;
            // Da wir die genaue Größe nicht kennen, verwenden wir einen Schätzwert basierend auf den Items
            int percent = (int)((bytesWritten / (totalItems * 50.0)) * 100); // 50 bytes pro Item ist eine Schätzung
            percent = Math.min(99, percent); // Nie 100% anzeigen bis zum Ende

            if (percent > lastPercentReported && percent % 10 == 0) {
                System.out.println("Speichern Fortschritt: " + percent + "%");
                lastPercentReported = percent;
            }
        }
    }

    /**
     * Ein InputStream, der den Fortschritt während des Lesens anzeigt.
     */
    private static class CustomProgressInputStream extends FilterInputStream {
        private long bytesRead = 0;
        private final long totalBytes;
        private int lastPercentReported = -1;

        public CustomProgressInputStream(InputStream in, long totalBytes) {
            super(in);
            this.totalBytes = totalBytes;
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b != -1) {
                updateProgress(1);
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int bytesReadNow = super.read(b, off, len);
            if (bytesReadNow != -1) {
                updateProgress(bytesReadNow);
            }
            return bytesReadNow;
        }

        private void updateProgress(int bytesReadNow) {
            bytesRead += bytesReadNow;
            int percent = (int)((bytesRead * 100.0) / totalBytes);

            if (percent > lastPercentReported && percent % 10 == 0) {
                System.out.println("Laden Fortschritt: " + percent + "%");
                lastPercentReported = percent;
            }
        }
    }
}
