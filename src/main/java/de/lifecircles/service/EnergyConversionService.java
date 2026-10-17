package de.lifecircles.service;

import de.lifecircles.model.Cell;
import de.lifecircles.model.EnergyConversion;

/** One bounded, lossy reaction per living cell and step. No energy can run through a same-step cycle. */
public final class EnergyConversionService {
    public static final double EFFICIENCY=.8;
    public static final double MAX_INPUT_PER_SECOND=.1;
    private EnergyConversionService() {}

    /** Returns dissipated energy, for explicit balance checks. Capacity limits consumed source energy. */
    public static double convert(Cell cell,double dt) {
        if(!Double.isFinite(dt) || dt<0) throw new IllegalArgumentException("Invalid conversion time step");
        EnergyConversion conversion=cell.getEnergyConversion();
        double activity=cell.getEnzymeActivity();
        if(dt==0 || cell.isRemains() || !conversion.active() || activity==0) return 0;
        double source=cell.getEnergy(conversion.source()), target=cell.getEnergy(conversion.target());
        double room=Math.max(0,cell.getMaxEnergy(conversion.target())-target);
        double consumed=Math.min(source,Math.min(MAX_INPUT_PER_SECOND*conversion.capacity()*activity*dt,room/EFFICIENCY));
        if(consumed<=0) return 0;
        cell.setEnergy(conversion.source(),source-consumed);
        cell.setEnergy(conversion.target(),target+consumed*EFFICIENCY);
        return consumed*(1-EFFICIENCY);
    }
}
