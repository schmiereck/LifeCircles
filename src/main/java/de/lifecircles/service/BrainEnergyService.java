package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.CellBrainService;

/** Storage is paid every step; decision work is authorized before any recurrent state can change. */
public final class BrainEnergyService {
    /** Innate emergency respiration 000 -> 001: half the yield of an inherited enzyme, slow, on demand only. */
    public static final double BASAL_EFFICIENCY=.4;
    public static final double BASAL_MAX_INPUT_PER_SECOND=SimulationConfig.ENERGY_DECAY_RATE;
    private BrainEnergyService() {}
    public static boolean prepare(Cell cell,double dt,BrainEnergyCosts costs) { return prepare(cell,dt,costs,true); }
    public static boolean prepare(Cell cell,double dt,BrainEnergyCosts costs,boolean basal) { return prepare(cell,dt,costs,basal,false); }
    /** holdForces: a paused cell keeps its actor fields (tonus, see ZELLVERBINDUNGEN.md). */
    public static boolean prepare(Cell cell,double dt,BrainEnergyCosts costs,boolean basal,boolean holdForces) {
        if(!Double.isFinite(dt) || dt<0)throw new IllegalArgumentException("Invalid brain time step");
        cell.authorizeBrainDecision(false);
        if(cell.isRemains())return false;
        double storage=costs.storage()==0?0:costs.storageCost(cell.getBrain().getSynapseCount(),dt);
        double basalLimit=basal?BASAL_MAX_INPUT_PER_SECOND*dt*BASAL_EFFICIENCY:0;
        // A bill that even emergency respiration cannot reach forbids thinking; do not walk
        // the entire network to reserve work that cannot run. Free experiments need no census.
        boolean reachable=cell.getEnergy(EnergyType.SECONDARY)+basalLimit>=storage;
        double reserve=!reachable || costs.work()==0?0:costs.workCost(cell.getBrain().getMaximumProcessedSynapses());
        // Tiny relative margin against rounding below the bill; any excess simply stays stored as 001.
        if(basal)respire(cell,(storage+reserve-cell.getEnergy(EnergyType.SECONDARY))*(1+1e-9),dt);
        double energy=cell.getEnergy(EnergyType.SECONDARY);
        boolean maintained=energy>=storage;
        double remaining=Math.max(0,energy-storage);
        cell.setEnergy(EnergyType.SECONDARY,remaining);
        boolean permitted=maintained && remaining>=reserve;
        cell.setBrainPaused(!permitted,holdForces);
        cell.authorizeBrainDecision(permitted);
        return permitted;
    }
    /**
     * Covers at most the current brain shortfall from 000. Never touches the 000 needed for
     * this step's maintenance, so the brain cannot starve the body. Returns dissipated energy.
     */
    static double respire(Cell cell,double shortfall,double dt) {
        if(!(shortfall>0))return 0;
        double primary=cell.getEnergy(EnergyType.PRIMARY);
        double spare=Math.max(0,primary-SimulationConfig.ENERGY_DECAY_RATE*dt);
        double consumed=Math.min(spare,Math.min(BASAL_MAX_INPUT_PER_SECOND*dt,shortfall/BASAL_EFFICIENCY));
        if(consumed<=0)return 0;
        cell.setEnergy(EnergyType.PRIMARY,primary-consumed);
        cell.setEnergy(EnergyType.SECONDARY,cell.getEnergy(EnergyType.SECONDARY)+consumed*BASAL_EFFICIENCY);
        return consumed*(1-BASAL_EFFICIENCY);
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
