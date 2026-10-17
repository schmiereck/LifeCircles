package de.lifecircles.service;

import java.util.*;
import java.util.concurrent.atomic.LongAdder;

/** Optional wall-clock phase timings; disabled by default, no per-cell clocks. */
public final class SimulationProfiler {
    public enum Phase { GRID, SUN, REPULSION, INTERACTIONS, TRANSFER, INPUTS, BRAINS,
        INTEGRATION, REPRODUCTION, MUTATION, SELECTION, STATISTICS, SNAPSHOT, CONVERSION, BRAIN_FUNDING }
    public record Timing(long calls, long nanos) {}
    private volatile boolean enabled = Boolean.getBoolean("life.profile");
    private final LongAdder[] nanos = new LongAdder[Phase.values().length];
    private final LongAdder[] calls = new LongAdder[Phase.values().length];
    public SimulationProfiler() {
        for (int i = 0; i < nanos.length; i++) { nanos[i] = new LongAdder(); calls[i] = new LongAdder(); }
    }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public long start() { return enabled ? System.nanoTime() : 0; }
    public void finish(Phase phase, long start) {
        if (start == 0) return;
        nanos[phase.ordinal()].add(System.nanoTime() - start); calls[phase.ordinal()].increment();
    }
    public Map<Phase, Timing> snapshot() {
        Map<Phase, Timing> result = new EnumMap<>(Phase.class);
        for (Phase phase : Phase.values()) result.put(phase, new Timing(calls[phase.ordinal()].sum(), nanos[phase.ordinal()].sum()));
        return Collections.unmodifiableMap(result);
    }
    /** Call between updates when measurements should restart. */
    public void reset() { for (int i = 0; i < nanos.length; i++) { nanos[i].reset(); calls[i].reset(); } }
}
