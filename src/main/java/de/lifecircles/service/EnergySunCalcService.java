package de.lifecircles.service;

import de.lifecircles.model.Cell;
import de.lifecircles.model.EnergyType;
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
    /** Fraction of the current year in [0,1); 0 is mid-summer. Advancing by phase keeps live length changes smooth. */
    private double seasonPhase;
    private volatile double currentIntensity = 1;

    public EnergySunCalcService() {
        this(new Random());
    }

    public EnergySunCalcService(Random random) {
        this(random, true);
    }

    /** Brute-force mode is retained for exact differential tests. */
    EnergySunCalcService(Random random, boolean indexed) {
        this.random = random;
        this.indexed = indexed;
    }

    /** Smooth year: 1 at mid-summer (phase 0), winter factor at mid-winter (phase 0.5). */
    public static double seasonFactor(double phase, double winterIntensity) {
        return winterIntensity + (1 - winterIntensity) * (.5 + .5 * Math.cos(2 * Math.PI * phase));
    }

    /** Day/night times season factor of the last calculated step, in [0,1]. */
    public double getCurrentIntensity() { return currentIntensity; }
    public double getSeasonPhase() { return seasonPhase; }

    /** Entry depth of a downward ray into a blocker's box, or infinity if it misses it before tMax. */
    private static double blockerHitT(Blocker blocker, double rayStartX, double rayStartY, double dirX, double dirY, double tMax) {
        double bx = blocker.getPosition().getX() - blocker.getWidth() / 2;
        double by = blocker.getPosition().getY() - blocker.getHeight() / 2;
        double tmin = 0, tmax = tMax;
        if (dirX != 0) {
            double tx1 = (bx - rayStartX) / dirX;
            double tx2 = (bx + blocker.getWidth() - rayStartX) / dirX;
            tmin = Math.max(tmin, Math.min(tx1, tx2));
            tmax = Math.min(tmax, Math.max(tx1, tx2));
        } else if (rayStartX < bx || rayStartX > bx + blocker.getWidth()) {
            return Double.POSITIVE_INFINITY;
        }
        double ty1 = (by - rayStartY) / dirY;
        double ty2 = (by + blocker.getHeight() - rayStartY) / dirY;
        tmin = Math.max(tmin, Math.min(ty1, ty2));
        tmax = Math.min(tmax, Math.max(ty1, ty2));
        return tmax >= tmin && tmin > 0 && tmin < tMax ? tmin : Double.POSITIVE_INFINITY;
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
        // One source type for the complete ray batch; sunlight never fills both stores for free.
        final EnergyType energyType = config.getSunEnergyType();
        // Spectral zones choose each ray's form from where it hits; uniform mode draws no extra numbers.
        final boolean zones = config.isSunSpectralZones();
        final double zoneWidth = SunSpectrum.zoneWidth(width, config.getSunZoneWidth());
        final SunSpectrum.Mix mix = SunSpectrum.Mix.of(config);
        // This call calculates one step's sunlight, not lifetime exposure.
        // Reset even when the accumulator produces no rays or all rays are blocked.
        for (Cell cell : cells) cell.resetSunRayHit();
        cellIndex.clear(); blockerIndex.clear();

        // Update time in the day/night cycle
        this.timeInCycle = (this.timeInCycle + deltaTime) % SimulationConfig.SUN_DAY_NIGHT_CYCLE_DURATION;

        final double seasonDuration = config.getSeasonDuration();
        if (seasonDuration > 0) this.seasonPhase = (this.seasonPhase + deltaTime / seasonDuration) % 1.0D;

        // Calculate intensity based on sinusoidal function, scaled by the season
        final double intensity = ((Math.sin((this.timeInCycle / SimulationConfig.SUN_DAY_NIGHT_CYCLE_DURATION) * Math.PI) *
                (1.0D - SimulationConfig.SUN_NIGHT_INTENSITY)) + SimulationConfig.SUN_NIGHT_INTENSITY)
                * (seasonDuration > 0 ? seasonFactor(this.seasonPhase, config.getWinterIntensity()) : 1.0D);
        this.currentIntensity = intensity;

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
        if (!(dirY > 0)) return rays;
        // The whole sky shines (SPEKTRALZONEN.md, Sonne): rays start evenly along the top edge and wrap around the
        // periodic x axis until they reach the bottom, so slanted light reaches every part of the world. A ray is
        // traced unwrapped; instead of moving objects to their periodic images, the ray is shifted by whole widths.
        final double tMax = height / dirY;
        final double drift = height * dirX / dirY;
        double cellLeft = Double.POSITIVE_INFINITY, cellRight = Double.NEGATIVE_INFINITY;
        for (Cell cell : cells) {
            cellLeft = Math.min(cellLeft, cell.getPosition().getX() - cell.getRadiusSize());
            cellRight = Math.max(cellRight, cell.getPosition().getX() + cell.getRadiusSize());
        }
        double blockerLeft = Double.POSITIVE_INFINITY, blockerRight = Double.NEGATIVE_INFINITY;
        for (Blocker blocker : blockers) {
            blockerLeft = Math.min(blockerLeft, blocker.getPosition().getX() - blocker.getWidth() / 2);
            blockerRight = Math.max(blockerRight, blocker.getPosition().getX() + blocker.getWidth() / 2);
        }
        // Shifts k (ray moved by -k*width) for which some ray can meet some object.
        final double left = Math.min(cellLeft, blockerLeft), right = Math.max(cellRight, blockerRight);
        final int firstShift = left > right ? 0 : (int) Math.floor((Math.min(0, drift) - right) / width);
        final int lastShift = left > right ? -1 : (int) Math.ceil((width + Math.max(0, drift) - left) / width);
        ProjectedSunIndex<Cell> cellStrips = null;
        ProjectedSunIndex<Blocker> blockerStrips = null;
        // For a few rays/cells, scanning avoids index construction overhead.
        double firstRay = (0 - (double) lastShift * width) * dirY, lastRay = (width - (double) firstShift * width) * dirY;
        if (indexed && numRays >= 16 && cells.size() + blockers.size() >= 256 && lastShift >= firstShift
                && cellIndex.begin(firstRay, lastRay, height) && blockerIndex.begin(firstRay, lastRay, height)) {
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
            final double rayStartX = random.nextDouble() * width;
            final double rayStartY = 0;
            final double spanLeft = rayStartX + Math.min(0, drift), spanRight = rayStartX + Math.max(0, drift);
            // Find shade first, so its depth also bounds the subsequent cell traversal.
            double nearestBlockerHitT = Double.POSITIVE_INFINITY;
            double nearestCellHitT = Double.POSITIVE_INFINITY;
            Cell hitCell = null;
            int nearestOrdinal = Integer.MAX_VALUE;
            for (int shift = firstShift; shift <= lastShift; shift++) {
                final double startX = rayStartX - shift * width;
                if (spanRight - shift * width < blockerLeft || spanLeft - shift * width > blockerRight) continue;
                var blockerCandidates = blockerStrips == null ? null : blockerStrips.candidates(startX * dirY);
                Iterator<Blocker> blockerIterator = blockerCandidates == null ? blockers.iterator() : blockerCandidates;
                while (blockerIterator.hasNext()) {
                    if (blockerCandidates != null) {
                        double limitY = dirY * Math.min(nearestBlockerHitT, tMax);
                        if (blockerCandidates.lowerY() > limitY + Math.max(1e-9, 8 * Math.ulp(limitY))) break;
                    }
                    Blocker blocker = blockerIterator.next();
                    double blockerT = blockerHitT(blocker, startX, rayStartY, dirX, dirY, tMax);
                    if (blockerT < nearestBlockerHitT) nearestBlockerHitT = blockerT;
                }
            }
            for (int shift = firstShift; shift <= lastShift; shift++) {
                final double startX = rayStartX - shift * width;
                if (spanRight - shift * width < cellLeft || spanLeft - shift * width > cellRight) continue;
                if (cellStrips == null) {
                    int ordinal = 0;
                    for (Cell cell : cells) {
                        double tCell = circleHitT(cell, startX, rayStartY, dirX, dirY);
                        if (tCell > 0 && tCell < tMax && (tCell < nearestCellHitT
                                || (tCell == nearestCellHitT && ordinal < nearestOrdinal))) {
                            nearestCellHitT = tCell; nearestOrdinal = ordinal; hitCell = cell;
                        }
                        ordinal++;
                    }
                } else {
                    var depthCandidates = cellStrips.candidates(startX * dirY);
                    while (depthCandidates.hasNext()) {
                        double limitY = dirY * Math.min(nearestCellHitT, Math.min(nearestBlockerHitT, tMax));
                        if (depthCandidates.lowerY() > limitY + Math.max(1e-9, 8 * Math.ulp(limitY))) break;
                        int ordinal = depthCandidates.ordinal();
                        Cell cell = depthCandidates.next();
                        double tCell = circleHitT(cell, startX, rayStartY, dirX, dirY);
                        if (tCell > 0 && tCell < tMax && (tCell < nearestCellHitT
                                || (tCell == nearestCellHitT && ordinal < nearestOrdinal))) {
                            nearestCellHitT = tCell;
                            nearestOrdinal = ordinal;
                            hitCell = cell;
                        }
                    }
                }
            }
            double rayHitT = tMax;
            EnergyType rayType = energyType;
            boolean absorbed = false;
            if (hitCell != null && nearestCellHitT <= nearestBlockerHitT) {
                rayHitT = nearestCellHitT;
                if (!hitCell.isRemains()) {
                    if (zones) rayType = SunSpectrum.formAt(rayStartX + dirX * rayHitT, width, zoneWidth, random.nextDouble(), mix);
                    hitCell.setEnergy(rayType, hitCell.getEnergy(rayType) + config.getEnergyPerRay());
                    hitCell.notifySunRayHit();
                    absorbed = true;
                }
            } else if (nearestBlockerHitT < Double.POSITIVE_INFINITY) {
                rayHitT = nearestBlockerHitT;
            }
            // Unwrapped end; the renderer draws the ray wrapped into the world.
            double finalRayEndX = rayStartX + dirX * rayHitT;
            double finalRayEndY = rayStartY + dirY * rayHitT;
            // Unabsorbed rays are only drawn; show the dominant local form without consuming randomness.
            if (zones && !absorbed) rayType = SunSpectrum.dominantAt(finalRayEndX, width, zoneWidth, mix);
            rays.add(new SunRay(
                new Vector2D(rayStartX, rayStartY),
                new Vector2D(finalRayEndX, finalRayEndY),
                rayType
            ));
        }
        return rays;
    }
}
