package de.lifecircles.service;

import de.lifecircles.model.Cell;
import de.lifecircles.model.Blocker;
import de.lifecircles.model.SunRay;
import de.lifecircles.model.Vector2D;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

/**
 * Service handling generation of sun energy rays,
 * applying energy to cells, and returning rays for visualization.
 */
public class EnergySunCalcService {
    private double rayAccumulator = 0.0;
    private double timeInCycle = 0.0; // Tracks time within the day/night cycle
    private final Random random = new Random();

    public List<SunRay> calculateSunEnergy(
            final List<Cell> cells,
            final List<Blocker> blockers,
            final double width,
            final double height,
            final SimulationConfig config,
            final double deltaTime) {
        final List<SunRay> rays = new ArrayList<>();

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
        // Sonnenstrahlen starten am oberen Rand (y=0), aber x-Position muss so gewählt werden, dass der Strahl das Fenster trifft
        for (int rayNo = 0; rayNo < numRays; rayNo++) {
            // Strahlen gleichmäßig entlang der oberen Kante verteilen
            final double rayStartX = random.nextDouble() * width;
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
            // Endpunkt des Strahls
            double rayEndX = rayStartX + dirX * tMax;
            double rayEndY = rayStartY + dirY * tMax;
            // Schnitt mit Zellen und Blockern wie gehabt, aber entlang der Strahlrichtung
            double nearestCellHitT = Double.POSITIVE_INFINITY;
            Cell hitCell = null;
            for (Cell cell : cells) {
                // Ray-Circle-Intersection
                Vector2D c = cell.getPosition();
                double r = cell.getRadiusSize();
                double dx = rayStartX - c.getX();
                double dy = rayStartY - c.getY();
                double a = dirX * dirX + dirY * dirY;
                double b = 2 * (dx * dirX + dy * dirY);
                double cVal = dx * dx + dy * dy - r * r;
                double discriminant = b * b - 4 * a * cVal;
                if (discriminant >= 0) {
                    double sqrtDisc = Math.sqrt(discriminant);
                    double t1 = (-b - sqrtDisc) / (2 * a);
                    double t2 = (-b + sqrtDisc) / (2 * a);
                    double tCell = (t1 > 0) ? t1 : ((t2 > 0) ? t2 : Double.POSITIVE_INFINITY);
                    if (tCell > 0 && tCell < nearestCellHitT && tCell < tMax) {
                        nearestCellHitT = tCell;
                        hitCell = cell;
                    }
                }
            }
            double nearestBlockerHitT = Double.POSITIVE_INFINITY;
            for (Blocker blocker : blockers) {
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
