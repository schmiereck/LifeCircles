package de.lifecircles.service;

import de.lifecircles.model.Blocker;
import java.util.ArrayList;
import java.util.List;

/**
 * Blockers by x bucket for searches within a fixed range, rebuilt per step. A query returns every blocker whose
 * periodic horizontal gap to x is at most the range (possibly more), in blocker-list order, so nearest-blocker
 * ties and force summation order stay as with the full list. Wide worlds (WORLD2) have hundreds of blockers.
 */
final class TerrainIndex {
    private final List<Blocker>[] buckets;
    private final double bucketWidth;

    @SuppressWarnings("unchecked")
    TerrainIndex(List<Blocker> blockers, double worldWidth, double range) {
        double reach = range + 1e-6; // margin against rounding at bucket edges
        int count = Math.max(1, (int) (worldWidth / Math.max(reach, 1)));
        bucketWidth = worldWidth / count;
        buckets = new List[count];
        for (int i = 0; i < count; i++) buckets[i] = new ArrayList<>();
        for (Blocker blocker : blockers) {
            double left = blocker.getPosition().getX() - blocker.getWidth() / 2 - reach;
            double right = blocker.getPosition().getX() + blocker.getWidth() / 2 + reach;
            int first = (int) Math.floor(left / bucketWidth), last = (int) Math.floor(right / bucketWidth);
            if (blocker.getWidth() >= worldWidth || last - first + 1 >= count) {
                for (List<Blocker> bucket : buckets) bucket.add(blocker);
                continue;
            }
            for (int b = first; b <= last; b++) buckets[Math.floorMod(b, count)].add(blocker);
        }
    }

    List<Blocker> near(double x) {
        return buckets[Math.floorMod((int) Math.floor(x / bucketWidth), buckets.length)];
    }
}
