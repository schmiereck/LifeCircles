package de.lifecircles.service;

import de.lifecircles.model.Cell;
import de.lifecircles.model.Blocker;
import de.lifecircles.model.SunRay;
import de.lifecircles.model.Vector2D;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Iterator;

/**
 * Service handling generation of sun energy rays,
 * applying energy to cells, and returning rays for visualization.
 */
public class EnergySunCalcService {
    private double rayAccumulator = 0.0;
    private double timeInCycle = 0.0; // Tracks time within the day/night cycle
    private final Random random;
    private final boolean indexed;
    private final ProjectedSunIndex<Cell> cellIndex = new ProjectedSunIndex<>();
    private final ProjectedSunIndex<Blocker> blockerIndex = new ProjectedSunIndex<>();

    public EnergySunCalcService() {
        this(new Random());
    }

    EnergySunCalcService(Random random) {
        this(random, true);
    }

    /** Brute-force mode is retained for exact differential tests. */
    EnergySunCalcService(Random random, boolean indexed) {
        this.random = random;
        this.indexed = indexed;
    }

    private static double circleHitT(Cell cell, double rayStartX, double rayStartY, double dirX, double dirY) {
        Vector2D c = cell.getPosition();
        double r = cell.getRadiusSize();
        double dx = rayStartX - c.getX();
        double dy = rayStartY - c.getY();
        double a = dirX * dirX + dirY * dirY;
        double b = 2 * (dx * dirX + dy * dirY);
        double cVal = dx * dx + dy * dy - r * r;
        double discriminant = b * b - 4 * a * cVal;
        if (discriminant < 0) return Double.POSITIVE_INFINITY;
        double sqrtDisc = Math.sqrt(discriminant);
        double t1 = (-b - sqrtDisc) / (2 * a);
        double t2 = (-b + sqrtDisc) / (2 * a);
        return (t1 > 0) ? t1 : ((t2 > 0) ? t2 : Double.POSITIVE_INFINITY);
    }

    public List<SunRay> calculateSunEnergy(
            final List<Cell> cells,
            final List<Blocker> blockers,
            final double width,
            final double height,
            final SimulationConfig config,
            final double deltaTime) {
        // This call calculates one step's sunlight, not lifetime exposure.
        // Reset even when the accumulator produces no rays or all rays are blocked.
        for (Cell cell : cells) cell.resetSunRayHit();
        cellIndex.clear(); blockerIndex.clear();

        // Update time in the day/night cycle
        this.timeInCycle = (this.timeInCycle + deltaTime) % SimulationConfig.SUN_DAY_NIGHT_CYCLE_DURATION;

        // Calculate intensity based on sinusoidal function
        final double intensity = (Math.sin((this.timeInCycle / SimulationConfig.SUN_DAY_NIGHT_CYCLE_DURATION) * Math.PI) *
                (1.0D - SimulationConfig.SUN_NIGHT_INTENSITY)) + SimulationConfig.SUN_NIGHT_INTENSITY;

        // Accumulate ray count based on pixel spacing to maintain constant density
        // Adjust rays per second based on intensity
        final double raysPerSecond = intensity * (width / config.getSunRaySpacingPx()) * config.getSunRayRate();
        this.rayAccumulator += raysPerSecond * deltaTime;
        final int numRays = (int) this.rayAccumulator;
        this.rayAccumulator -= numRays;
        final List<SunRay> rays = new ArrayList<>(Math.max(0, numRays));
        if (numRays == 0) return rays;

        // Sonnenwinkel interpolieren: Tag = vorwärts, Nacht = rückwärts
        double t = this.timeInCycle / SimulationConfig.SUN_DAY_NIGHT_CYCLE_DURATION;
        double sunAngleDeg;
        if (t < 0.5) {
            // Tag: von Morgen zu Abend
            double tDay = t / 0.5;
            sunAngleDeg = SimulationConfig.SUN_ANGLE_MORNING_DEG +
                    (SimulationConfig.SUN_ANGLE_EVENING_DEG - SimulationConfig.SUN_ANGLE_MORNING_DEG) * tDay;
        } else {
            // Nacht: von Abend zurück zu Morgen
            double tNight = (t - 0.5) / 0.5;
            sunAngleDeg = SimulationConfig.SUN_ANGLE_EVENING_DEG +
                    (SimulationConfig.SUN_ANGLE_MORNING_DEG - SimulationConfig.SUN_ANGLE_EVENING_DEG) * tNight;
        }
        double sunAngleRad = Math.toRadians(sunAngleDeg);
        double dirX = Math.sin(sunAngleRad);
        double dirY = Math.cos(sunAngleRad);
        // Erweiterung: X-Startbereich so wählen, dass alle Bodenbereiche erreicht werden
        double minX = 0;
        double maxX = width;
        if (dirY > 0.0001) { // Nur sinnvoll, wenn Strahlen nach unten gehen
            // Für den linken Rand: x=0, berechne StartX, sodass Strahl bei x=0, y=height ankommt
            double startXForLeft = 0 - (height * dirX / dirY);
            // Für den rechten Rand: x=width, berechne StartX, sodass Strahl bei x=width, y=height ankommt
            double startXForRight = width - (height * dirX / dirY);
            minX = Math.min(startXForLeft, startXForRight);
            maxX = Math.max(startXForLeft, startXForRight);
        }
        // Optional: minX/maxX auf sinnvolle Werte begrenzen (z.B. max 2*width außerhalb)
        double margin = width * 2.0;
        minX = Math.max(minX, -margin);
        maxX = Math.min(maxX, width + margin);
        ProjectedSunIndex<Cell> cellStrips = null;
        ProjectedSunIndex<Blocker> blockerStrips = null;
        // For a few rays/cells, scanning avoids index construction overhead.
        if (indexed && dirY > 0 && numRays >= 16 && cells.size() + blockers.size() >= 256
                && cellIndex.begin(minX * dirY, maxX * dirY, height) && blockerIndex.begin(minX * dirY, maxX * dirY, height)) {
            cellStrips = cellIndex; blockerStrips = blockerIndex;
            int ordinal = 0;
            for (Cell cell : cells) cellStrips.add(cell,
                    ordinal++, cell.getPosition().getX() * dirY - cell.getPosition().getY() * dirX, cell.getRadiusSize(),
                    cell.getPosition().getY() - cell.getRadiusSize());
            ordinal = 0;
            for (Blocker blocker : blockers) blockerStrips.add(blocker,
                    ordinal++, blocker.getPosition().getX() * dirY - blocker.getPosition().getY() * dirX,
                    (Math.abs(dirY) * blocker.getWidth() + Math.abs(dirX) * blocker.getHeight()) / 2,
                    blocker.getPosition().getY() - blocker.getHeight() / 2);
        }
        for (int rayNo = 0; rayNo < numRays; rayNo++) {
            // Strahlen gleichmäßig (aber zufällig) im erweiterten Bereich verteilen
            final double rayStartX = minX + random.nextDouble() * (maxX - minX);
            final double rayStartY = 0;
            // Strahlrichtung: (dirX, dirY)
            // Berechne, wo der Strahl das untere oder seitliche Fensterende trifft
            double tMax = Double.POSITIVE_INFINITY;
            // Schnitt mit unterem Rand
            if (dirY > 0) {
                double tY = (height - rayStartY) / dirY;
                tMax = Math.min(tMax, tY);
            }
            // Schnitt mit linker/rechter Rand
            if (dirX != 0) {
                double tX1 = (0 - rayStartX) / dirX;
                double tX2 = (width - rayStartX) / dirX;
                double tX = dirX > 0 ? tX2 : tX1;
                if (tX > 0) tMax = Math.min(tMax, tX);
            }
            // Find shade first, so its depth also bounds the subsequent cell traversal.
            double nearestBlockerHitT = Double.POSITIVE_INFINITY;
            var blockerCandidates = blockerStrips == null ? null : blockerStrips.candidates(rayStartX * dirY);
            Iterator<Blocker> blockerIterator = blockerCandidates == null ? blockers.iterator() : blockerCandidates;
            while (blockerIterator.hasNext()) {
                if (blockerCandidates != null) {
                    double limitY = dirY * Math.min(nearestBlockerHitT, tMax);
                    if (blockerCandidates.lowerY() > limitY + Math.max(1e-9, 8 * Math.ulp(limitY))) break;
                }
                Blocker blocker = blockerIterator.next();
                // Ray-AABB-Intersection
                double bx = blocker.getPosition().getX() - blocker.getWidth() / 2;
                double by = blocker.getPosition().getY() - blocker.getHeight() / 2;
                double bw = blocker.getWidth();
                double bh = blocker.getHeight();
                double tmin = 0, tmax = tMax;
                if (dirX != 0) {
                    double tx1 = (bx - rayStartX) / dirX;
                    double tx2 = (bx + bw - rayStartX) / dirX;
                    double txmin = Math.min(tx1, tx2);
                    double txmax = Math.max(tx1, tx2);
                    tmin = Math.max(tmin, txmin);
                    tmax = Math.min(tmax, txmax);
                } else if (rayStartX < bx || rayStartX > bx + bw) {
                    continue;
                }
                if (dirY != 0) {
                    double ty1 = (by - rayStartY) / dirY;
                    double ty2 = (by + bh - rayStartY) / dirY;
                    double tymin = Math.min(ty1, ty2);
                    double tymax = Math.max(ty1, ty2);
                    tmin = Math.max(tmin, tymin);
                    tmax = Math.min(tmax, tymax);
                } else if (rayStartY < by || rayStartY > by + bh) {
                    continue;
                }
                if (tmax >= tmin && tmin > 0 && tmin < nearestBlockerHitT && tmin < tMax) {
                    nearestBlockerHitT = tmin;
                }
            }
            // Schnitt mit Zellen und Blockern wie gehabt, aber entlang der Strahlrichtung
            double nearestCellHitT = Double.POSITIVE_INFINITY;
            Cell hitCell = null;
            if (cellStrips == null) {
                for (Cell cell : cells) {
                    double tCell = circleHitT(cell, rayStartX, rayStartY, dirX, dirY);
                    if (tCell > 0 && tCell < nearestCellHitT && tCell < tMax) {
                        nearestCellHitT = tCell; hitCell = cell;
                    }
                }
            } else {
                int nearestOrdinal = Integer.MAX_VALUE;
                var depthCandidates = cellStrips.candidates(rayStartX * dirY);
                while (depthCandidates.hasNext()) {
                    double limitY = dirY * Math.min(nearestCellHitT, Math.min(nearestBlockerHitT, tMax));
                    if (depthCandidates.lowerY() > limitY + Math.max(1e-9, 8 * Math.ulp(limitY))) break;
                    int ordinal = depthCandidates.ordinal();
                    Cell cell = depthCandidates.next();
                    double tCell = circleHitT(cell, rayStartX, rayStartY, dirX, dirY);
                    if (tCell > 0 && tCell < tMax && (tCell < nearestCellHitT
                            || (tCell == nearestCellHitT && ordinal < nearestOrdinal))) {
                        nearestCellHitT = tCell;
                        nearestOrdinal = ordinal;
                        hitCell = cell;
                    }
                }
            }
            double rayHitT = tMax;
            if (hitCell != null && nearestCellHitT <= nearestBlockerHitT) {
                rayHitT = nearestCellHitT;
                hitCell.setEnergy(hitCell.getEnergy() + config.getEnergyPerRay());
                hitCell.notifySunRayHit();
            } else if (nearestBlockerHitT < Double.POSITIVE_INFINITY) {
                rayHitT = nearestBlockerHitT;
            }
            double finalRayEndX = rayStartX + dirX * rayHitT;
            double finalRayEndY = rayStartY + dirY * rayHitT;
            rays.add(new SunRay(
                new Vector2D(rayStartX, rayStartY),
                new Vector2D(finalRayEndX, finalRayEndY)
            ));
        }
        return rays;
    }
}
