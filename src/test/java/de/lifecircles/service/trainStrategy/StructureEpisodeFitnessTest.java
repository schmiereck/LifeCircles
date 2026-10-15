package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class StructureEpisodeFitnessTest {
    Environment world() { Environment world=new Environment(11200,1200);world.addGroundBlocker();return world; }
    StructureEpisodeFitness.Body body(Environment world) {
        List<Cell> cells=new ArrayList<>();
        for(int row=0;row<12;row++) for(int col=0;col<5;col++) {
            Cell cell=new Cell(new Vector2D(700+(col-2)*40+(row%2)*20,1130-row*Math.sqrt(3)*20),20,null);
            world.addCell(cell);cells.add(cell);
        }
        return new StructureEpisodeFitness.Body(0,null,cells);
    }
    void episode(StructureEpisodeFitness fitness,Environment world,StructureEpisodeFitness.Body body,boolean building) {
        for(int i=1;i<=60;i++) fitness.sample(world,List.of(body),i*.5,building);
    }
    @Test void rigidMotionIsNotDeformationButShearingIs() {
        var world=world();var body=body(world);
        for(Cell cell:body.cells) {
            var p=cell.getPosition();cell.setPosition(new Vector2D(10-p.getY(),30+p.getX()));
        }
        assertEquals(0,StructureEpisodeFitness.shapeError(body.reference,body.cells),1e-6);
        body.cells.getFirst().getPosition().setX(1000);
        assertTrue(StructureEpisodeFitness.shapeError(body.reference,body.cells)>1);
    }
    @Test void stablePackScoresOneAndAirbornePackScoresZero() {
        var world=world();var body=body(world);var fitness=new StructureEpisodeFitness(8);
        episode(fitness,world,body,false);assertEquals(1,body.summary(false).score(),1e-9);assertTrue(body.summary(false).mastered());
        world=world();body=body(world);
        for(Cell cell:body.cells) cell.setPosition(cell.getPosition().add(new Vector2D(0,-100)));
        episode(new StructureEpisodeFitness(8),world,body,false);
        assertEquals(0,body.summary(false).score());assertFalse(body.summary(false).mastered());
    }
    @Test void lastMomentRepairDoesNotReplaceWholeEpisodeFitness() {
        var world=world();var body=body(world);var fitness=new StructureEpisodeFitness(8);
        for(Cell cell:body.cells) cell.setPosition(cell.getPosition().add(new Vector2D(0,-100)));
        for(int i=1;i<=50;i++) fitness.sample(world,List.of(body),i*.5,false);
        for(Cell cell:body.cells) cell.setPosition(body.reference.get(cell));
        for(int i=51;i<=60;i++) fitness.sample(world,List.of(body),i*.5,false);
        assertEquals(1.0/6,body.summary(false).score(),1e-9);assertFalse(body.summary(false).mastered());
    }
    @Test void earlyConstructionScoresHigherThanLateConstruction() {
        double[] scores=new double[2];
        for(int run=0;run<2;run++) {
            var world=world();var body=body(world);var fitness=new StructureEpisodeFitness(8);
            for(Cell cell:body.cells) cell.setPosition(cell.getPosition().add(new Vector2D(0,-100)));
            for(int i=1;i<=60;i++) {
                if(i==(run==0?5:40)) for(Cell cell:body.cells) cell.setPosition(body.reference.get(cell));
                fitness.sample(world,List.of(body),i*.5,true);
            }
            scores[run]=body.summary(true).score();
        }
        assertTrue(scores[0]>scores[1]+.3);
    }
    @Test void lostMembersAndInvalidBodiesCannotWin() {
        var world=world();var body=body(world);world.getCellList().removeFirst();
        episode(new StructureEpisodeFitness(8),world,body,false);assertEquals(0,body.summary(false).score());
        world=world();body=body(world);body.invalid=true;
        episode(new StructureEpisodeFitness(8),world,body,false);assertEquals(0,body.summary(false).score());
    }
}
