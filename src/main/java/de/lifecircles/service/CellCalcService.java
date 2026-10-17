package de.lifecircles.service;

import de.lifecircles.model.Cell;
import de.lifecircles.model.neural.CellBrainService;

/**
 * Service for cell calculation operations.
 */
public class CellCalcService {
    
    /**
     * Updates the cell's position, rotation, and behavior based on its current state.
     * @param cell The cell to update
     * @param deltaTime Time step in seconds
     */
    public static void updateCell(final Cell cell, final double deltaTime) {
        updateCell(cell, deltaTime, CellBrainService.generateInputs(cell));
    }

    public static void updateCell(final Cell cell, final double deltaTime, final double[] inputs) {
        CellBrainService.think(cell, inputs);
        advanceCell(cell, deltaTime);
    }

    /** Advance physics/metabolism after all cells have made their decisions. */
    public static void advanceCell(final Cell cell, final double deltaTime) {
        advanceCell(cell, deltaTime, BrainEnergyCosts.DEFAULT);
    }

    public static void advanceCell(Cell cell, double deltaTime, BrainEnergyCosts costs) {

        // Newborns grow linearly toward the current brain target during their first
        // simulation second. Thinking may change that target, but never growthAge.
        if (!cell.isRemains() && cell.isGrowing()) {
            cell.incGrowthAge(deltaTime);
            if (cell.getGrowthAge() >= SimulationConfig.CELL_GROWTH_DURATION) {
                // Wachstumsprozess abgeschlossen
                cell.setRealRadiusSize(cell.getTargetRadiusSize());
                cell.setIsGrowing(false);
            } else {
                // Lineare Interpolation zwischen Startgröße und Zielgröße
                double growthProgress = cell.getGrowthAge() / SimulationConfig.CELL_GROWTH_DURATION;
                double minSize = SimulationConfig.getInstance().getCellMinGrowRadiusSize();
                cell.setRealGrowRadiusSize(minSize + (cell.getTargetRadiusSize() - minSize) * growthProgress);
            }
        }

        // Adult smoothing advances once per simulation step, independently of thinking.
        if (!cell.isRemains() && !cell.isGrowing()) {
            cell.applyDelayedSizeChange(deltaTime);
        }

        // Update physics
        cell.setPosition(cell.getPosition().add(cell.getVelocity().multiply(deltaTime)));
        // Integrate d(omega)/dt = -friction * omega, including its effect on angle.
        // Forces have already supplied their impulse in updateForces. For free
        // rotation, both angle and speed are independent of step subdivision.
        double friction = SimulationConfig.getInstance().getRotationalFriction();
        double dampingTime = friction * deltaTime;
        double rotationTime = dampingTime == 0 ? deltaTime : -Math.expm1(-dampingTime) / friction;
        cell.setRotation(cell.getRotation() + cell.getAngularVelocity() * rotationTime);
        cell.setAngularVelocity(cell.getAngularVelocity() * Math.exp(-dampingTime));
        
        // Normalize rotation to [0, 2π)
        double rotation = cell.getRotation() % (2 * Math.PI);
        if (rotation < 0) {
            rotation += 2 * Math.PI;
        }
        // Adding 2*pi to a tiny negative remainder can round to exactly 2*pi.
        if (rotation >= 2 * Math.PI) rotation = 0;
        cell.setRotation(rotation);

        // Update energy and age
        if (!cell.isRemains()) {
            EnergyCellCalcService.decayEnergy(cell, deltaTime, costs);
            cell.incAge(deltaTime);
        }
    }

    /**
     * velocityForce to velocity.
     * angularVelocityForce to angularVelocity
     */
    public static void updateForces(final Cell cell) {
        cell.updateForce();
    }

    public static void updateForces(final Cell cell, final double deltaTime) {
        cell.updateForce(deltaTime);
    }
}
