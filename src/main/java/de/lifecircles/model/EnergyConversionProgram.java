package de.lifecircles.model;

import java.io.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

/** Immutable genotype: one expressed enzyme for each of the eight cell specializations. */
public final class EnergyConversionProgram implements Serializable {
    private static final long serialVersionUID=1L;
    public static final int SPECIALIZATION_COUNT=8;
    public static final EnergyConversionProgram NONE=uniform(EnergyConversion.NONE);
    private final EnergyConversion[] routes;

    public EnergyConversionProgram(EnergyConversion[] routes) {
        if(routes.length!=SPECIALIZATION_COUNT) throw new IllegalArgumentException("Eight specialization routes required");
        this.routes=routes.clone();
        for(EnergyConversion route:this.routes) Objects.requireNonNull(route);
    }
    public static EnergyConversionProgram uniform(EnergyConversion route) {
        Objects.requireNonNull(route);
        EnergyConversion[] routes=new EnergyConversion[SPECIALIZATION_COUNT];Arrays.fill(routes,route);
        return new EnergyConversionProgram(routes);
    }
    public EnergyConversion route(int specialization) {
        return routes[Objects.checkIndex(specialization,SPECIALIZATION_COUNT)];
    }
    public EnergyConversionProgram withRoute(int specialization,EnergyConversion route) {
        Objects.checkIndex(specialization,SPECIALIZATION_COUNT);Objects.requireNonNull(route);
        if(routes[specialization].equals(route)) return this;
        EnergyConversion[] copy=routes.clone();copy[specialization]=route;
        return new EnergyConversionProgram(copy);
    }
    public EnergyConversionProgram mutate(Random random,double rate,double strength) {
        Objects.requireNonNull(random);
        if(!Double.isFinite(rate) || rate<0 || rate>1 || !Double.isFinite(strength) || strength<0)
            throw new IllegalArgumentException("Invalid conversion mutation parameters");
        if(rate==0 || strength==0) return this;
        EnergyConversion[] changed=routes.clone();boolean differs=false;
        for(int i=0;i<changed.length;i++) {
            changed[i]=routes[i].mutate(random,rate,strength);
            differs|=!changed[i].equals(routes[i]);
        }
        return differs?new EnergyConversionProgram(changed):this;
    }
    @Override public boolean equals(Object other) {
        return other instanceof EnergyConversionProgram program && Arrays.equals(routes,program.routes);
    }
    @Override public int hashCode() { return Arrays.hashCode(routes); }
    @Serial private void readObject(ObjectInputStream input) throws IOException,ClassNotFoundException {
        input.defaultReadObject();
        if(routes==null || routes.length!=SPECIALIZATION_COUNT || Arrays.stream(routes).anyMatch(Objects::isNull))
            throw new InvalidObjectException("Invalid specialization conversion program");
    }
}
