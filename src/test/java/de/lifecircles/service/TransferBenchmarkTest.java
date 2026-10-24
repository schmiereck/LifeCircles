package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import de.lifecircles.service.trainStrategy.World2TrainStrategy;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Opt-in: energy transfers of a saved WORLD2 population after one interaction pass (sensed ports), from the same
 * energy state each call. Compares with the former algorithm bit for bit, then time per call. Example:
 * mvn test -Dtest=TransferBenchmarkTest -Dtransfer.bench.cells=target/trainmode-study/prof-world2.cells
 */
@EnabledIfSystemProperty(named="transfer.bench.cells",matches=".+")
class TransferBenchmarkTest {
    static final int ROUNDS=Integer.getInteger("transfer.bench.rounds",400);

    @Test void world2Population() throws Exception {
        SimulationConfig config=SimulationConfig.getInstance();
        Environment world=new World2TrainStrategy(new Random(1)).initializeEnvironment();
        new World2TrainStrategy(new Random(1)).initialize(world);
        world.resetCells(FileService.getInstance().loadCellsFromFile(System.getProperty("transfer.bench.cells")));
        List<Cell> cells=world.getCellList();
        var grid=new SpatialGridPartitioningStrategy(world.getWidth(),world.getHeight(),(config.getCellMaxRadiusSize()+config.getCellActorMaxFieldRadius())*2);
        grid.build(cells);
        SensorActorForceCellCalcService.processInteractions(cells,grid,world,SensorActorForceCellCalcService.Affinity.fromConfig(),
                SensorActorForceCellCalcService.Ground.fromConfig(true));
        double dt=1.0/60;
        double[][] before=EnergyTransferLinkRegressionTest.snapshot(cells);
        EnergyTransferLinkRegressionTest.reference(cells,world,dt);
        double[][] expected=EnergyTransferLinkRegressionTest.snapshot(cells);
        EnergyTransferLinkRegressionTest.restore(cells,before);
        EnergyTransferCellCalcService.processEnergyTransfers(cells,world,dt);
        double[][] actual=EnergyTransferLinkRegressionTest.snapshot(cells);
        EnergyTransferLinkRegressionTest.restore(cells,before);
        int changed=0;
        for(int i=0;i<cells.size();i++){assertArrayEquals(expected[i],actual[i],0,"cell "+i);if(!Arrays.equals(before[i],actual[i]))changed++;}
        System.out.printf(Locale.ROOT,"TRANSBENCH cells=%d changed=%d%n",cells.size(),changed);
        long[] time=new long[2];
        for(int r=0;r<ROUNDS;r++)for(int k=0;k<2;k++) {
            int x=(k+r)%2;long t=System.nanoTime();
            if(x==0)EnergyTransferLinkRegressionTest.reference(cells,world,dt);else EnergyTransferCellCalcService.processEnergyTransfers(cells,world,dt);
            long spent=System.nanoTime()-t;
            EnergyTransferLinkRegressionTest.restore(cells,before);
            if(r>=ROUNDS/4)time[x]+=spent;
        }
        int measured=ROUNDS-ROUNDS/4;
        System.out.printf(Locale.ROOT,"TRANSBENCH reference %.3f ms/call, current %.3f ms/call (%d calls)%n",time[0]/1e6/measured,time[1]/1e6/measured,measured);
    }
}
