package de.lifecircles.service;

import java.util.*;

/** Reusable perpendicular strips, grouped by earliest possible hit depth for downward rays. */
final class ProjectedSunIndex<T> {
    private int[] heads = new int[0], tails = new int[0], next = new int[0], ordinals = new int[0];
    private int[] firstRows = new int[0], lastRows = new int[0];
    private Object[] items = new Object[0];
    private int size, bins, rows;
    private double origin, binWidth, rowHeight, height;

    void clear() {
        // Release departed/dead objects even on subsequent steps without indexed sunlight.
        Arrays.fill(items, 0, size, null);
        size = 0;
    }

    boolean begin(double firstRay, double lastRay, double height) {
        clear();
        double minimum = Math.min(firstRay, lastRay), maximum = Math.max(firstRay, lastRay);
        double span = maximum - minimum + 48;
        if (!Double.isFinite(span) || !Double.isFinite(minimum) || !(span > 0)
                || !Double.isFinite(height) || !(height > 0)) return false;
        this.height = height;
        rowHeight = Math.max(128, height / 255);
        rows = Math.min(256, (int) Math.floor(height / rowHeight) + 1);
        origin = minimum - 24;
        // Bound the head arrays for very wide worlds; object intervals are clipped to emitted rays.
        int maxBins = Math.max(2, 65536 / rows);
        binWidth = Math.max(24, span / (maxBins - 1));
        bins = Math.min(maxBins, (int) Math.ceil(span / binWidth) + 1);
        int buckets = bins * rows;
        if (firstRows.length < bins) { firstRows = new int[bins]; lastRows = new int[bins]; }
        Arrays.fill(firstRows, 0, bins, rows); Arrays.fill(lastRows, 0, bins, -1);
        if (heads.length < buckets) { heads = new int[buckets]; tails = new int[buckets]; }
        Arrays.fill(heads, 0, buckets, -1);
        Arrays.fill(tails, 0, buckets, -1);
        return true;
    }

    void add(T item, int ordinal, double center, double extent, double earliestY) {
        double padding = Math.max(1e-9, 8 * Math.ulp(Math.abs(center) + extent));
        double paddedY = earliestY - Math.max(1e-9, 8 * Math.ulp(Math.abs(earliestY)));
        if (paddedY > height) return;
        // One depth bucket per object: its earliest possible hit, not its center.
        // Entire intersections are tested when encountered, even if the hit lies in a later row.
        int row = Math.max(0, Math.min(rows - 1, (int) Math.floor(paddedY / rowHeight)));
        int first = Math.max(0, (int) Math.floor((center - extent - padding - origin) / binWidth));
        int last = Math.min(bins - 1, (int) Math.floor((center + extent + padding - origin) / binWidth));
        for (int bin = first; bin <= last; bin++) {
            int bucket = row * bins + bin;
            firstRows[bin] = Math.min(firstRows[bin], row); lastRows[bin] = Math.max(lastRows[bin], row);
            if (size == items.length) {
                int capacity = Math.max(64, items.length + items.length / 2);
                items = Arrays.copyOf(items, capacity); next = Arrays.copyOf(next, capacity);
                ordinals = Arrays.copyOf(ordinals, capacity);
            }
            items[size] = item; next[size] = -1; ordinals[size] = ordinal;
            // Keep input order within a depth group; the tracer uses ordinals for cross-group ties.
            if (tails[bucket] == -1) heads[bucket] = size; else next[tails[bucket]] = size;
            tails[bucket] = size++;
        }
    }

    Candidates candidates(double coordinate) {
        int bin = (int) Math.floor((coordinate - origin) / binWidth);
        return new Candidates(bin);
    }

    final class Candidates implements Iterator<T> {
        private final int bin;
        private final int lastRow;
        private int node, row;
        Candidates(int bin) {
            this.bin = bin;
            this.row = bin < 0 || bin >= bins ? rows : firstRows[bin];
            this.lastRow = bin < 0 || bin >= bins ? -1 : lastRows[bin];
            this.node = row >= rows ? -1 : heads[row * bins + bin];
        }
        public boolean hasNext() {
            while (node == -1 && row < lastRow) node = heads[++row * bins + bin];
            return node != -1;
        }
        double lowerY() { return row * rowHeight; }
        int ordinal() { return ordinals[node]; }
        @SuppressWarnings("unchecked") public T next() {
            if (!hasNext()) throw new NoSuchElementException();
            T item = (T) items[node]; node = next[node]; return item;
        }
    }
}
