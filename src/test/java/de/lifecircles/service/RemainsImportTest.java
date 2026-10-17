package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import de.lifecircles.service.trainStrategy.TrainStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RemainsImportTest {
    @TempDir Path directory;
    private Cell living(double x){return CellFactory.createCell(new Vector2D(x,100),10,.02,0,0);}
    @Test void fullSaveLoadAndMergePreserveAllEightReservesAndCorpseSnapshots() throws Exception {
        Cell corpse=living(150);for(EnergyType type:EnergyType.ALL)corpse.setEnergy(type,.05+.1*type.getCode());corpse.becomeRemains();
        String file=directory.resolve("remains.cells").toString();FileService.getInstance().saveCellsToFile(file,new ArrayList<>(List.of(corpse)));
        Environment world=new Environment(300,300);world.addCell(living(80));
        TrainStrategy strategy=new TrainStrategy() {
            public void initialize(Environment w){}
            public void selectAndMutate(Environment w){}
            public Environment initializeEnvironment(){throw new UnsupportedOperationException();}
        };
        var service=new CalculationService(world,strategy,new SpatialGridPartitioningStrategy(300,300,60));
        service.loadCellsFromFile(file,true);var snapshot=service.getLatestState().getCells();assertEquals(2,snapshot.size());
        assertFalse(snapshot.getFirst().isRemains());assertTrue(snapshot.getLast().isRemains());
        for(EnergyType type:EnergyType.ALL)assertEquals(corpse.getEnergy(type),snapshot.getLast().getEnergy(type));
        assertSame(RemainsBrain.INSTANCE,world.getCellList().getLast().getBrain());
        world.getCellList().getLast().setEnergy(EnergyType.RGB_111,.1);
        assertEquals(.75,snapshot.getLast().getEnergy(EnergyType.RGB_111),1e-14);
    }
    @Test void saveBestDoesNotExportDeadCellsAsGenomeCandidates() throws Exception {
        Cell live=living(100),dead=living(200);live.setEnergy(.2);dead.setEnergy(.9);dead.becomeRemains();
        String file=directory.resolve("best.cells").toString();
        FileService.getInstance().saveBestCellsToFile(file,List.of(dead,live),300);
        var loaded=FileService.getInstance().loadCellsFromFile(file);assertEquals(1,loaded.size());
        assertFalse(loaded.getFirst().isRemains());assertInstanceOf(CellBrain.class,loaded.getFirst().getBrain());assertEquals(.2,loaded.getFirst().getEnergy());
    }
}
