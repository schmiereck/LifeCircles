package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.partitioningStrategy.PartitioningStrategy;
import java.util.List;
import java.util.ArrayList;

/** Sensor fields use negative strength for attraction and positive strength for repulsion. */
public class SensorActorForceCellCalcService {
    /** A neighbour cell in reach: its first actor slot (-1 if not listed), centre offset from the own centre. */
    private record Candidate(Cell cell, int first, double range, double scale, double typeAffinity, double dx, double dy) {}

    /**
     * Key-lock adhesion: attraction acts in full only between similar actor colours on cells of
     * similar body type; toward a foreign partner the rest of it acts as bounded repulsion.
     */
    public record Affinity(double sharpness, double rejection) {
        public static Affinity fromConfig() {
            SimulationConfig config = SimulationConfig.getInstance();
            return new Affinity(config.getAdhesionSharpness(), config.getAdhesionRejection());
        }
        double of(CellType a, CellType b) { return Math.pow(Math.max(0, a.similarity(b)), sharpness); }
        /** Complete affinity of two actors: actor colours times body types. */
        public double between(SensorActor a, SensorActor b) {
            return of(a.getType(), b.getType()) * of(a.getParentCell().getType(), b.getParentCell().getType());
        }
        /** Symmetric in both partners, so action and reaction stay equal. */
        double effective(double strength, double affinity) {
            return strength < 0 ? strength * affinity - strength * (1 - affinity) * rejection : strength;
        }
    }
    public static void processInteractions(final List<Cell> cells, final PartitioningStrategy partitioner) {
        processInteractions(cells, partitioner, Environment.getInstance());
    }

    /**
     * Stage A of BODENHALT.md: a blocker surface acts like an actor everywhere on it. Only the actor's own
     * attraction, weighted with its colour affinity to the blocker material, and the blockers' stickiness
     * pull toward the nearest surface point; there is no rejection and no push off the ground.
     */
    public record Ground(double stickiness, double sharpness) {
        /** Without recognition every actor colour holds fully. */
        public static Ground fromConfig(boolean recognition) {
            SimulationConfig config = SimulationConfig.getInstance();
            return new Ground(config.getGroundStickiness(), recognition ? config.getAdhesionSharpness() : 0);
        }
        public double affinity(CellType actor, Blocker blocker) {
            return Math.pow(Math.max(0, actor.similarity(blocker.getType())), sharpness);
        }
    }

    public static void processInteractions(final List<Cell> cells, final PartitioningStrategy partitioner,
                                           final Environment environment) {
        processInteractions(cells, partitioner, environment, null);
    }

    public static void processInteractions(final List<Cell> cells, final PartitioningStrategy partitioner,
                                           final Environment environment, final Affinity affinity) {
        processInteractions(cells, partitioner, environment, affinity, null);
    }

    /** affinity null keeps the original colour-blind fields, ground null keeps blockers without fields. */
    public static void processInteractions(final List<Cell> cells, final PartitioningStrategy partitioner,
                                           final Environment environment, final Affinity affinity, final Ground ground) {
        final double width = environment.getWidth();
        final double height = environment.getHeight();
        final double softening = SimulationConfig.getInstance().getActorFieldSofteningFraction();
        final double bondFactor = SimulationConfig.getInstance().getBondFieldFactor();
        // Spring fields: commands at or beyond this (negative) strength pull with spring gain springGain / range.
        final double springLimit = SimulationConfig.getInstance().isSpringAdhesion()
                ? -SimulationConfig.getInstance().getSpringAdhesionThreshold() * SimulationConfig.getInstance().getCellActorMaxAttractiveForceStrength()
                : Double.NEGATIVE_INFINITY;
        final double springGain = SimulationConfig.getInstance().getSpringTearFactor();
        // Equal masses, but the spring stiffness grows with the cell radius: cap gain·radius for the explicit 60 Hz step.
        final double springStiffnessLimit = SimulationConfig.getInstance().getSpringStiffnessLimit();
        final boolean springPreload = SimulationConfig.getInstance().isSpringPreload();
        final double springDamping = SimulationConfig.getInstance().getSpringDamping();
        final double springBending = SimulationConfig.getInstance().getSpringBending();
        final double bendingDamping = SimulationConfig.getInstance().getSpringBendingDamping();
        final double maxRadiusSquared = Math.pow(SimulationConfig.getInstance().getCellMaxRadiusSize(), 2);
        // Field range per listed cell, once (calcSensorRadius takes a sine).
        final double[] range = new double[cells.size()];
        double maxRange = 0;
        for (int i = 0; i < range.length; i++) {
            Cell cell = cells.get(i);
            maxRange = Math.max(maxRange, range[i] = calcSensorRadius(cell.getRadiusSize(), cell.getSensorActors().size()));
        }
        // Blockers by x: candidates for touching (exact test below) and, with ground hold, for surface fields.
        final TerrainIndex terrain = new TerrainIndex(environment.getBlockerList(), width, maxRange);
        // Actor positions relative to their own centre, in flat arrays: a pair offset is then the centres' periodic
        // offset (once per cell pair) plus the difference of two small vectors, without a periodic wrap per pair.
        final int[] first = new int[cells.size() + 1];
        for (int i = 0; i < cells.size(); i++) {
            cells.get(i).setListIndex(i);
            first[i + 1] = first[i] + cells.get(i).getSensorActors().size();
        }
        final double[] relX = new double[first[cells.size()]], relY = new double[first[cells.size()]];
        java.util.stream.IntStream.range(0, cells.size()).parallel().forEach(i -> {
            Cell cell = cells.get(i);
            Vector2D centre = cell.getPosition();
            final double cos = Math.cos(cell.getRotation()), sin = Math.sin(cell.getRotation());
            int a = first[i];
            for (SensorActor actor : cell.getSensorActors()) {
                actor.updateCachedPosition(cos, sin);
                actor.setSensedCell(null);
                actor.setSensedActor(null);
                relX[a] = actor.getCachedPosition().getX() - centre.getX();
                relY[a++] = actor.getCachedPosition().getY() - centre.getY();
            }
        });

        // Workers write only their own cell. Reverse traversal computes the
        // equal and opposite reaction at the other cell's own actor.
        java.util.stream.IntStream.range(0, cells.size()).parallel().forEach(index -> {
            final Cell cell = cells.get(index);
            final double ownRange = range[index];
            final List<Candidate> candidates = new ArrayList<>();
            // Broad phase once per directed cell pair, preserving neighbor and force summation order.
            for (Cell other : partitioner.getNeighbors(cell)) {
                if (other == cell) continue;
                final int slot = other.getListIndex();
                final boolean listed = slot >= 0 && slot < cells.size() && cells.get(slot) == other;
                double otherRange = listed ? range[slot] : calcSensorRadius(other.getRadiusSize(), other.getSensorActors().size());
                double cutoff = cell.getRadiusSize() + other.getRadiusSize() + Math.max(ownRange, otherRange);
                Vector2D offset = cell.getPosition().getShortestDirectionWithWrapAround(other.getPosition(), width, height);
                if (offset.lengthSquared() <= cutoff * cutoff) {
                    candidates.add(new Candidate(other, listed ? first[slot] : -1,
                            otherRange, cell.getRadiusSize() * other.getRadiusSize() / maxRadiusSquared,
                            affinity == null ? 1 : affinity.of(cell.getType(), other.getType()), offset.getX(), offset.getY()));
                }
            }
            int own = first[cell.getListIndex()];
            // Angular spring: strongest spring pull per candidate pair, collected over all actor pairs.
            final double[] bendWeight = springBending > 0 ? new double[candidates.size()] : null;
            for (SensorActor actor : cell.getSensorActors()) {
                final double ownX = relX[own], ownY = relY[own++];
                double nearest = Double.POSITIVE_INFINITY;
                Blocker touchingBlocker = null;
                // The lowest sensing id wins, so the candidate order does not matter.
                for (Blocker blocker : terrain.near(actor.getCachedPosition().getX())) {
                    if (blocker.containsPoint(actor.getCachedPosition(), width, height)
                            && (touchingBlocker == null || blocker.getSensingId() < touchingBlocker.getSensingId())) {
                        touchingBlocker = blocker;
                    }
                }
                if (touchingBlocker != null) {
                    actor.setSensedCell(touchingBlocker);
                    actor.setSensedActor(touchingBlocker);
                }
                // Blockers do not move: the reaction goes into the terrain. Scale as for a maximum-size partner.
                final double groundScale = cell.getRadiusSize() / Math.sqrt(maxRadiusSquared);
                if (actor.getAnchor() != null) {
                    // An anchor replaces the surface fields with its frozen attraction, like a bond (sent and received).
                    Vector2D towardAnchor = actor.getCachedPosition().getShortestDirectionWithWrapAround(actor.getAnchor(), width, height);
                    double d = towardAnchor.length();
                    if (d > 1e-12 && d < ownRange) {
                        double strength = 2 * actor.getBondStrength() * bondFactor * (1 - d / ownRange) / Math.max(d, ownRange * softening);
                        cell.applyForce(towardAnchor.multiply(-strength * groundScale), actor.getCachedPosition());
                    }
                } else if (ground != null) {
                    double emitted = Math.min(0, actor.getForceStrength());
                    for (Blocker blocker : terrain.near(actor.getCachedPosition().getX())) {
                        double strength = emitted * ground.affinity(actor.getType(), blocker) + ground.stickiness();
                        if (strength == 0) continue;
                        Vector2D towardSurface = blocker.offsetToSurface(actor.getCachedPosition(), width, height);
                        double d = towardSurface.length();
                        if (d <= 1e-12 || d >= ownRange) continue;
                        strength *= (1 - d / ownRange) / Math.max(d, ownRange * softening);
                        cell.applyForce(towardSurface.multiply(-strength * groundScale), actor.getCachedPosition());
                    }
                }
                for (int ci = 0; ci < candidates.size(); ci++) {
                    final Candidate candidate = candidates.get(ci);
                    final Cell other = candidate.cell();
                    final double otherRange = candidate.range();
                    final double scale = candidate.scale();
                    // Actors sit on the rim: farther than rim plus the larger range from the other centre, none of
                    // its actors is sensed or exerts a field (triangle inequality; margin against rounding).
                    final double pairRange = Math.max(ownRange, otherRange);
                    final double reach = other.getRadiusSize() + pairRange + 1e-6;
                    final double toCentreX = candidate.dx() - ownX, toCentreY = candidate.dy() - ownY;
                    if (toCentreX * toCentreX + toCentreY * toCentreY > reach * reach) continue;
                    // Same cap in both directions of the pair keeps actio = reactio.
                    final double pairSpringGain = Math.min(springGain,
                            springStiffnessLimit / Math.max(cell.getRadiusSize(), other.getRadiusSize()));
                    final List<SensorActor> otherActors = other.getSensorActors();
                    for (int m = 0; m < otherActors.size(); m++) {
                        final SensorActor otherActor = otherActors.get(m);
                        final double otherX, otherY;
                        if (candidate.first() >= 0) { otherX = relX[candidate.first() + m]; otherY = relY[candidate.first() + m]; }
                        else { otherX = otherActor.getCachedPosition().getX() - other.getPosition().getX(); otherY = otherActor.getCachedPosition().getY() - other.getPosition().getY(); }
                        // Points TO the other actor; the reverse pair gets exactly the negated vector.
                        final double dx = candidate.dx() + (otherX - ownX), dy = candidate.dy() + (otherY - ownY);
                        final double squared = dx * dx + dy * dy;
                        // Beyond both ranges: neither sensed nor any field.
                        if (squared > pairRange * pairRange) continue;
                        final double distance = Math.sqrt(squared);
                        if (touchingBlocker == null && distance <= otherRange
                                && isBetterTarget(distance, nearest, other, otherActor, actor)) {
                            nearest = distance;
                            actor.setSensedCell(other);
                            actor.setSensedActor(otherActor);
                        }
                        if (distance <= 1e-12) continue;
                        // Out of both ranges both terms are zero; skip the affinity there.
                        if (distance >= otherRange && distance >= ownRange) continue;
                        // Received field plus the reaction to our emitted field.
                        double strength = 0;
                        double received = otherActor.getForceStrength(), emitted = actor.getForceStrength();
                        // Decided per emitter from its command, so both directions of the pair agree.
                        boolean receivedSpring = received <= springLimit, emittedSpring = emitted <= springLimit;
                        if (actor.getBondPartner() == otherActor) {
                            // A bond replaces this pair's fields with its frozen, already recognised attraction.
                            received = actor.getBondStrength() * bondFactor; emitted = received;
                            receivedSpring = emittedSpring = false;
                        } else if (affinity != null && (received < 0 || emitted < 0)) {
                            double a = candidate.typeAffinity() * affinity.of(actor.getType(), otherActor.getType());
                            received = affinity.effective(received, a);
                            emitted = affinity.effective(emitted, a);
                        }
                        // A spring pulls with |F| = gain·|s|·d/range and tears at the range; rejection stays a plain field.
                        // With preload it never pulls weaker than the classic field (no soft spot near contact).
                        final boolean receivedPull = receivedSpring && received < 0 && distance < otherRange;
                        final boolean emittedPull = emittedSpring && emitted < 0 && distance < ownRange;
                        if (distance < otherRange) {
                            final double classic = (1 - distance / otherRange) / Math.max(distance, otherRange * softening);
                            strength += received * (!receivedPull ? classic
                                    : springPreload ? Math.max(pairSpringGain / otherRange, classic) : pairSpringGain / otherRange);
                        }
                        if (distance < ownRange) {
                            final double classic = (1 - distance / ownRange) / Math.max(distance, ownRange * softening);
                            strength += emitted * (!emittedPull ? classic
                                    : springPreload ? Math.max(pairSpringGain / ownRange, classic) : pairSpringGain / ownRange);
                        }
                        if (bendWeight != null) {
                            // Symmetric: the reverse pair sees received and emitted swapped.
                            if (receivedPull) bendWeight[ci] = Math.max(bendWeight[ci], -received);
                            if (emittedPull) bendWeight[ci] = Math.max(bendWeight[ci], -emitted);
                        }
                        if (springDamping > 0 && (receivedPull || emittedPull)) {
                            // Dashpot along the pair: relative speed of the two actor points projected on the axis.
                            // The reverse pair sees the negated speed and axis, so actio = reactio holds.
                            final double ownOmega = cell.getAngularVelocity(), otherOmega = other.getAngularVelocity();
                            final double relVX = other.getVelocity().getX() - otherOmega * otherY - (cell.getVelocity().getX() - ownOmega * ownY);
                            final double relVY = other.getVelocity().getY() + otherOmega * otherX - (cell.getVelocity().getY() + ownOmega * ownX);
                            // strength multiplies -dx: closing speed along dx/d per unit distance.
                            strength -= springDamping * (relVX * dx + relVY * dy) / squared;
                        }
                        if (strength != 0) {
                            // towardOtherActor points TO the other actor. The minus sign
                            // makes negative strength attract and positive strength repel.
                            // A finite linear core removes the direction jump at contact.
                            // Each emitter uses its own range, preserving pair symmetry.
                            cell.applyForce(new Vector2D(dx * (-strength * scale), dy * (-strength * scale)), actor.getCachedPosition());
                        }
                    }
                }
            }
            if (bendWeight != null) {
                // E = c·Σ(1 − cos(n·Δ)) with Δ = bond angle − cell rotation for both cells: rest where the neighbour
                // sits in front of an actor. Torque on the own cell plus the transverse pair force on the centres
                // (reverse traversal applies the opposite) keep momentum and angular momentum.
                final int n = cell.getSensorActors().size();
                for (int ci = 0; ci < candidates.size(); ci++) {
                    if (bendWeight[ci] <= 0) continue;
                    final Candidate candidate = candidates.get(ci);
                    final Cell other = candidate.cell();
                    if (other.getSensorActors().size() != n) continue;
                    final double d = Math.sqrt(candidate.dx() * candidate.dx() + candidate.dy() * candidate.dy());
                    if (d <= 1e-9) continue;
                    final double c = springBending * bendWeight[ci] * candidate.scale() / (n * n);
                    final double phi = Math.atan2(candidate.dy(), candidate.dx());
                    // Generalised torque per end: spring plus dashpot against rotation relative to the bond.
                    double ownG = c * n * Math.sin(n * (phi - cell.getRotation()));
                    double otherG = c * n * Math.sin(n * (phi + Math.PI - other.getRotation()));
                    if (bendingDamping > 0) {
                        final double eta = bendingDamping * c * n * n;
                        final double bondOmega = (candidate.dx() * (other.getVelocity().getY() - cell.getVelocity().getY())
                                - candidate.dy() * (other.getVelocity().getX() - cell.getVelocity().getX())) / (d * d);
                        ownG -= eta * (cell.getAngularVelocity() - bondOmega);
                        otherG -= eta * (other.getAngularVelocity() - bondOmega);
                    }
                    cell.applyTorque(ownG);
                    final double s = (ownG + otherG) / d;
                    // Tangent (−u_y, u_x); the own cell is pulled along +t when the bond lags behind the actors.
                    cell.applyForce(new Vector2D(-candidate.dy() / d * s, candidate.dx() / d * s));
                }
            }
        });
    }

    public static double calcSensorRadius(final double radiusSize, final int totalSensors) {
        return 2.0D * radiusSize * Math.sin(Math.PI / totalSensors) * SimulationConfig.cellActorMaxFieldRadiusFactor;
    }

    /** Total ordering: distance, then stable cell identity, then fixed actor angle. */
    private static boolean isBetterTarget(double distance, double nearest, Cell candidateCell,
                                          SensorActor candidateActor, SensorActor sensor) {
        int byDistance = Double.compare(distance, nearest);
        if (byDistance != 0) return byDistance < 0;
        if (!(sensor.getSensedCell() instanceof Cell currentCell)) return true;
        int byCell = Long.compare(candidateCell.getSensingId(), currentCell.getSensingId());
        if (byCell != 0) return byCell < 0;
        return Double.compare(candidateActor.getAngleOnCell(),
                ((SensorActor) sensor.getSensedActor()).getAngleOnCell()) < 0;
    }
}
