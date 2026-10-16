package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Synthetic input interventions: output influence, not a fitness assay. */
@EnabledIfSystemProperty(named="function.signals.folder",matches=".+")
class FunctionSignalStudyTest {
    enum Context {BASE,ENERGY_LOW,ENERGY_HIGH,AGE_NEW,AGE_OLD,SUN,ROTATE90,ROTATE180,ROTATE270,
        NEIGHBOR_GREEN,NEIGHBOR_RED,NEIGHBOR_BLUE,NEIGHBOR_LOW,NEIGHBOR_HIGH,NEIGHBOR_ATTRACT,NEIGHBOR_REPEL,
        NEIGHBOR_STATE1,NEIGHBOR_STATE3,NEIGHBOR_STATE7,NEIGHBOR_NEW,NEIGHBOR_OLD}
    record Probe(double[] raw,double[] applied,int actor,int childState) {}
    static Probe probe(Cell genome,int state,Context context) {
        Environment world=new Environment(600,400,new Random(1));
        Cell cell=FunctionComparisonLab.founder(genome,new Vector2D(300,339),state);
        if(context==Context.ROTATE90)cell.setRotation(Math.PI/2);
        if(context==Context.ROTATE180)cell.setRotation(Math.PI);
        if(context==Context.ROTATE270)cell.setRotation(3*Math.PI/2);
        double[] raw=new double[FunctionComparisonLab.OUTPUTS],applied=new double[raw.length];
        int actor=0;
        for(int t=0;t<64;t++) {
            double[] inputs=CellBrainService.generateInputs(cell,world);
            if(context==Context.ENERGY_LOW)inputs[GlobalInputFeature.MY_CELL_ENERGY.ordinal()]=.05;
            if(context==Context.ENERGY_HIGH)inputs[GlobalInputFeature.MY_CELL_ENERGY.ordinal()]=.95;
            if(context==Context.AGE_NEW)inputs[GlobalInputFeature.MY_CELL_AGE.ordinal()]=0;
            if(context==Context.AGE_OLD)inputs[GlobalInputFeature.MY_CELL_AGE.ordinal()]=100;
            if(context==Context.SUN)inputs[GlobalInputFeature.MY_CELL_SUNRAY_HIT.ordinal()]=1;
            if(context.name().startsWith("NEIGHBOR"))for(int i=0;i<12;i++) {
                int b=GlobalInputFeature.values().length+i*SensorInputFeature.values().length;
                inputs[b+SensorInputFeature.SENSED_CELL_TYPE_RED.ordinal()]=context==Context.NEIGHBOR_RED?1:0;
                inputs[b+SensorInputFeature.SENSED_CELL_TYPE_GREEN.ordinal()]=context==Context.NEIGHBOR_RED || context==Context.NEIGHBOR_BLUE?0:1;
                inputs[b+SensorInputFeature.SENSED_CELL_TYPE_BLUE.ordinal()]=context==Context.NEIGHBOR_BLUE?1:0;
                inputs[b+SensorInputFeature.SENSED_ACTOR_TYPE_RED.ordinal()]=inputs[b+SensorInputFeature.SENSED_CELL_TYPE_RED.ordinal()];
                inputs[b+SensorInputFeature.SENSED_ACTOR_TYPE_GREEN.ordinal()]=inputs[b+SensorInputFeature.SENSED_CELL_TYPE_GREEN.ordinal()];
                inputs[b+SensorInputFeature.SENSED_ACTOR_TYPE_BLUE.ordinal()]=inputs[b+SensorInputFeature.SENSED_CELL_TYPE_BLUE.ordinal()];
                inputs[b+SensorInputFeature.SENSED_CELL_ENERGY.ordinal()]=context==Context.NEIGHBOR_LOW?.05:context==Context.NEIGHBOR_HIGH?.95:.5;
                inputs[b+SensorInputFeature.SENSED_CELL_AGE.ordinal()]=context==Context.NEIGHBOR_NEW?0:context==Context.NEIGHBOR_OLD?100:20;
                int neighborState=context==Context.NEIGHBOR_STATE1?1:context==Context.NEIGHBOR_STATE3?3:context==Context.NEIGHBOR_STATE7?7:0;
                for(int bit=0;bit<3;bit++)inputs[b+SensorInputFeature.SENSED_CELL_STATE_0.ordinal()+bit]=(neighborState&(1<<bit))==0?0:1;
                inputs[b+SensorInputFeature.SENSED_ACTOR_FORCE_STRENGTH.ordinal()]=context==Context.NEIGHBOR_ATTRACT?-45:context==Context.NEIGHBOR_REPEL?75:0;
            }
            CellBrainService.think(cell,inputs);
            if(t>=48) {
                double[] current=FunctionComparisonLab.commands(cell),physical=applied(cell);
                for(int j=0;j<raw.length;j++){raw[j]+=current[j]/16;applied[j]+=physical[j]/16;}
            }
        }
        for(int i=1;i<12;i++)if(cell.getSensorActors().get(i).getReproductionDesire()>cell.getSensorActors().get(actor).getReproductionDesire())actor=i;
        if(cell.getSensorActors().get(actor).getReproductionDesire()<SimulationConfig.getInstance().getReproductionDesireThreshold())actor=-1;
        return new Probe(raw,applied,actor,actor<0?-1:cell.getSensorActors().get(actor).getReproductionState());
    }
    static double[] applied(Cell cell) {
        var c=SimulationConfig.getInstance();double[] out=new double[FunctionComparisonLab.OUTPUTS];
        out[0]=(cell.getTargetRadiusSize()-c.getCellMinRadiusSize())/(c.getCellMaxRadiusSize()-c.getCellMinRadiusSize());
        for(int i=0;i<12;i++) {
            var actor=cell.getSensorActors().get(i);int b=1+i*ActorOutputFeature.values().length;
            out[b]=actor.getType().getRed();out[b+1]=actor.getType().getGreen();out[b+2]=actor.getType().getBlue();
            double force=actor.getForceStrength();out[b+3]=(1+force/(force<0?c.getCellActorMaxAttractiveForceStrength():c.getCellActorMaxRepulsiveForceStrength()))/2;
            out[b+4]=actor.getEnergyAbsorption();out[b+5]=actor.getEnergyDelivery();out[b+6]=actor.getReproductionDesire();out[b+7]=actor.getReproductionEnergyShareOutput();
            for(int bit=0;bit<3;bit++)out[b+8+bit]=(actor.getReproductionState()&(1<<bit))==0?0:1;
        }
        return out;
    }
    static double maximumDelta(double[] a,double[] b) {
        double max=0;for(int i=0;i<a.length;i++){assertTrue(Double.isFinite(a[i]) && Double.isFinite(b[i]));max=Math.max(max,Math.abs(a[i]-b[i]));}return max;
    }
    @Test void probeSelectedGenomes() throws Exception {
        Path root=Path.of(System.getProperty("function.signals.folder"));
        Path out=Path.of(System.getProperty("function.signals.output","target/function-signals.csv"));Files.createDirectories(out.toAbsolutePath().getParent());
        int models=Integer.getInteger("function.signals.models",18);Random previous=NeuralNetwork.getRandom();
        StringBuilder csv=new StringBuilder("model,state,context,max_raw_delta,max_applied_delta,max_applied_feature,winner_changed,child_state_changed\n");
        try {
            for(int model=0;model<models;model++) {
                Cell genome=NoneEnergyStudyTest.load(Files.readAllBytes(root.resolve("model-"+model+".cells"))).getFirst();
                for(int state=0;state<8;state++) {
                    Probe baseline=probe(genome,state,Context.BASE);
                    for(Context context:Context.values())if(context!=Context.BASE) {
                        Probe changed=probe(genome,state,context);
                        int index=0;for(int j=1;j<changed.applied.length;j++)if(Math.abs(changed.applied[j]-baseline.applied[j])>Math.abs(changed.applied[index]-baseline.applied[index]))index=j;
                        String feature=index==0?"SIZE":"ACTOR_"+((index-1)/11)+"_"+ActorOutputFeature.values()[(index-1)%11];
                        csv.append(String.format(Locale.ROOT,"%d,%d,%s,%.12g,%.12g,%s,%s,%s%n",model,state,context,
                                maximumDelta(baseline.raw,changed.raw),maximumDelta(baseline.applied,changed.applied),feature,baseline.actor!=changed.actor,baseline.childState!=changed.childState));
                    }
                }
            }
        } finally {NeuralNetwork.setRandom(previous);}
        Files.writeString(out,csv);System.out.println("Input interventions for "+models+" genomes written to "+out);
    }
}
