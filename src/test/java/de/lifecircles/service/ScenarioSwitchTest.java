package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.trainStrategy.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class ScenarioSwitchTest {
    final SimulationConfig config=SimulationConfig.getInstance();
    double width,height,scale;
    TrainMode mode;
    CalculationService service;
    @BeforeEach void setup() {
        width=config.getWidth();height=config.getHeight();scale=config.getScaleSimulation();mode=config.getTrainMode();
        Environment world=new Environment(300,300);
        TrainStrategy strategy=new TrainStrategy() {
            public void initialize(Environment w) {}
            public void selectAndMutate(Environment w) {}
            public Environment initializeEnvironment(){return world;}
        };
        service=new CalculationService(world,strategy,new SpatialGridPartitioningStrategy(300,300,80));
    }
    @AfterEach void cleanup() {
        service.stop();config.setWidth(width);config.setHeight(height);config.setScaleSimulation(scale);config.setTrainMode(mode);
    }
    private void awaitSteps() throws Exception {
        long deadline=System.nanoTime()+3_000_000_000L;
        while(service.getStepCount()<2 && System.nanoTime()<deadline)Thread.sleep(5);
        assertTrue(service.getStepCount()>=2);
    }
    @Test void switchWhileRunningPublishesNewPausedWorldAndResetDoesNotDuplicateBlockers() throws Exception {
        service.start();service.start();awaitSteps();
        var old=service.getLatestState();
        service.newSimulation(TrainMode.CARRION_RAIN);
        assertTrue(service.isPaused());assertEquals(0,service.getStepCount());
        var state=service.getLatestState();assertNotSame(old,state);assertEquals(3200,state.getWidth());
        assertEquals(16,state.getCells().size());assertEquals(10,state.getBlockers().size());
        assertEquals(8,StatisticsManagerService.getInstance().getTotalPopulation());
        assertEquals(8,StatisticsManagerService.getInstance().getRemainsPopulation());
        assertTrue(StatisticsManagerService.getInstance().getTotalHistory().isEmpty());
        Thread.sleep(50);assertEquals(0,service.getStepCount());assertSame(state,service.getLatestState());
        service.resetSimulation();assertEquals(10,service.getLatestState().getBlockers().size());
        service.start();awaitSteps();service.pause();
        long steps=service.getStepCount();Thread.sleep(50);assertEquals(steps,service.getStepCount());
        assertEquals(1,Thread.getAllStackTraces().keySet().stream().filter(t->t.isAlive() && t.getName().equals("LifeCircles calculation")).count());
    }
    @Test void stopAndImmediateRestartUseOneWorkerAndCanAdvanceAgain() throws Exception {
        service.start();awaitSteps();service.stop();assertFalse(service.isRunning());
        long steps=service.getStepCount();service.start();
        long deadline=System.nanoTime()+3_000_000_000L;
        while(service.getStepCount()==steps && System.nanoTime()<deadline)Thread.sleep(5);
        assertTrue(service.getStepCount()>steps);service.stop();assertFalse(service.isRunning());
    }
}
