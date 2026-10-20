package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.ActorOutputFeature;
import de.lifecircles.model.neural.GlobalOutputFeature;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import de.lifecircles.service.trainStrategy.TrainMode;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/** Training worlds use adhesion, bonds and ground hold only with the separate opt-in. */
class TrainingAdhesionTest {
    final SimulationConfig config = SimulationConfig.getInstance();
    @BeforeEach @AfterEach void reset() {
        config.setCellAdhesion(true); config.setTrainingAdhesion(false); config.setAdhesionBonds(false);
    }

    @Test void everyTrainingModeFollowsTheOptInWhileOpenWorldsFollowCellAdhesion() {
        for (TrainMode mode : TrainMode.values()) {
            boolean open = mode == TrainMode.NONE || mode == TrainMode.EVOLUTIONARY || mode == TrainMode.WORLD2;
            SimulationStepPolicy policy = CalculationService.strategyFor(mode).stepPolicy();
            assertEquals(open, policy.useCellAdhesion(), mode + " default");
            config.setTrainingAdhesion(true);
            assertTrue(policy.useCellAdhesion(), mode + " opted in");
            config.setCellAdhesion(false);
            assertFalse(policy.useCellAdhesion(), mode + " needs cell adhesion");
            reset();
        }
    }

    /** Radius 20, every actor fully attracting, so a full step keeps the pair locking in. */
    static class AttractBrain extends CellBondTest.Size20Brain {
        @Override public double[] process() {
            double[] o = super.process();
            for (int i = 0; i < 12; i++) o[GlobalOutputFeature.values().length + i * ActorOutputFeature.values().length + ActorOutputFeature.FORCE.ordinal()] = 0;
            return o;
        }
    }

    static boolean bondsAfterOneSecond(SimulationStepPolicy policy) {
        Environment world = new Environment(400, 300);
        for (double x : new double[]{100, 142}) {
            Cell cell = new Cell(new Vector2D(x, 100), 20, new AttractBrain());
            cell.setType(CellBondTest.BODY); cell.setEnergy(1); cell.setEnergy(EnergyType.SECONDARY, 1);
            world.addCell(cell);
        }
        var grid = new SpatialGridPartitioningStrategy(world.getWidth(), world.getHeight(), 100);
        for (int i = 0; i < CellBondTest.steps(1); i++) { grid.build(world.getCellList()); world.update(CellBondTest.DT, grid, policy); }
        return world.getCellList().stream().flatMap(c -> c.getSensorActors().stream()).anyMatch(a -> a.getBondPartner() != null);
    }

    @Test void trainingStepFormsBondsOnlyWhenOptedIn() {
        config.setAdhesionBonds(true);
        assertFalse(bondsAfterOneSecond(SimulationStepPolicy.TRAINING));
        config.setTrainingAdhesion(true);
        assertTrue(bondsAfterOneSecond(SimulationStepPolicy.TRAINING));
    }
}
