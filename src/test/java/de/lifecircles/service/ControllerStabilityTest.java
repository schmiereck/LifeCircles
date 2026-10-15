package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class ControllerStabilityTest {
    static CellBrainInterface fixedBrain() {
        return ControllerStabilityLab.uniformBrain(90);
    }
    @ParameterizedTest @ValueSource(ints={45,15})
    void uniformStrengthSeparatesStandingFromSpreading(int strength) throws Exception {
        var config=SimulationConfig.getInstance(); double age=config.getReproductionAgeThreshold();
        try(var saved=new MechanicsLab.Settings()) {
            config.setReproductionAgeThreshold(Double.MAX_VALUE);
            MechanicsLab.configure(new MechanicsLab.Parameters("current",90,125,.8,.8,.25));
            var result=ControllerStabilityLab.run(()->ControllerStabilityLab.uniformBrain(strength),
                    new CellType(0,1,0),0,2,true,1.0/60,30,null,"");
            if(strength==45) {assertTrue(result.height()>.95); assertEquals(1,result.contacts());}
            else {assertTrue(result.height()<.65); assertTrue(result.width()>2);}
        } finally {config.setReproductionAgeThreshold(age);}
    }
    @Test void blockedChannelsGiveKnownStableMechanicalControl() throws Exception {
        var config=SimulationConfig.getInstance(); double age=config.getReproductionAgeThreshold();
        try(var saved=new MechanicsLab.Settings()) {
            config.setReproductionAgeThreshold(Double.MAX_VALUE);
            MechanicsLab.configure(new MechanicsLab.Parameters("current",90,125,.8,.8,.25));
            var result=ControllerStabilityLab.run(ControllerStabilityTest::fixedBrain,new CellType(0,1,0),0,0,false,1.0/60,30,null,"");
            assertTrue(result.height()>.95); assertTrue(result.width()<1.02);
            assertTrue(result.speed()<.001); assertEquals(20,result.radius(),1e-9);
            assertEquals(-90,result.meanSignedForce(),1e-9); assertEquals(1,result.contacts());
        } finally {config.setReproductionAgeThreshold(age);}
    }
    @Test void frozenOutputsRemainFrozenWhileBrainKeepsExecuting() {
        var delegate=new ChangingBrain();
        var intervention=new ControllerStabilityLab.InterventionBrain(delegate,7,true);
        var first=intervention.process().clone();
        assertArrayEquals(first,intervention.process());
        assertEquals(2,delegate.calls);
        assertEquals(0,first[GlobalOutputFeature.values().length+ActorOutputFeature.ENERGY_DELIVERY.ordinal()]);
        assertEquals(0,first[GlobalOutputFeature.values().length+ActorOutputFeature.REPRODUCTION_DESIRE.ordinal()]);
    }
    @Test void liveOutputsAndChannelMaskAreIndependent() {
        var delegate=new ChangingBrain();
        var intervention=new ControllerStabilityLab.InterventionBrain(delegate,ControllerStabilityLab.FORCE,false);
        var first=intervention.process().clone(); var second=intervention.process();
        int b=GlobalOutputFeature.values().length;
        assertNotEquals(first[b+ActorOutputFeature.FORCE.ordinal()],second[b+ActorOutputFeature.FORCE.ordinal()]);
        assertEquals(1,second[GlobalOutputFeature.SIZE.ordinal()]);
        assertEquals(0,second[b+ActorOutputFeature.TYPE_RED.ordinal()]);
        assertEquals(1,second[b+ActorOutputFeature.TYPE_GREEN.ordinal()]);
    }
    static class ChangingBrain implements CellBrainInterface {
        final CellBrainInterface delegate=fixedBrain(); int calls;
        public double[] process(){double[] out=delegate.process().clone();java.util.Arrays.fill(out,++calls*.1);return out;}
        public int getSynapseCount(){return 0;}
        public boolean[] determineActiveHiddenLayers(int state){return new boolean[0];}
        public void setInputs(double[] inputs){delegate.setInputs(inputs);}
        public int getInputCount(){return delegate.getInputCount();}
        public long getProccessedSynapses(){return 0;}
        public NeuralNetwork mutate(double r,double s){throw new UnsupportedOperationException();}
        public double getOutputValue(int i){return delegate.getOutputValue(i);}
        public double getInputValue(int i){return delegate.getInputValue(i);}
    }
}
