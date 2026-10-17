package de.lifecircles.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.Random;

/** One inherited enzyme route, independent of visual cell colors and neural runtime state. */
public record EnergyConversion(EnergyType source, EnergyType target, double capacity) implements Serializable {
    private static final long serialVersionUID=1L;
    public static final EnergyConversion NONE=new EnergyConversion(EnergyType.SECONDARY,EnergyType.PRIMARY,0);
    public EnergyConversion {
        Objects.requireNonNull(source); Objects.requireNonNull(target);
        if(source==target || !Double.isFinite(capacity) || capacity<0 || capacity>1)
            throw new IllegalArgumentException("Conversion needs distinct types and capacity in [0,1]");
    }
    public boolean active() { return capacity>0; }
    public EnergyConversion mutate(Random random,double rate,double strength) {
        Objects.requireNonNull(random);
        if(!Double.isFinite(rate) || rate<0 || rate>1 || !Double.isFinite(strength) || strength<0)
            throw new IllegalArgumentException("Invalid conversion mutation parameters");
        if(rate==0 || strength==0) return this;
        EnergyType nextSource=source,nextTarget=target;
        double nextCapacity=capacity;
        if(random.nextDouble()<rate) nextCapacity=Math.clamp(capacity+random.nextDouble(-1,1)*strength,0,1);
        if(random.nextDouble()<rate) nextSource=differentType(random,nextTarget);
        if(random.nextDouble()<rate) nextTarget=differentType(random,nextSource);
        return new EnergyConversion(nextSource,nextTarget,nextCapacity);
    }
    private static EnergyType differentType(Random random,EnergyType excluded) {
        int code=random.nextInt(EnergyType.ALL.size()-1);
        return EnergyType.ALL.get(code>=excluded.getCode()?code+1:code);
    }
}
