package de.lifecircles.service;

import de.lifecircles.model.Cell;
import de.lifecircles.model.Environment;
import de.lifecircles.service.trainStrategy.DefaultTrainStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.*;
import java.util.List;

/**
 * Opt-in: draws bond snapshots of a saved population in the default world's terrain, e.g. the final
 * NAME.cells of a zone study: mvn test -Dtest=BondSnapshotReplayTest -Dbond.snapshot.load=PATH
 * Writes target/zone-season-study/FILE-replay-t0*.png. Saved populations hold only living cells,
 * so bonds to remains are missing.
 */
@EnabledIfSystemProperty(named = "bond.snapshot.load", matches = ".+")
class BondSnapshotReplayTest {
    @Test void replay() throws Exception {
        Path file = Paths.get(System.getProperty("bond.snapshot.load"));
        var strategy = new DefaultTrainStrategy();
        Environment world = strategy.initializeEnvironment(); strategy.initialize(world);
        List<Cell> cells = FileService.getInstance().loadCellsFromFile(file.toString());
        world.resetCells(cells);
        Path dir = Paths.get("target", "zone-season-study"); Files.createDirectories(dir);
        String name = file.getFileName().toString().replaceFirst("\\.cells$", "") + "-replay";
        for (List<Cell> body : ZoneSeasonStudyTest.bondedBodies(world)) {
            double[] shape = ZoneSeasonStudyTest.bodyShape(world, body);
            if (body.size() >= 5 || shape[1] >= 80) System.out.printf("BONDREPLAY %s body %d cells span %.0f rise %.0f%n", name, body.size(), shape[0], shape[1]);
        }
        ZoneSeasonStudyTest.writeSnapshots(world, dir, name, 0);
    }
}
