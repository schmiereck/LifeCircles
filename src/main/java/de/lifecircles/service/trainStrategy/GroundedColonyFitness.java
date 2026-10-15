package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import java.util.*;

/** Geometric support proxy. It does not create physical bonds or certify load capacity. */
final class GroundedColonyFitness {
    static final int SAMPLES = 20;
    private final int sections;
    private final Map<Cell, double[]> history = new IdentityHashMap<>();
    private int sampleIndex;
    private SpatialGridPartitioningStrategy grid;
    private double gridWidth, gridHeight, gridReach;

    record Rating(double score, boolean base) {}

    GroundedColonyFitness(int sections) { this.sections = sections; }
    void reset() { history.clear(); sampleIndex = 0; grid = null; }

    void sample(Environment world) {
        Map<Cell, Rating> ratings = current(world);
        Set<Cell> alive = Collections.newSetFromMap(new IdentityHashMap<>());
        alive.addAll(world.getCellList());
        history.keySet().removeIf(cell -> !alive.contains(cell));
        int slot = sampleIndex++ % SAMPLES;
        for (Cell cell : alive) {
            double[] values = history.computeIfAbsent(cell, ignored -> new double[SAMPLES]);
            Rating rating = ratings.get(cell);
            values[slot] = rating == null ? 0 : rating.score();
        }
    }

    /** Lower quartile: a brief spike cannot replace sustained height. Missing samples are zero. */
    double persistentScore(Cell cell) {
        double[] values = history.get(cell);
        if (values == null) return 0;
        values = values.clone(); Arrays.sort(values);
        return values[SAMPLES / 4 - 1];
    }

    List<Cell> ranked(Environment world) {
        Map<Cell, Rating> ratings = current(world);
        List<Cell> candidates = ratings.isEmpty() ? new ArrayList<>(world.getCellList())
                : new ArrayList<>(ratings.keySet());
        candidates.removeIf(cell -> section(cell, world) < 0 || cell.getEnergy() <= 0);
        // If no ground-connected colony exists, bootstrap without rewarding airborne height.
        candidates.sort(Comparator.comparingDouble((Cell cell) -> ratings.isEmpty() ? 0 : persistentScore(cell)).reversed()
                .thenComparingInt(cell -> ratings.containsKey(cell) && ratings.get(cell).base() ? 0 : 1)
                .thenComparing(Comparator.comparingDouble(Cell::getAge).reversed())
                .thenComparingDouble(cell -> centerDistance(cell, world))
                .thenComparingLong(Cell::getSensingId));
        return candidates;
    }

    private double centerDistance(Cell cell, Environment world) {
        double width = world.getWidth() / sections;
        return Math.abs(cell.getPosition().getX() - (section(cell, world) + .5) * width) / (width / 2);
    }

    private int section(Cell cell, Environment world) {
        double x = cell.getPosition().getX(), y = cell.getPosition().getY();
        if (!Double.isFinite(x) || !Double.isFinite(y) || x < 0 || x >= world.getWidth()
                || y < 0 || y >= world.getHeight()) return -1;
        return (int)(x / (world.getWidth() / sections));
    }

    Map<Cell, Rating> current(Environment world) {
        List<Cell> cells = world.getCellList();
        Map<Cell, Integer> indices = new IdentityHashMap<>();
        int[] parent = new int[cells.size()];
        double maxRadius = 0;
        for (int i = 0; i < cells.size(); i++) {
            parent[i] = i; indices.put(cells.get(i), i);
            maxRadius = Math.max(maxRadius, cells.get(i).getRadiusSize());
        }
        Map<Cell, Rating> ratings = new IdentityHashMap<>();
        if (cells.isEmpty() || maxRadius <= 0 || !Double.isFinite(maxRadius)) return ratings;
        // Bound bucket count even when every cell is a tiny newborn or the world is very large.
        double reach = Math.max(Math.max(42,2.1 * maxRadius),Math.max(world.getWidth(),world.getHeight())/256);
        if (grid == null || gridWidth != world.getWidth() || gridHeight != world.getHeight() || gridReach != reach) {
            gridWidth = world.getWidth(); gridHeight = world.getHeight(); gridReach = reach;
            grid = new SpatialGridPartitioningStrategy(gridWidth,gridHeight,reach);
        }
        grid.build(cells);
        for (int i = 0; i < cells.size(); i++) {
            Cell cell = cells.get(i); int section = section(cell, world);
            if (section < 0 || cell.getEnergy() <= 0) continue;
            for (Cell other : grid.getNeighbors(cell)) {
                int j = indices.get(other);
                if (j <= i || section(other, world) != section || other.getEnergy() <= 0) continue;
                double tolerance = .1 * Math.min(cell.getRadiusSize(),other.getRadiusSize());
                double contactReach = cell.getRadiusSize()+other.getRadiusSize()+tolerance;
                // No wrapping across ceiling/floor or across training-box boundaries.
                if (cell.getPosition().distance(other.getPosition()) <= contactReach)
                    parent[find(parent,j)] = find(parent,i);
            }
        }
        Map<Integer, List<Cell>> groups = new HashMap<>();
        Map<Cell, Double> grounded = new IdentityHashMap<>();
        for (int i = 0; i < cells.size(); i++) {
            Cell cell = cells.get(i);
            if (section(cell,world) < 0 || cell.getEnergy() <= 0) continue;
            groups.computeIfAbsent(find(parent,i), ignored -> new ArrayList<>()).add(cell);
            for (Blocker ground : world.getBlockerList()) {
                if (ground.getBlockerType() != Blocker.BlockerType.GROUND) continue;
                double top = ground.getPosition().getY()-ground.getHeight()/2;
                double x = cell.getPosition().getX(), y = cell.getPosition().getY();
                if (x >= ground.getPosition().getX()-ground.getWidth()/2
                        && x <= ground.getPosition().getX()+ground.getWidth()/2
                        && y <= top && Math.abs(y+cell.getRadiusSize()-top) <= .1*cell.getRadiusSize())
                    grounded.merge(cell,top,Math::max);
            }
        }
        for (List<Cell> group : groups.values()) {
            double floor = group.stream().filter(grounded::containsKey).mapToDouble(grounded::get).max().orElse(Double.NaN);
            if (!Double.isFinite(floor) || floor <= 0) continue;
            group.sort(Comparator.comparingDouble(cell -> cell.getPosition().getY()-cell.getRadiusSize()));
            // Average the upper quarter, rather than rewarding a single exceptionally high tip.
            int topCount = Math.max(1,(group.size()+3)/4);
            double height = 0;
            for (int i=0;i<topCount;i++) height += floor-group.get(i).getPosition().getY()+group.get(i).getRadiusSize();
            double score = Math.max(0,Math.min(1,height/topCount/floor));
            for (Cell cell : group) ratings.put(cell,new Rating(score,grounded.containsKey(cell)));
        }
        return ratings;
    }

    private static int find(int[] parent,int node) {
        while (parent[node] != node) { parent[node] = parent[parent[node]]; node = parent[node]; }
        return node;
    }
}
