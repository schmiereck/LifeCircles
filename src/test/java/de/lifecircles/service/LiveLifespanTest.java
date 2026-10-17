package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LiveLifespanTest {
    @Test void loweringLifespanRemovesOlderCellsOnNextUpdateButKeepsYoungerCells() {
        var config=SimulationConfig.getInstance();double previous=config.getCellDeathAge(),sun=config.getSunRayRate();
        try {
            config.setSunRayRate(0);config.setCellDeathAge(10000);
            Environment world=new Environment(300,300);
            Cell older=new Cell(new Vector2D(80,100),10,new PhysicsRegressionTest.ConstantBrain());
            Cell younger=new Cell(new Vector2D(180,100),10,new PhysicsRegressionTest.ConstantBrain());
            older.setAge(200);younger.setAge(50);older.setEnergy(.2);younger.setEnergy(.2);
            world.addCell(older);world.addCell(younger);
            var grid=new SpatialGridPartitioningStrategy(300,300,60);
            grid.build(world.getCellList());world.update(0,grid);
            assertEquals(2,world.getCellList().size());
            config.setCellDeathAge(100);
            grid.build(world.getCellList());world.update(0,grid);
            assertEquals(2,world.getCellList().size());assertTrue(older.isRemains());assertFalse(younger.isRemains());
            config.setCellDeathAge(1000);younger.setAge(100);
            grid.build(world.getCellList());world.update(0,grid);assertFalse(younger.isRemains());
            younger.setAge(1000);
            grid.build(world.getCellList());world.update(0,grid);assertTrue(younger.isRemains());
        } finally {config.setCellDeathAge(previous);config.setSunRayRate(sun);}
    }
    @Test void rejectsUnusableLifespanWithoutChangingThePreviousSetting() {
        var config=SimulationConfig.getInstance();double previous=config.getCellDeathAge();
        for(double value:new double[]{0,-1,Double.NaN,Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class,()->config.setCellDeathAge(value));
            assertEquals(previous,config.getCellDeathAge());
        }
    }
}
