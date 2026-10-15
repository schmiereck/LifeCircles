package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import de.lifecircles.service.trainStrategy.TestTrainStrategyUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

final class StandingStructureLab {
    record Result(double heightRatio,double widthRatio,double shape,double component,double speed,double overlap) {}
    static Result run(MechanicsLab.Parameters profile,int columns,int rows,double radius,double dt) {
        MechanicsLab.configure(profile);
        int steps = (int)Math.round(30/dt), lateStart = (int)Math.round(25/dt);
        Environment world = new Environment(1400,1200); world.addGroundBlocker();
        for (int row=0;row<rows;row++) for (int col=0;col<columns;col++) {
            double x = 700 + (col-(columns-1)/2.0)*2*radius + (row%2)*radius;
            // Break perfect symmetry with a tiny, reproducible top displacement.
            if (row==rows-1) x += .01*radius;
            double y = world.getHeight()-Environment.GroundBlockerHeight-radius-row*Math.sqrt(3)*radius;
            TestTrainStrategyUtils.createAndAddCell(world,x,y,radius,true,false,false,false);
        }
        List<Cell> body = new ArrayList<>(world.getCellList());
        var reference = MechanicsLab.positions(body);
        double height = MechanicsLab.height(body), width = width(body), overlap = 0, speed = 0;
        int samples=0;
        var grid = new SpatialGridPartitioningStrategy(1400,1200,80);
        for (int step=0;step<steps;step++) {
            grid.build(world.getCellList()); world.update(dt,grid);
            assertEquals(body.size(),world.getCellList().size());
            for (Cell cell : body) {
                assertTrue(Double.isFinite(cell.getPosition().getX()));
                assertTrue(Double.isFinite(cell.getPosition().getY()));
            }
            for (int i=0;i<body.size();i++) for (int j=i+1;j<body.size();j++)
                overlap=Math.max(overlap,Math.max(0,1-body.get(i).getPosition().distance(body.get(j).getPosition())/(2*radius)));
            if(step>=lateStart) {
                speed+=Math.sqrt(body.stream().mapToDouble(c->c.getVelocity().length()*c.getVelocity().length()).average().orElseThrow()); samples++;
            }
        }
        return new Result(MechanicsLab.height(body)/height,width(body)/width,
                MechanicsLab.shapeError(reference,body,radius),MechanicsLab.largestComponent(body)/(double)body.size(),speed/samples,overlap);
    }
    private static double width(List<Cell> cells) {
        return cells.stream().mapToDouble(c->c.getPosition().getX()+c.getRadiusSize()).max().orElseThrow()
                - cells.stream().mapToDouble(c->c.getPosition().getX()-c.getRadiusSize()).min().orElseThrow();
    }
}
