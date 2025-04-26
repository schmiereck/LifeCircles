package de.lifecircles.service;

import de.lifecircles.model.Cell;
import de.lifecircles.model.SensorActor;
import de.lifecircles.model.SunRay;
import de.lifecircles.model.Vector2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for processing energy beams between cells.
 */
public class EnergyBeamCellCalcService {
    // Fraction of cell energy emitted per beam
    private static final double ENERGY_BEAM_THRESHOLD = 0.001;

    /**
     * Processes sensor-actor energy beams and returns the resulting SunRays.
     * Updates cell energies (source and hit cells) internally.
     * @param cells list of cells in the environment
     * @param width simulation width (unused but kept for signature consistency)
     * @param height simulation height (unused but kept for signature consistency)
     * @return list of SunRay to visualize energy beams
     */
    public static List<SunRay> processEnergyBeams(List<Cell> cells, double width, double height) {
        List<SunRay> beams = new ArrayList<>();
        // partition spatially to limit checks
        PartitioningStrategy partitioner = PartitioningStrategyFactory.createStrategy(width, height, Cell.getMaxSize());
        partitioner.build(cells);

        for (Cell cell : cells) {
            Vector2D start = cell.getPosition();
            for (SensorActor actor : cell.getSensorActors()) {
                if (!actor.shouldFireEnergyBeam()) continue;

                double beamEnergy = cell.getEnergy() * ENERGY_BEAM_THRESHOLD;
                cell.setEnergy(cell.getEnergy() - beamEnergy);

                Vector2D dir = actor.getPosition().subtract(start).normalize();
                double maxDist = Cell.getMaxSize();
                Vector2D farEnd = start.add(dir.multiply(maxDist));

                Cell hitCell = null;
                double minT = Double.MAX_VALUE;

                for (Cell other : partitioner.getNeighbors(cell)) {
                    if (other == cell) continue;
                    Vector2D toOther = other.getPosition().subtract(start);
                    double proj = dir.dot(toOther);
                    double radius = other.getSize() / 2;
                    // quick reject: beam misses circle
                    if (proj + radius < 0 || proj - radius > maxDist) continue;
                    double d2 = toOther.lengthSquared() - proj * proj;
                    if (d2 > radius * radius) continue;
                    double thc = Math.sqrt(radius * radius - d2);
                    double t0 = proj - thc;
                    double t1 = proj + thc;
                    double tHit = (t0 >= 0 ? t0 : (t1 >= 0 ? t1 : -1));
                    if (tHit < 0 || tHit > maxDist) continue;
                    if (tHit < minT) {
                        minT = tHit;
                        hitCell = other;
                    }
                }

                Vector2D endPoint;
                if (hitCell != null) {
                    endPoint = start.add(dir.multiply(minT));
                    hitCell.setEnergy(hitCell.getEnergy() + beamEnergy);
                } else {
                    endPoint = farEnd;
                }
                beams.add(new SunRay(start, endPoint));
            }
        }
        return beams;
    }
}
