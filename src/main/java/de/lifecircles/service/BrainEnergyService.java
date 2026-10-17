package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.CellBrainService;

/** Storage is paid every step; decision work is authorized before any recurrent state can change. */
public final class BrainEnergyService {
    private BrainEnergyService() {}
    public static boolean prepare(Cell cell,double dt,BrainEnergyCosts costs) {
        if(!Double.isFinite(dt) || dt<0)throw new IllegalArgumentException("Invalid brain time step");
        cell.authorizeBrainDecision(false);
        if(cell.isRemains())return false;
        double energy=cell.getEnergy(EnergyType.SECONDARY);
        double storage=costs.storage()==0?0:costs.storageCost(cell.getBrain().getSynapseCount(),dt);
        boolean maintained=energy>=storage;
        double remaining=Math.max(0,energy-storage);
        cell.setEnergy(EnergyType.SECONDARY,remaining);
        // An unpaid storage bill already forbids thinking; do not walk the entire
        // network again to reserve work that cannot run. Free experiments need no census.
        double reserve=!maintained || costs.work()==0?0:costs.workCost(cell.getBrain().getMaximumProcessedSynapses());
        boolean permitted=maintained && remaining>=reserve;
        cell.setBrainPaused(!permitted);
        cell.authorizeBrainDecision(permitted);
        return permitted;
    }
    public static void thinkPrepared(Cell cell,double[] inputs,BrainEnergyCosts costs) {
        if(cell.isRemains() || cell.isBrainPaused())return;
        if(!cell.consumeBrainDecisionAuthorization())throw new IllegalStateException("Decision was not funded by prepare");
        CellBrainService.think(cell,inputs);
        double cost=costs.workCost(cell.getBrain().getProccessedSynapses());
        double available=cell.getEnergy(EnergyType.SECONDARY);
        if(cost>available)throw new IllegalStateException("Brain exceeded its reserved work bound");
        cell.setEnergy(EnergyType.SECONDARY,available-cost);
    }
}
