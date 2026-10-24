package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import de.lifecircles.service.trainStrategy.World2TrainStrategy;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Opt-in: how long the render thread's snapshot holds the simulation lock (the calculation waits meanwhile).
 * mvn test -Dtest=SnapshotBenchmarkTest -Dsnapshot.bench.cells=target/trainmode-study/prof-world2.cells
 */
@EnabledIfSystemProperty(named="snapshot.bench.cells",matches=".+")
class SnapshotBenchmarkTest {
    @Test void world2Population() throws Exception {
        SimulationConfig config=SimulationConfig.getInstance();
        World2TrainStrategy strategy=new World2TrainStrategy(new Random(1));
        Environment world=strategy.initializeEnvironment();
        var grid=new SpatialGridPartitioningStrategy(world.getWidth(),world.getHeight(),(config.getCellMaxRadiusSize()+config.getCellActorMaxFieldRadius())*2);
        CalculationService service=new CalculationService(world,strategy,grid);
        world.resetCells(FileService.getInstance().loadCellsFromFile(System.getProperty("snapshot.bench.cells")));
        long[] time=new long[2];int rounds=400;
        for(int r=0;r<rounds;r++)for(int k=0;k<2;k++) {
            boolean actors=(r+k)%2==0;
            long t=System.nanoTime();service.getLatestState(actors);long spent=System.nanoTime()-t;
            if(r>=rounds/4)time[actors?1:0]+=spent;
        }
        int measured=rounds-rounds/4;
        System.out.printf(Locale.ROOT,"SNAPBENCH cells=%d without actors %.3f ms, with actors %.3f ms per snapshot%n",
                world.getCellList().size(),time[0]/1e6/measured,time[1]/1e6/measured);
    }
}
