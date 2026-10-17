package de.lifecircles.service;

import de.lifecircles.model.*;

/** Explicit founder budgets; biological births must instead partition the parent's actual stores. */
public final class EnergyAllocationService {
    public static final double BRAIN_SHARE=.2;
    private EnergyAllocationService() {}
    public static void initializeFounder(Cell cell,double total) {
        if(!Double.isFinite(total) || total<0 || total>1)throw new IllegalArgumentException("Founder budget must be in [0,1]");
        for(EnergyType type:EnergyType.ALL)cell.setEnergy(type,0);
        double brain=total*BRAIN_SHARE;
        cell.setEnergy(EnergyType.PRIMARY,total-brain);cell.setEnergy(EnergyType.SECONDARY,brain);
    }
}
