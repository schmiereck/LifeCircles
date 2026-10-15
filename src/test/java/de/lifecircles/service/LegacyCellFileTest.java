package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import de.lifecircles.service.trainStrategy.TrainStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Optional real-save compatibility check; never modifies the input file. */
@EnabledIfSystemProperty(named="legacy.cells",matches=".+")
class LegacyCellFileTest {
    @Test void inspectAndSimulateSavedPopulation() throws Exception {
        var cells = FileService.getInstance().loadCellsFromFileWithProgress(System.getProperty("legacy.cells"));
        assertFalse(cells.isEmpty());
        System.out.printf(Locale.ROOT,"Loaded: %d; X %.3f..%.3f; Y %.3f..%.3f; radius %.3f..%.3f; energy %.6f..%.6f%n",
                cells.size(),cells.stream().mapToDouble(c->c.getPosition().getX()).min().orElseThrow(),
                cells.stream().mapToDouble(c->c.getPosition().getX()).max().orElseThrow(),
                cells.stream().mapToDouble(c->c.getPosition().getY()).min().orElseThrow(),
                cells.stream().mapToDouble(c->c.getPosition().getY()).max().orElseThrow(),
                cells.stream().mapToDouble(Cell::getRadiusSize).min().orElseThrow(),
                cells.stream().mapToDouble(Cell::getRadiusSize).max().orElseThrow(),
                cells.stream().mapToDouble(Cell::getEnergy).min().orElseThrow(),
                cells.stream().mapToDouble(Cell::getEnergy).max().orElseThrow());
        Environment world = new Environment(6400,1200);
        TrainStrategy strategy = new TrainStrategy() {
            public void initialize(Environment w) {}
            public void selectAndMutate(Environment w) {}
            public Environment initializeEnvironment() { throw new UnsupportedOperationException(); }
        };
        var service = new CalculationService(world,strategy,new SpatialGridPartitioningStrategy(6400,1200,60));
        service.pause(); assertEquals(0,service.getLatestState().getCells().size());
        service.importCells(cells,true);
        assertEquals(cells.size(),service.getLatestState().getCells().size());
        System.out.printf("World cells: %d; imported snapshot cells: %d%n",world.getCellList().size(),service.getLatestState().getCells().size());
        service.update(1.0/60);
        System.out.printf("After one step: %d visible cells%n",service.getLatestState().getCells().size());
        assertFalse(service.getLatestState().getCells().isEmpty());
        for (Cell cell : world.getCellList()) {
            assertTrue(Double.isFinite(cell.getPosition().getX()));
            assertTrue(Double.isFinite(cell.getPosition().getY()));
            assertTrue(Double.isFinite(cell.getRadiusSize()));
            assertTrue(Double.isFinite(cell.getEnergy()));
        }
    }
}
