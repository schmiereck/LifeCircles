package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import de.lifecircles.service.trainStrategy.World2TrainStrategy;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Opt-in: sensor/actor interactions of a saved WORLD2 population in the WORLD2 terrain, as in one step with
 * adhesion, recognition and ground fields. Compares the former algorithm with the current one: same forces and
 * sensed targets bit for bit, then time per call (parallel, as in the simulation). Example:
 * mvn test -Dtest=InteractionBenchmarkTest -Dinteraction.bench.cells=target/trainmode-study/prof-world2.cells
 */
@EnabledIfSystemProperty(named="interaction.bench.cells",matches=".+")
class InteractionBenchmarkTest {
    static final int ROUNDS=Integer.getInteger("interaction.bench.rounds",200);

    @Test void world2Population() throws Exception {
        SimulationConfig config=SimulationConfig.getInstance();
        Environment world=new World2TrainStrategy(new Random(1)).initializeEnvironment();
        new World2TrainStrategy(new Random(1)).initialize(world);
        List<Cell> cells=FileService.getInstance().loadCellsFromFile(System.getProperty("interaction.bench.cells"));
        world.resetCells(cells);
        var grid=new SpatialGridPartitioningStrategy(world.getWidth(),world.getHeight(),(config.getCellMaxRadiusSize()+config.getCellActorMaxFieldRadius())*2);
        grid.build(world.getCellList());
        var affinity=SensorActorForceCellCalcService.Affinity.fromConfig();
        var ground=SensorActorForceCellCalcService.Ground.fromConfig(true);
        List<Cell> list=world.getCellList();
        System.out.printf(Locale.ROOT,"INTERBENCH cells=%d blockers=%d%n",list.size(),world.getBlockerList().size());

        // Equality from the same state: forces become velocities through updateForce.
        double[][] before=state(list);
        SensorActorForceReference.processInteractions(list,grid,world,affinity,ground);
        List<Object> expected=observe(list);
        restore(list,before);
        SensorActorForceCellCalcService.processInteractions(list,grid,world,affinity,ground);
        List<Object> actual=observe(list);
        restore(list,before);
        assertEquals(expected.size(),actual.size());
        for(int i=0;i<expected.size();i++)assertEquals(expected.get(i),actual.get(i),"observation "+i);

        long[] time=new long[2];
        for(int r=0;r<ROUNDS;r++)for(int k=0;k<2;k++) {
            int x=(k+r)%2;long t=System.nanoTime();
            if(x==0)SensorActorForceReference.processInteractions(list,grid,world,affinity,ground);
            else SensorActorForceCellCalcService.processInteractions(list,grid,world,affinity,ground);
            if(r>=ROUNDS/4)time[x]+=System.nanoTime()-t;
        }
        int measured=ROUNDS-ROUNDS/4;
        System.out.printf(Locale.ROOT,"INTERBENCH reference %.3f ms/call, current %.3f ms/call (%d calls)%n",
                time[0]/1e6/measured,time[1]/1e6/measured,measured);
    }

    static double[][] state(List<Cell> cells) {
        double[][] s=new double[cells.size()][];
        for(int i=0;i<s.length;i++){Cell c=cells.get(i);s[i]=new double[]{c.getVelocity().getX(),c.getVelocity().getY(),c.getAngularVelocity()};}
        return s;
    }
    static void restore(List<Cell> cells,double[][] s) {
        for(int i=0;i<s.length;i++){Cell c=cells.get(i);c.updateForce(0);c.setVelocity(new Vector2D(s[i][0],s[i][1]));c.setAngularVelocity(s[i][2]);}
    }
    /** Velocities after applying the accumulated forces, and every actor's sensed cell and actor. */
    static List<Object> observe(List<Cell> cells) {
        List<Object> out=new ArrayList<>();
        for(Cell c:cells) {
            c.updateForce(1.0/60);
            out.add(Double.doubleToLongBits(c.getVelocity().getX()));out.add(Double.doubleToLongBits(c.getVelocity().getY()));
            out.add(Double.doubleToLongBits(c.getAngularVelocity()));
            for(SensorActor a:c.getSensorActors()){out.add(System.identityHashCode(a.getSensedCell()));out.add(System.identityHashCode(a.getSensedActor()));}
        }
        return out;
    }
}
