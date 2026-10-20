package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.trainStrategy.World2TrainStrategy;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** The bucket index returns every blocker within range, in list order, also across the periodic edge. */
class TerrainIndexTest {
    @Test void nearContainsEveryBlockerWithinRangeInListOrder() {
        double width = SimulationConfig.getInstance().getWidth(), height = SimulationConfig.getInstance().getHeight();
        var strategy = new World2TrainStrategy(new Random(1));
        Environment world = strategy.initializeEnvironment(); strategy.initialize(world);
        SimulationConfig.getInstance().setWidth(width); SimulationConfig.getInstance().setHeight(height);
        List<Blocker> blockers = world.getBlockerList();
        Random random = new Random(2);
        for (double range : new double[]{13.7, 40, 900}) {
            var index = new TerrainIndex(blockers, world.getWidth(), range);
            for (int i = 0; i < 3000; i++) {
                double x = random.nextDouble() * 3 * world.getWidth() - world.getWidth();
                Vector2D p = new Vector2D(x, random.nextDouble() * world.getHeight());
                List<Blocker> near = index.near(x);
                for (Blocker b : blockers)
                    if (b.offsetToSurface(p, world.getWidth(), world.getHeight()).length() <= range) assertTrue(near.contains(b), "x=" + x + " " + b.getPosition());
                int last = -1;
                for (Blocker b : near) { int at = blockers.indexOf(b); assertTrue(at > last, "order"); last = at; }
            }
            assertTrue(index.near(100).size() < blockers.size() / 4 || range > 500);
        }
    }
}
