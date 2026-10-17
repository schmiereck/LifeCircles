package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import de.lifecircles.service.trainStrategy.TrainStrategy;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class CellFileImportTest {
    @TempDir Path directory;
    private static class Strategy implements TrainStrategy {
        public void initialize(Environment world) {}
        public void selectAndMutate(Environment world) {}
        public Environment initializeEnvironment() { throw new UnsupportedOperationException(); }
    }
    private Cell cell(double x) {
        return new Cell(new Vector2D(x,100),10,new CellBrain(new NeuralNetwork(
                new ValuesNeuronValueFunctionFactory(),CellFactory.calcInputCount(12),new int[]{1},
                CellFactory.calcOutputCount(12),0,0)));
    }
    private CalculationService service(Environment world,TrainStrategy strategy) {
        return new CalculationService(world,strategy,new SpatialGridPartitioningStrategy(300,300,60));
    }
    private String file() throws Exception {
        String name = directory.resolve("population.cells").toString();
        Cell a=cell(150),b=cell(200);
        a.setEnergy(EnergyType.SECONDARY,.625);b.setEnergy(EnergyType.SECONDARY,.375);
        FileService.getInstance().saveCellsToFile(name,new ArrayList<>(List.of(a,b)));
        return name;
    }

    @ParameterizedTest
    @CsvSource({"true,true,true","true,true,false","true,false,true","true,false,false",
                "false,true,true","false,true,false","false,false,true","false,false,false"})
    void loadPublishesImmediatelyWithoutAdvancingSimulation(boolean merge,boolean paused,boolean actors) throws Exception {
        Environment world = new Environment(300,300); Cell original = cell(100); world.addCell(original);
        CalculationService service = service(world,new Strategy());
        if (paused) service.pause();
        var before = service.getLatestState(actors);
        service.loadCellsFromFile(file(),merge);
        var after = service.getLatestState(actors);
        assertNotSame(before,after); assertSame(after,service.getLatestState(actors));
        assertEquals(1,before.getCells().size()); assertEquals(merge ? 3 : 2,after.getCells().size());
        assertEquals(merge,world.getCellList().contains(original));
        assertEquals(0,service.getStepCount()); assertEquals(paused,service.isPaused()); assertFalse(service.isRunning());
        for (var cell : after.getCells()) {
            assertEquals(0,cell.getAge()); assertEquals(actors ? 12 : 0,cell.getActors().size());
        }
        assertEquals(merge ? 3 : 2,service.getLatestState(!actors).getCells().size());
        assertEquals(.625,after.getCells().get(merge ? 1 : 0).getEnergy(EnergyType.SECONDARY));
        assertEquals(.375,after.getCells().get(merge ? 2 : 1).getEnergy(EnergyType.SECONDARY));
    }

    @Test void failedReadPreservesPopulationAndSnapshot() throws Exception {
        Environment world = new Environment(300,300); world.addCell(cell(100));
        CalculationService service = service(world,new Strategy());
        var before = service.getLatestState();
        Path broken = directory.resolve("broken.cells"); Files.writeString(broken,"not a saved population");
        assertThrows(java.io.IOException.class,()->service.loadCellsFromFile(broken.toString(),false));
        assertSame(before,service.getLatestState()); assertEquals(1,world.getCellList().size());
    }

    @Test void rejectedTrainingImportPreservesPopulationAndSnapshot() {
        Environment world=new Environment(300,300);Cell original=cell(100);world.addCell(original);
        var strategy=new Strategy() {
            public void onPopulationImported(Environment environment,List<Cell> imported,boolean merge) {
                throw new IllegalArgumentException("Unsupported training genome");
            }
        };
        CalculationService service=service(world,strategy);var before=service.getLatestState();
        assertThrows(IllegalArgumentException.class,()->service.importCells(List.of(cell(150)),false));
        assertEquals(List.of(original),world.getCellList());assertSame(before,service.getLatestState());
    }

    @Test @Timeout(10) void importWaitsForCompleteSimulationStep() throws Exception {
        Environment world = new Environment(300,300);
        CountDownLatch selecting = new CountDownLatch(1), release = new CountDownLatch(1);
        CalculationService service = service(world,new Strategy() {
            @Override public void selectAndMutate(Environment w) {
                selecting.countDown();
                try { assertTrue(release.await(5,TimeUnit.SECONDS)); }
                catch (InterruptedException e) { throw new AssertionError(e); }
            }
        });
        try (ExecutorService workers = Executors.newFixedThreadPool(2)) {
            Future<?> step = workers.submit(()->service.update(0));
            try {
                assertTrue(selecting.await(5,TimeUnit.SECONDS));
                List<Cell> imported = List.of(cell(150)); CountDownLatch importing = new CountDownLatch(1);
                Future<?> load = workers.submit(()->{ importing.countDown(); service.importCells(imported,true); });
                assertTrue(importing.await(5,TimeUnit.SECONDS));
                assertThrows(TimeoutException.class,()->load.get(100,TimeUnit.MILLISECONDS));
                release.countDown(); step.get(5,TimeUnit.SECONDS); load.get(5,TimeUnit.SECONDS);
                assertEquals(1,service.getLatestState().getCells().size());
            } finally { release.countDown(); }
        }
    }
}
