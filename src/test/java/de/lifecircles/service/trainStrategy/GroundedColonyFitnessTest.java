package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.*;
import de.lifecircles.service.SimulationConfig;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class GroundedColonyFitnessTest {
    private final GroundedColonyFitness fitness = new GroundedColonyFitness(16);
    private Environment world() {
        Environment world = new Environment(6400,1200); world.addGroundBlocker(); return world;
    }
    private double floor(Environment world) {
        var ground = world.getBlockerList().getFirst(); return ground.getPosition().getY()-ground.getHeight()/2;
    }
    private Cell add(Environment world,double x,double y) {
        Cell cell = new Cell(new Vector2D(x,y),10,null); world.addCell(cell); return cell;
    }
    private List<Cell> tower(Environment world,double x,int rows) {
        List<Cell> cells = new ArrayList<>();
        for (int row=0;row<rows;row++) cells.add(add(world,x,floor(world)-10-20*row));
        return cells;
    }
    private void sample(Environment world,int count) { for(int i=0;i<count;i++) fitness.sample(world); }

    @Test void contactChainCreditsBaseAndUpperCellsTogether() {
        Environment world = world(); var body = tower(world,200,6); var ratings = fitness.current(world);
        assertEquals(6,ratings.size()); assertTrue(ratings.get(body.getFirst()).base());
        assertFalse(ratings.get(body.getLast()).base());
        for(Cell cell:body) assertEquals(ratings.get(body.getFirst()).score(),ratings.get(cell).score());
        sample(world,20); assertSame(body.getFirst(),fitness.ranked(world).getFirst());
    }

    @Test void detachedHighCellCannotBeatGroundedTower() {
        Environment world = world(); var body = tower(world,200,6); Cell tip = add(world,210,100);
        sample(world,20);
        assertFalse(fitness.current(world).containsKey(tip)); assertFalse(fitness.ranked(world).contains(tip));
        assertTrue(body.contains(fitness.ranked(world).getFirst()));
    }

    @Test void transientConnectedHeightDoesNotBeatPersistentHeight() {
        Environment world = world(); var stable = tower(world,200,6); var brief = tower(world,600,3);
        sample(world,19);
        for(int row=3;row<9;row++) brief.add(add(world,600,floor(world)-10-20*row));
        sample(world,1);
        assertTrue(fitness.current(world).get(brief.getFirst()).score()>fitness.current(world).get(stable.getFirst()).score());
        assertTrue(stable.contains(fitness.ranked(world).getFirst()));
    }

    @Test void lostSupportRemovesHistoricWinnerFromSelection() {
        Environment world = world(); var high = tower(world,200,8); var low = tower(world,600,3);
        sample(world,20); world.getCellList().remove(high.getFirst());
        assertTrue(low.contains(fitness.ranked(world).getFirst()));
        assertTrue(high.subList(1,high.size()).stream().noneMatch(fitness.ranked(world)::contains));
    }

    @Test void broadActorFieldIsNotAContactChain() {
        Environment world = world(); Cell base = tower(world,200,1).getFirst();
        Cell upper = add(world,200,base.getPosition().getY()-23);
        assertTrue(fitness.current(world).containsKey(base)); assertFalse(fitness.current(world).containsKey(upper));
    }

    @Test void smallGroundGapIsToleratedButHoveringIsNot() {
        Environment world = world(); Cell near = add(world,200,floor(world)-10-.5);
        Cell hover = add(world,600,floor(world)-10-2);
        assertTrue(fitness.current(world).containsKey(near)); assertFalse(fitness.current(world).containsKey(hover));
    }

    @Test void wallsAndPlatformsDoNotCountAsGround() {
        Environment world = new Environment(6400,1200);
        for(var type:List.of(Blocker.BlockerType.WALL,Blocker.BlockerType.PLATFORM)) {
            world.getBlockerList().clear(); world.addBlocker(new Blocker(new Vector2D(200,1100),200,20,Color.GRAY,type));
            world.resetCells(); add(world,200,1080);
            assertTrue(fitness.current(world).isEmpty());
        }
    }

    @Test void actualGroundSurfaceIsUsedInsteadOfDefaultGroundHeight() {
        Environment world = new Environment(6400,1200);
        world.addBlocker(new Blocker(new Vector2D(200,1000),200,20,Color.GRAY,Blocker.BlockerType.GROUND));
        Cell base = add(world,200,980);
        assertTrue(fitness.current(world).get(base).base());
    }

    @Test void contactsAcrossTrainingSectionsDoNotProvideSupport() {
        Environment world = world(); Cell base = add(world,399,floor(world)-10);
        Cell upper = add(world,401,floor(world)-30);
        assertTrue(fitness.current(world).containsKey(base)); assertFalse(fitness.current(world).containsKey(upper));
    }

    @Test void periodicCeilingDoesNotConnectToFloor() {
        Environment world = new Environment(6400,1200);
        world.addBlocker(new Blocker(new Vector2D(3200,1210),6400,20,Color.GRAY,Blocker.BlockerType.GROUND));
        Cell base = add(world,200,1190), ceiling = add(world,200,10);
        assertTrue(fitness.current(world).containsKey(base)); assertFalse(fitness.current(world).containsKey(ceiling));
    }

    @Test void singleAirborneOutlierDoesNotRescaleExistingScores() {
        Environment world = world(); Cell base = tower(world,200,4).getFirst();
        double before = fitness.current(world).get(base).score(); add(world,600,10);
        assertEquals(before,fitness.current(world).get(base).score());
    }

    @Test void missingHistoryDoesNotGiveLateNewcomerFullCredit() {
        Environment world = world(); var stable = tower(world,200,3); sample(world,19);
        var late = tower(world,600,8); sample(world,1);
        assertEquals(0,fitness.persistentScore(late.getFirst()));
        assertTrue(stable.contains(fitness.ranked(world).getFirst()));
    }

    @Test void bootstrapHasNoHeightBonusAndIgnoresOldGroundedScores() {
        Environment world = world(); Cell former = tower(world,230,4).getFirst(); sample(world,20);
        world.resetCells(List.of(former)); former.setPosition(new Vector2D(230,10));
        Cell centered = add(world,200,800); sample(world,1);
        assertSame(centered,fitness.ranked(world).getFirst());
    }

    @Test void resetRemovesPreviousGenerationScores() {
        Environment world = world(); Cell base = tower(world,200,4).getFirst(); sample(world,20);
        assertTrue(fitness.persistentScore(base)>0); fitness.reset(); assertEquals(0,fitness.persistentScore(base));
    }

    @Test void deadBaseDoesNotSupportLivingUpperCells() {
        Environment world = world(); var body = tower(world,200,3); body.getFirst().setEnergy(0);
        assertTrue(fitness.current(world).isEmpty());
    }

    @Test void fullPhysicsRewardsStandingPackOverCollapsedChain() {
        SimulationConfig config = SimulationConfig.getInstance(); double sun = config.getSunRayRate();
        try {
            config.setSunRayRate(0);
            Environment world = world(); List<Cell> standing = new ArrayList<>(), narrow = new ArrayList<>();
            for(int row=0;row<12;row++) for(int col=0;col<5;col++) {
                TestTrainStrategyUtils.createAndAddCell(world,200+(col-2)*40+(row%2)*20+(row==11?.2:0),
                        floor(world)-20-row*Math.sqrt(3)*20,20,true,false,false,false);
                standing.add(world.getCellList().getLast());
            }
            for(int row=0;row<8;row++) {
                TestTrainStrategyUtils.createAndAddCell(world,600+(row%2)*12+(row==7?.12:0),
                        floor(world)-12-row*Math.sqrt(3)*12,12,true,false,false,false);
                narrow.add(world.getCellList().getLast());
            }
            var physicsGrid = new SpatialGridPartitioningStrategy(6400,1200,80);
            for(int step=1;step<=1800;step++) {
                physicsGrid.build(world.getCellList()); world.update(1.0/60,physicsGrid);
                if(step>1200 && step%30==0) fitness.sample(world);
            }
            assertEquals(68,world.getCellList().size());
            assertTrue(standing.contains(fitness.ranked(world).getFirst()));
            assertTrue(fitness.persistentScore(standing.getFirst())>fitness.persistentScore(narrow.getFirst()));
        } finally { config.setSunRayRate(sun); }
    }
}
