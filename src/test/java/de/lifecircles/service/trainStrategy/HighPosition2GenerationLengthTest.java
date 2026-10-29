package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.Environment;
import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

/** Studies may lengthen HIGH_POSITION2 generations; the default stays 50 s. Footer of both HIGH_POSITION modes. */
class HighPosition2GenerationLengthTest {
    static int selections(Integer generationSteps, int steps) {
        int[] calls={0};
        HighPosition2TrainStrategy.EvaluationObserver observer=(world,fitness,winners)->calls[0]++;
        var strategy=generationSteps==null?new HighPosition2TrainStrategy(new Random(1),observer)
                :new HighPosition2TrainStrategy(new Random(1),observer,generationSteps);
        Environment world=strategy.initializeEnvironment();strategy.initialize(world);
        for(int i=0;i<steps;i++)strategy.selectAndMutate(world);
        return calls[0];
    }

    @Test void selectsAfterTheConfiguredNumberOfSteps() {
        assertEquals(0,selections(null,2999));
        assertEquals(1,selections(null,3000));
        assertEquals(1,selections(7200,7200*2-1));
        assertEquals(2,selections(7200,7200*2));
        assertThrows(IllegalArgumentException.class,()->new HighPosition2TrainStrategy(new Random(1),null,600));
    }

    /** The footer shows the generation, the time within it and the phase, like RACE and HIGH_POSITION3. */
    @Test void footerCountsGenerations() {
        var hp2=new HighPosition2TrainStrategy(new Random(1),null);
        Environment world=hp2.initializeEnvironment();hp2.initialize(world);
        assertTrue(hp2.getStatus().startsWith("HIGH_POSITION2 · G1 "),hp2.getStatus());
        for(int i=0;i<2400;i++)hp2.selectAndMutate(world,1/60.0);
        assertTrue(hp2.getStatus().contains("G1 · Schritt 2400/3000 · 40 s · Wachstum"),hp2.getStatus());
        for(int i=0;i<30;i++)hp2.selectAndMutate(world,1/60.0);
        assertTrue(hp2.getStatus().contains("Bewertung"),hp2.getStatus());
        for(int i=0;i<570;i++)hp2.selectAndMutate(world,1/60.0);
        assertTrue(hp2.getStatus().contains("G2 · Schritt 0/3000 · 0 s"),hp2.getStatus());
        assertFalse(hp2.getStatus().endsWith("letzte Bestwertung —"),hp2.getStatus());

        var hp=new HighPositionTrainStrategy();
        world=hp.initializeEnvironment();hp.initialize(world);
        for(int i=0;i<10000;i++)hp.selectAndMutate(world,1/60.0);
        assertTrue(hp.getStatus().startsWith("HIGH_POSITION · G2 · Schritt 0/10000 · 0 s · 16 Zellen"),hp.getStatus());
        assertTrue(hp.getStatus().matches(".*höchste -?\\d+ px · letzte Bestwertung -?\\d+ px"),hp.getStatus());
    }
}
